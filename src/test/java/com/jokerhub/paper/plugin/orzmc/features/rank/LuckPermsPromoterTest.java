package com.jokerhub.paper.plugin.orzmc.features.rank;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerAccess;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** LuckPermsPromoter 测试：LP 可用性检测 + 晋升命令派发。 */
class LuckPermsPromoterTest {

    private ServerAccess serverAccess;
    private Server server;
    private PluginManager pluginManager;
    private LuckPermsPromoter promoter;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        serverAccess = mock(ServerAccess.class);
        server = mock(Server.class);
        pluginManager = mock(PluginManager.class);
        when(serverAccess.server()).thenReturn(server);
        promoter = new LuckPermsPromoter(serverAccess, u -> "TestPlayer");
    }

    private MockedStatic<org.bukkit.Bukkit> mockBukkit(boolean lpEnabled) {
        MockedStatic<org.bukkit.Bukkit> mocked = mockStatic(org.bukkit.Bukkit.class);
        mocked.when(() -> org.bukkit.Bukkit.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.isPluginEnabled("LuckPerms")).thenReturn(lpEnabled);
        return mocked;
    }

    @Test
    void isLuckPermsEnabled_lpLoaded_returnsTrue() {
        try (MockedStatic<org.bukkit.Bukkit> mocked = mockBukkit(true)) {
            assertTrue(promoter.isLuckPermsEnabled());
        }
    }

    @Test
    void isLuckPermsEnabled_lpMissing_returnsFalse() {
        try (MockedStatic<org.bukkit.Bukkit> mocked = mockBukkit(false)) {
            assertFalse(promoter.isLuckPermsEnabled());
        }
    }

    @Test
    void promoteToNext_lpMissing_skipsDispatch() {
        try (MockedStatic<org.bukkit.Bukkit> mocked = mockBukkit(false)) {
            promoter.promoteToNext(id);
            verify(server, never()).dispatchCommand(any(), anyString());
        }
    }

    @Test
    void promoteToNext_lpLoaded_dispatchesPromote() {
        try (MockedStatic<org.bukkit.Bukkit> mocked = mockBukkit(true)) {
            promoter.promoteToNext(id);
            verify(server).dispatchCommand(any(), eq("lp user TestPlayer promote rank"));
        }
    }

    @Test
    void promoteToBuilder_lpMissing_skipsDispatch() {
        try (MockedStatic<org.bukkit.Bukkit> mocked = mockBukkit(false)) {
            promoter.promoteToBuilder(id);
            verify(server, never()).dispatchCommand(any(), anyString());
        }
    }

    @Test
    void promoteToBuilder_lpLoaded_dispatchesParentAdd() {
        try (MockedStatic<org.bukkit.Bukkit> mocked = mockBukkit(true)) {
            promoter.promoteToBuilder(id);
            verify(server).dispatchCommand(any(), eq("lp user TestPlayer parent add builder"));
        }
    }
}
