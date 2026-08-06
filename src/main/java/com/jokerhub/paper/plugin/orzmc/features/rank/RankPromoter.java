package com.jokerhub.paper.plugin.orzmc.features.rank;

import java.util.UUID;

/** 晋升执行器：实际调用 LuckPerms 变更玩家组。 */
public interface RankPromoter {

    /** 玩家是否在 default 组（自动晋升仅作用于 default→member）。 */
    boolean isInGroup(UUID playerId, String groupName);

    /** 沿 rank track 晋升一级（default→member）。 */
    void promoteToNext(UUID playerId);

    /** 直接晋升为 builder（申请审核通过后）。 */
    void promoteToBuilder(UUID playerId);

    /** 玩家名→UUID 解析（离线服查最后已知 UUID）。 */
    UUID resolvePlayerId(String playerName);
}
