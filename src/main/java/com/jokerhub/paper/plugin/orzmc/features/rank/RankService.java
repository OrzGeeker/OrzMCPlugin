package com.jokerhub.paper.plugin.orzmc.features.rank;

import java.util.UUID;

/**
 * 玩家权限晋升服务。
 *
 * <p>自动晋升：default→member（累计在线时长读服务器原生 stats，达阈值）；member→builder
 * 走申请审核；builder→admin 纯手动。晋升执行通过 {@link RankPromoter} 委托给 LuckPerms。</p>
 */
public final class RankService {

    /** 默认晋升阈值（小时）。 */
    public static final int DEFAULT_MEMBER_THRESHOLD_HOURS = 10;

    private final RankStore store;
    private final RankPromoter promoter;
    private final long memberThresholdMinutes;

    public RankService(RankStore store, RankPromoter promoter) {
        this(store, promoter, DEFAULT_MEMBER_THRESHOLD_HOURS);
    }

    public RankService(RankStore store, RankPromoter promoter, int memberThresholdHours) {
        this.store = store;
        this.promoter = promoter;
        this.memberThresholdMinutes = memberThresholdHours * 60L;
    }

    /** 检查玩家是否达到自动晋升条件（default→member）。
     *
     * <p>时长从服务器原生 stats 读取（玩家离线也有数据），因此可在任意时刻调用，
     * 不需要玩家在线。LP promote 幂等（玩家已在更高级别时无副作用）。</p>
     */
    public void checkPromotion(UUID playerId) {
        if (!promoter.isInGroup(playerId, "default")) {
            return; // 已晋升（member 及以上）不重复处理
        }
        long playtime = store.getPlaytimeMinutes(playerId);
        if (playtime >= memberThresholdMinutes) {
            promoter.promoteToNext(playerId);
        }
    }

    /** member 玩家申请晋升 builder。 */
    public void applyForBuilder(UUID playerId) {
        store.setPendingApplication(playerId, true);
    }

    /** 是否有待审核申请。 */
    public boolean hasPendingApplication(UUID playerId) {
        return store.hasPendingApplication(playerId);
    }

    /** 玩家累计在线时长（分钟）——读服务器原生 stats。 */
    public long playtimeMinutes(UUID playerId) {
        return store.getPlaytimeMinutes(playerId);
    }

    /** 晋升 member 阈值（分钟）。 */
    public long memberThresholdMinutes() {
        return memberThresholdMinutes;
    }

    /** 玩家名→UUID 解析（离线服需查缓存）。 */
    public UUID resolvePlayerId(String playerName) {
        return promoter.resolvePlayerId(playerName);
    }

    /** 管理员审核申请。 */
    public void reviewApplication(UUID playerId, boolean approved) {
        store.setPendingApplication(playerId, false);
        if (approved) {
            promoter.promoteToBuilder(playerId);
        }
    }
}
