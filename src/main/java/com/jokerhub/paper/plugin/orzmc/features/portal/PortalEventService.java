package com.jokerhub.paper.plugin.orzmc.features.portal;

import com.jokerhub.paper.plugin.orzmc.core.ports.portal.PortalPort;
import com.jokerhub.paper.plugin.orzmc.features.security.PlayerAuthenticationService;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public final class PortalEventService {

    /** 跨服 transfer 触发冷却（毫秒）：防 PlayerPortalEvent + PlayerMoveEvent 双路径重复触发。 */
    private static final long TRANSFER_COOLDOWN_MS = 5000;

    /** Folia 下 PlayerPortalEvent 不触发（2026-08-18 反编译 folia-26.2.jar 实证：callPlayerPortalEvent 无调用者），
     * 需用 PlayerMoveEvent 区域检测补偿；Paper 上保持原事件路径。 */
    private static final boolean FOLIA = isFolia();

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private final ServerFacade server;
    private final PortalPort portalService;
    private final PlayerAuthenticationService authService;
    private final boolean folia;
    private final Map<UUID, Long> lastTransfer = new ConcurrentHashMap<>();

    public PortalEventService(ServerFacade server, PortalPort portalService) {
        this(server, portalService, FOLIA);
    }

    /** 测试用：可注入 folia 模式（真实环境默认自动检测）。 */
    PortalEventService(ServerFacade server, PortalPort portalService, boolean folia) {
        this.server = server;
        this.portalService = portalService;
        this.authService = new PlayerAuthenticationService();
        this.folia = folia;
    }

    /** Paper 路径：PlayerPortalEvent（玩家即将传送门传送）。 */
    public void handle(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        // 检查玩家是否已认证
        if (!authService.isAuthenticated(player)) {
            // 未登录时，不进行传送
            event.setCancelled(true);
            return;
        }

        Location from = event.getFrom();
        String target = portalService.findTarget(from);
        if (target == null) return;
        event.setCancelled(true);
        transfer(player, target);
    }

    /**
     * Folia 补偿路径：PlayerMoveEvent 区域检测（PlayerPortalEvent 在 Folia 26.2 不触发）。
     *
     * <p>进入检测：仅当玩家方块坐标变化（真正走进传送门）才检查，站在传送门内不动不重复触发；
     * 命中传送门内部区域 → 认证通过 → transfer。未认证玩家不拦截（登录插件自行保护）。</p>
     */
    public void handleMove(PlayerMoveEvent event) {
        if (!folia) {
            return;
        }
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null) {
            return;
        }
        // 只有方块坐标变化才算「走进」，避免站在传送门内每 tick 重复触发
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastTransfer.get(player.getUniqueId());
        if (last != null && now - last < TRANSFER_COOLDOWN_MS) {
            return;
        }
        if (!authService.isAuthenticated(player)) {
            return;
        }
        String target = portalService.findTarget(to);
        if (target == null) {
            return;
        }
        lastTransfer.put(player.getUniqueId(), now);
        transfer(player, target);
    }

    private void transfer(Player player, String target) {
        String[] parts = target.split(":");
        String host = parts[0];
        String port = parts.length > 1 ? parts[1] : "25565";
        String cmd = "transfer " + host + " " + port + " " + player.getName();
        server.executeConsoleCommands(() -> {}, cmd);
    }
}
