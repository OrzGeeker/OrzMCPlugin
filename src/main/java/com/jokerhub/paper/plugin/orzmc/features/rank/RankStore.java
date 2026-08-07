package com.jokerhub.paper.plugin.orzmc.features.rank;

import java.util.UUID;

/**
 * 玩家晋升状态存储。
 *
 * <p>时长数据由 {@link #getPlaytimeMinutes} 从服务器原生 stats 读取（只读，不自行累计），
 * 申请状态持久化到 ranks.yml。</p>
 */
public interface RankStore {

    /** 累计在线时长（分钟）——读服务器原生 stats（玩家离线也可读）。 */
    long getPlaytimeMinutes(UUID playerId);

    /** 是否有待审核的 builder 申请。 */
    boolean hasPendingApplication(UUID playerId);

    /** 设置申请状态。 */
    void setPendingApplication(UUID playerId, boolean pending);
}
