package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.features.review.ReviewRequest;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewStore;
import java.util.UUID;

/**
 * 玩家权限服务：自动晋升（default→member）+ 当前权限组查询。
 *
 * <p>自动晋升：default→member（累计在线时长读服务器原生 stats，达阈值）；member→builder
 * 走通用审核框架（见 {@code ReviewService}，本服务不直接受理申请）；builder→admin 纯手动。
 * 晋升执行通过 {@link RankPromoter} 委托给 LuckPerms。</p>
 *
 * <p>当前权限组（{@link #currentGroup}）依据本地状态推断：
 * builder = 存在 APPROVED 的 builder-promotion 审核记录；member = 已自动晋升标记；其余 default。
 * （与一期一致：LP 为软依赖，控制台命令无回显，不做实时组查询。）</p>
 */
public final class RankService {

    /** 默认晋升阈值（小时）。 */
    public static final int DEFAULT_MEMBER_THRESHOLD_HOURS = 10;

    private static final String BUILDER_PROMOTION_TYPE = "builder-promotion";

    private final RankStore store;
    private final ReviewStore reviewStore;
    private final RankPromoter promoter;
    private final int memberThresholdHours;

    public RankService(RankStore store, ReviewStore reviewStore, RankPromoter promoter) {
        this(store, reviewStore, promoter, DEFAULT_MEMBER_THRESHOLD_HOURS);
    }

    public RankService(RankStore store, ReviewStore reviewStore, RankPromoter promoter, int memberThresholdHours) {
        this.store = store;
        this.reviewStore = reviewStore;
        this.promoter = promoter;
        this.memberThresholdHours = memberThresholdHours;
    }

    /** 检查玩家是否达到自动晋升条件（default→member）。
     *
     * <p>时长从服务器原生 stats 读取（玩家离线也有数据），因此可在任意时刻调用，
     * 不需要玩家在线。已晋升玩家（promoted 标记）不重复处理。</p>
     */
    public void checkPromotion(UUID playerId) {
        if (store.hasPromoted(playerId)) {
            return; // 已晋升过（member 及以上）不重复处理
        }
        long playtime = store.getPlaytimeMinutes(playerId);
        if (playtime >= memberThresholdMinutes()) {
            promoter.promoteToNext(playerId);
            store.markPromoted(playerId);
        }
    }

    /** 玩家累计在线时长（分钟）——读服务器原生 stats。 */
    public long playtimeMinutes(UUID playerId) {
        return store.getPlaytimeMinutes(playerId);
    }

    /** 晋升 member 阈值（分钟）。 */
    public long memberThresholdMinutes() {
        return memberThresholdHours * 60L;
    }

    /** 玩家当前权限组（本地状态推断）。 */
    public String currentGroup(UUID playerId) {
        boolean isBuilder = reviewStore.listByApplicant(playerId).stream()
                .anyMatch(
                        r -> BUILDER_PROMOTION_TYPE.equals(r.typeId()) && r.status() == ReviewRequest.Status.APPROVED);
        if (isBuilder) {
            return "builder";
        }
        return store.hasPromoted(playerId) ? "member" : "default";
    }

    /** 玩家名→UUID 解析（离线服需查缓存）。 */
    public UUID resolvePlayerId(String playerName) {
        return promoter.resolvePlayerId(playerName);
    }

    /** 当前权限组展示名。 */
    public static String groupDisplayName(String group) {
        return switch (group) {
            case "builder" -> "建造者";
            case "member" -> "会员";
            default -> "访客";
        };
    }
}
