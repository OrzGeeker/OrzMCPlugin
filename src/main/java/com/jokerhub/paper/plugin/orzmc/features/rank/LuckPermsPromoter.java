package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerAccess;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerScheduler;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;

/**
 * LuckPerms 晋升执行器（控制台命令实现，运行时检测 LP 可用性）。
 *
 * <p>通过 Bukkit 控制台执行 LP 命令：{@code lp user &lt;name&gt; promote &lt;track&gt;}
 * 沿 rank track 晋升。track 需预先创建（default→member→builder→admin）。
 *
 * <p>LuckPerms 是软依赖：未安装时晋升命令跳过并记录警告（时长查询/申请状态仍可用）。
 *
 * <p>⚠️ 命令派发必须发生在主线程（Paper 限制：异步线程 dispatch 抛
 * {@code IllegalStateException: Asynchronous Command Dispatched Async}）。
 * 群指令（$v）与 orzdebug 走异步链路，通过注入的 {@link ServerScheduler} 回主线程执行；
 * 测试构造不传 scheduler 时保持同步直发（主线程上下文）。</p>
 */
public final class LuckPermsPromoter implements RankPromoter {

    private static final String TRACK = "rank";
    private static final String LUCKPERMS_PLUGIN = "LuckPerms";

    private final ServerAccess serverAccess;
    private final PlayerNameResolver nameResolver;
    private final ServerScheduler scheduler;

    public LuckPermsPromoter(ServerAccess serverAccess, PlayerNameResolver nameResolver) {
        this(serverAccess, nameResolver, null);
    }

    public LuckPermsPromoter(ServerAccess serverAccess, PlayerNameResolver nameResolver, ServerScheduler scheduler) {
        this.serverAccess = serverAccess;
        this.nameResolver = nameResolver;
        this.scheduler = scheduler;
    }

    /** 玩家名解析：离线服 UUID→最后已知名字。 */
    public interface PlayerNameResolver {
        String resolve(UUID playerId);
    }

    /** LuckPerms 是否已启用（软依赖检测）。 */
    public boolean isLuckPermsEnabled() {
        PluginManager pm = Bukkit.getPluginManager();
        return pm != null && pm.isPluginEnabled(LUCKPERMS_PLUGIN);
    }

    @Override
    public UUID resolvePlayerId(String playerName) {
        org.bukkit.OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
        return p.hasPlayedBefore() ? p.getUniqueId() : null;
    }

    @Override
    public boolean isInGroup(UUID playerId, String groupName) {
        // 真实查询 LP 组：用 lp user <name> parent info 的持久化输出
        // （控制台命令输出无法回显，改用 LuckPerms 的 H2 直接查询太重）
        // 折中：在线玩家用 Bukkit.hasPermission 判断组特有权限不可靠，
        // 因此这里保守返回 false（不做预判），由 promoted 标记保证幂等。
        return false;
    }

    @Override
    public void promoteToNext(UUID playerId) {
        String name = nameResolver.resolve(playerId);
        if (name == null || !isLuckPermsEnabled()) {
            return;
        }
        dispatch("lp user " + name + " promote " + TRACK);
    }

    @Override
    public void promoteToBuilder(UUID playerId) {
        String name = nameResolver.resolve(playerId);
        if (name == null || !isLuckPermsEnabled()) {
            return;
        }
        dispatch("lp user " + name + " parent add builder");
    }

    private void dispatch(String command) {
        Server server = serverAccess.server();
        if (Bukkit.isPrimaryThread() || scheduler == null) {
            server.dispatchCommand(Bukkit.getConsoleSender(), command);
            return;
        }
        // 异步上下文（群指令 $v / orzdebug / 异步审核链路）→ 回主线程派发
        scheduler.runSync(() -> server.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}
