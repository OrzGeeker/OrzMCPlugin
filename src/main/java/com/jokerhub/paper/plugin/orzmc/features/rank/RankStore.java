package com.jokerhub.paper.plugin.orzmc.features.rank;

import java.util.UUID;

/**
 * 玩家晋升状态存储（permission.yml 的 ranks 节）。
 *
 * <p>时长数据由 {@link #getPlaytimeMinutes} 从服务器原生 stats 读取（只读，不自行累计），
 * 晋升状态持久化到 permission.yml。审核申请记录不再存于此（见 {@code ReviewStore}）。</p>
 */
public interface RankStore {

    /** 累计在线时长（分钟）——读服务器原生 stats（玩家离线也可读）。 */
    long getPlaytimeMinutes(UUID playerId);

    /** 是否已完成自动晋升（default→member 已触发过）。 */
    boolean hasPromoted(UUID playerId);

    /** 标记已自动晋升。 */
    void markPromoted(UUID playerId);
}
