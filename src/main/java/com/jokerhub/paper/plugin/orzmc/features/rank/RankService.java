package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.features.review.ReviewNotifier;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewRequest;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewStore;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家权限服务：自动晋升 + 手动升降级 + 当前权限组查询。
 *
 * <p>权限链（track "rank"，LP 为唯一事实源）：default → member → builder → admin。
 * <ul>
 *   <li>自动晋升：default→member（累计在线时长读服务器原生 stats，达阈值，上线时检查）</li>
 *   <li>手动升降级：{@link #promote} / {@link #demote} 每次一级，LP track 原生钳位</li>
 *   <li>申请晋升：member→builder 走通用审核框架（见 {@code ReviewService}），通过后调 {@link #promote}</li>
 * </ul>
 * 升降级委托 {@link RankPromoter}（LP track API），结果状态翻译为业务提示。</p>
 *
 * <p>当前权限组（{@link #currentGroup}）以 LP 真实组为准（在线缓存/离线加载）；
 * 无 LuckPerms 时回退本地推断：存在 APPROVED 的 builder-promotion 审核记录 → builder，否则 default。</p>
 */
public final class RankService {

    /** 默认晋升阈值（小时）。 */
    public static final int DEFAULT_MEMBER_THRESHOLD_HOURS = 10;

    private static final String BUILDER_PROMOTION_TYPE = "builder-promotion";

    private final RankStore store;
    private final ReviewStore reviewStore;
    private final RankPromoter promoter;
    private final int memberThresholdHours;
    private final ReviewNotifier notifier;

    public RankService(RankStore store, ReviewStore reviewStore, RankPromoter promoter) {
        this(store, reviewStore, promoter, DEFAULT_MEMBER_THRESHOLD_HOURS, null);
    }

    public RankService(RankStore store, ReviewStore reviewStore, RankPromoter promoter, int memberThresholdHours) {
        this(store, reviewStore, promoter, memberThresholdHours, null);
    }

    public RankService(
            RankStore store,
            ReviewStore reviewStore,
            RankPromoter promoter,
            int memberThresholdHours,
            ReviewNotifier notifier) {
        this.store = store;
        this.reviewStore = reviewStore;
        this.promoter = promoter;
        this.memberThresholdHours = memberThresholdHours;
        this.notifier = notifier;
    }

    /** 玩家在线则发游戏内消息；通知端口未注入或玩家离线时静默。 */
    private void notifyPlayer(UUID playerId, String message) {
        if (notifier != null) {
            notifier.gameMessage(playerId, message);
        }
    }

    /** 群广播权限变化（模板键 + 变量）。 */
    private void notifyGroup(String templateKey, Map<String, String> vars) {
        if (notifier != null) {
            notifier.groupEvent(templateKey, vars);
        }
    }

    /** 检查玩家是否达到自动晋升条件（default→member）。
     *
     * <p>时长从服务器原生 stats 读取（玩家离线也有数据），因此可在任意时刻调用。
     * 幂等由 LP 保证：已在 member 及以上（track 非首组）不重复晋升。</p>
     */
    public void checkPromotion(UUID playerId) {
        if (!promoter.isAvailable()) {
            return; // 无 LuckPerms：晋升不可用
        }
        long playtime = store.getPlaytimeMinutes(playerId);
        if (playtime < memberThresholdMinutes()) {
            return;
        }
        String current = promoter.currentTrackGroup(playerId);
        if (current == null || "default".equals(current)) {
            promote(playerId);
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

    /** 玩家当前权限组：LP 真实组优先，无 LP/查询失败回退本地推断。 */
    public String currentGroup(UUID playerId) {
        if (promoter.isAvailable()) {
            String trackGroup = promoter.currentTrackGroup(playerId);
            if (trackGroup != null) {
                return trackGroup;
            }
        }
        return hasApprovedBuilder(playerId) ? "builder" : "default";
    }

    /** 是否拥有 APPROVED 的 builder 晋升记录。 */
    private boolean hasApprovedBuilder(UUID playerId) {
        return reviewStore.listByApplicant(playerId).stream()
                .anyMatch(
                        r -> BUILDER_PROMOTION_TYPE.equals(r.typeId()) && r.status() == ReviewRequest.Status.APPROVED);
    }

    /**
     * 升级一级（LP track 钳位）：default→member→builder→admin。
     *
     * @return 升级后的组名；链顶（admin）或不可用时返回 null
     */
    public String promote(UUID playerId) {
        if (!promoter.isAvailable()) {
            return null; // 无 LuckPerms：升级不可用
        }
        String to = promoter.promote(playerId);
        if (to == null) {
            return null; // 链顶（END_OF_TRACK）或失败
        }
        notifyPlayer(playerId, "你的权限已升级：" + groupDisplayName(to) + "。");
        notifyGroup(
                "rank_promoted",
                Map.of(
                        "player", promoter.playerName(playerId).orElse(playerId.toString()),
                        "group", groupDisplayName(to)));
        return to;
    }

    /**
     * 降级一级（LP track 钳位）：admin→builder→member→default。
     *
     * @return 降级后的组名；链底（default）或不可用时返回 null
     */
    public String demote(UUID playerId) {
        if (!promoter.isAvailable()) {
            return null; // 无 LuckPerms：降级不可用
        }
        String to = promoter.demote(playerId);
        if (to == null) {
            return null; // 链底（REMOVED_FROM_FIRST_GROUP / NOT_ON_TRACK）或失败
        }
        notifyPlayer(playerId, "你的权限已被降级：" + groupDisplayName(to) + "。");
        notifyGroup(
                "rank_demoted",
                Map.of(
                        "player", promoter.playerName(playerId).orElse(playerId.toString()),
                        "group", groupDisplayName(to)));
        return to;
    }

    /** LuckPerms 是否可用（软依赖检测）。 */
    public boolean isLuckPermsAvailable() {
        return promoter.isAvailable();
    }

    /** 玩家名→UUID 解析（离线服需查缓存）。 */
    public UUID resolvePlayerId(String playerName) {
        return promoter.resolvePlayerId(playerName);
    }

    /** 当前权限组展示名。 */
    public static String groupDisplayName(String group) {
        return switch (group) {
            case "admin" -> "管理员";
            case "builder" -> "建造者";
            case "member" -> "会员";
            default -> "访客";
        };
    }
}
