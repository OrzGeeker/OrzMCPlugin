package com.jokerhub.paper.plugin.orzmc.features.rank;

import java.util.UUID;

/** 玩家晋升状态存储（YAML 持久化实现）。 */
public interface RankStore {

    /** 累计在线时长（分钟）。 */
    long getPlaytimeMinutes(UUID playerId);

    /** 记录累计在线时长。 */
    void setPlaytimeMinutes(UUID playerId, long minutes);

    /** 是否有待审核的 builder 申请。 */
    boolean hasPendingApplication(UUID playerId);

    /** 设置申请状态。 */
    void setPendingApplication(UUID playerId, boolean pending);
}
