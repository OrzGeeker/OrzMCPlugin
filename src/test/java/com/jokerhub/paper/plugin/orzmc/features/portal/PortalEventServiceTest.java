package com.jokerhub.paper.plugin.orzmc.features.portal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.ports.portal.PortalPort;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.testutil.ServiceTestBase;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PortalEventService 测试：PlayerPortalEvent（Paper 路径）+ PlayerMoveEvent（Folia 补偿路径）。
 */
class PortalEventServiceTest extends ServiceTestBase {

    private ServerFacade server;
    private PortalPort portalService;
    private World world;
    private Player player;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        server = mock(ServerFacade.class);
        portalService = mock(PortalPort.class);
        world = mock(World.class);
        when(world.getName()).thenReturn("world");
        player = mock(Player.class);
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);
    }

    private Location loc(double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    // ---- Paper 路径：PlayerPortalEvent ----

    @Test
    void portalEvent_matchingTarget_cancelsAndTransfers() {
        when(portalService.findTarget(any(Location.class))).thenReturn("127.0.0.1:25566");
        PortalEventService service = new PortalEventService(server, portalService);

        PlayerPortalEvent event = new PlayerPortalEvent(
                player, loc(100, 64, 100), loc(100, 64, 100), TeleportCause.NETHER_PORTAL, 1, true, 1);
        service.handle(event);

        assertTrue(event.isCancelled());
        verify(server).executeConsoleCommands(any(), eq("transfer 127.0.0.1 25566 TestPlayer"));
    }

    @Test
    void portalEvent_noTarget_letsVanillaProceed() {
        when(portalService.findTarget(any(Location.class))).thenReturn(null);
        PortalEventService service = new PortalEventService(server, portalService);

        PlayerPortalEvent event = new PlayerPortalEvent(
                player, loc(100, 64, 100), loc(100, 64, 100), TeleportCause.NETHER_PORTAL, 1, true, 1);
        service.handle(event);

        assertFalse(event.isCancelled());
        verify(server, never()).executeConsoleCommands(any(), anyString());
    }

    // ---- Folia 补偿路径：PlayerMoveEvent ----

    @Test
    void moveEvent_paperMode_ignored() {
        // Paper 上仅 PlayerPortalEvent 生效，move 检测不注册逻辑
        PortalEventService service = new PortalEventService(server, portalService, false);
        PlayerMoveEvent event = new PlayerMoveEvent(player, loc(100, 64, 100), loc(101, 64, 100));

        service.handleMove(event);

        verify(portalService, never()).findTarget(any());
        verify(server, never()).executeConsoleCommands(any(), anyString());
    }

    @Test
    void moveEvent_foliaMode_walkingIntoPortal_transfers() {
        when(portalService.findTarget(any(Location.class))).thenReturn("127.0.0.1:25566");
        PortalEventService service = new PortalEventService(server, portalService, true);
        // 从传送门外（100,64,100）走进传送门方块（101,64,100）
        PlayerMoveEvent event = new PlayerMoveEvent(player, loc(100, 64, 100), loc(101, 64, 100));

        service.handleMove(event);

        verify(portalService).findTarget(loc(101, 64, 100));
        verify(server).executeConsoleCommands(any(), eq("transfer 127.0.0.1 25566 TestPlayer"));
    }

    @Test
    void moveEvent_standingInPortal_noBlockChange_skipped() {
        // 只有方块坐标变化才算走进；站在传送门内不动（例如只转头）不触发
        PortalEventService service = new PortalEventService(server, portalService, true);
        PlayerMoveEvent event = new PlayerMoveEvent(player, loc(101, 64, 100), loc(101.1, 64, 100));

        service.handleMove(event);

        verify(portalService, never()).findTarget(any());
        verify(server, never()).executeConsoleCommands(any(), anyString());
    }

    @Test
    void moveEvent_outsidePortal_noTransfer() {
        when(portalService.findTarget(any(Location.class))).thenReturn(null);
        PortalEventService service = new PortalEventService(server, portalService, true);
        PlayerMoveEvent event = new PlayerMoveEvent(player, loc(10, 64, 10), loc(11, 64, 10));

        service.handleMove(event);

        verify(server, never()).executeConsoleCommands(any(), anyString());
    }

    @Test
    void moveEvent_cooldown_skipsRepeatedTransfer() {
        when(portalService.findTarget(any(Location.class))).thenReturn("127.0.0.1:25566");
        PortalEventService service = new PortalEventService(server, portalService, true);
        service.handleMove(new PlayerMoveEvent(player, loc(100, 64, 100), loc(101, 64, 100)));
        verify(server, times(1)).executeConsoleCommands(any(), anyString());

        // 5 秒冷却内再次移动（方块变化）→ 不重复 transfer
        PlayerMoveEvent second = new PlayerMoveEvent(player, loc(101, 64, 100), loc(102, 64, 100));
        service.handleMove(second);

        verify(server, times(1)).executeConsoleCommands(any(), anyString());
    }

    @Test
    void moveEvent_afterCooldown_transfersAgain() throws Exception {
        when(portalService.findTarget(any(Location.class))).thenReturn("127.0.0.1:25566");
        PortalEventService service = new PortalEventService(server, portalService, true);
        service.handleMove(new PlayerMoveEvent(player, loc(100, 64, 100), loc(101, 64, 100)));
        verify(server, times(1)).executeConsoleCommands(any(), anyString());

        // 冷却（5s）过期后再次走进传送门 → 允许再次 transfer
        Thread.sleep(5100);
        service.handleMove(new PlayerMoveEvent(player, loc(101, 64, 100), loc(102, 64, 100)));
        verify(server, times(2)).executeConsoleCommands(any(), anyString());
    }
}
