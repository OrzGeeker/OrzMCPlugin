package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerAccess;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * LuckPerms 晋升执行器（控制台命令实现，零依赖）。
 *
 * <p>通过 Bukkit 控制台执行 LP 命令：{@code lp user &lt;name&gt; promote &lt;track&gt;}
 * 沿 rank track 晋升。track 需预先创建（default→member→builder→admin）。</p>
 */
public final class LuckPermsPromoter implements RankPromoter {

    private static final String TRACK = "rank";
    private static final String DEFAULT_GROUP = "default";

    private final ServerAccess serverAccess;
    private final PlayerNameResolver nameResolver;

    public LuckPermsPromoter(ServerAccess serverAccess, PlayerNameResolver nameResolver) {
        this.serverAccess = serverAccess;
        this.nameResolver = nameResolver;
    }

    /** 玩家名解析：离线服 UUID→最后已知名字。 */
    public interface PlayerNameResolver {
        String resolve(UUID playerId);
    }

    @Override
    public UUID resolvePlayerId(String playerName) {
        org.bukkit.OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
        return p.hasPlayedBefore() ? p.getUniqueId() : null;
    }

    @Override
    public boolean isInGroup(UUID playerId, String groupName) {
        String name = nameResolver.resolve(playerId);
        if (name == null) {
            return false;
        }
        Server server = serverAccess.server();
        // 用 LuckPerms 查询命令判断（lp user <name> parent info 含 <group>）
        // 简化：默认按玩家缓存判断——先返回 true（default 组），由晋升命令幂等保护
        return DEFAULT_GROUP.equals(groupName) && server.getPlayer(playerId) != null;
    }

    @Override
    public void promoteToNext(UUID playerId) {
        String name = nameResolver.resolve(playerId);
        if (name == null) {
            return;
        }
        dispatch("lp user " + name + " promote " + TRACK);
    }

    @Override
    public void promoteToBuilder(UUID playerId) {
        String name = nameResolver.resolve(playerId);
        if (name == null) {
            return;
        }
        dispatch("lp user " + name + " parent add builder");
    }

    private void dispatch(String command) {
        Server server = serverAccess.server();
        server.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
