package com.jokerhub.paper.plugin.orzmc.features.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.rank.RankService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.PlayerNotifyConfig;
import com.jokerhub.paper.plugin.orzmc.infra.notify.Notifier;
import com.jokerhub.paper.plugin.orzmc.infra.player.OnlineListFormatter;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.testutil.ServiceTestBase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

class PlayerEventAggregatorTest extends ServiceTestBase {

    @Mock
    private ServerFacade server;

    @Mock
    private TypedConfigProvider configs;

    @Mock
    private Notifier notifier;

    @Mock
    private Server bukkitServer;

    @Mock
    private Logger logger;

    private OnlineListFormatter formatter;
    private PlayerEventAggregator aggregator;

    @BeforeEach
    void setUp() {
        formatter = new OnlineListFormatter();
        when(configs.playerNotify()).thenReturn(new PlayerNotifyConfig(true, true, true, 3000L, 6, false));
        when(server.server()).thenReturn(bukkitServer);
        when(server.logger()).thenReturn(logger);
        when(configs.renderEvent(anyString(), anyMap())).thenReturn(MessageEnvelope.publicMessage("ok"));
        aggregator = new PlayerEventAggregator(server, configs, notifier, formatter);
    }

    private Player mockPlayer(String name) {
        Player p = mock(Player.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getName()).thenReturn(name);
        when(p.getPlayerProfile()).thenReturn(profile);
        when(p.getName()).thenReturn(name);
        when(p.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(p.isOp()).thenReturn(false);
        return p;
    }

    /** 执行已捕获的尾部冲刷任务（模拟调度器在窗口到期后运行）。仅适用于恰好调度了一次的场景。 */
    private void runTail() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(server).runLater(task.capture(), anyLong());
        task.getValue().run();
    }

    // ---- 单发：窗口内仅 1 条事件，复用原模板 ----

    @Test
    void enqueue_singleJoin_flushRendersPlayerJoin() {
        Player p = mockPlayer("Alice");
        doReturn(List.of(p)).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_join"), anyMap())).thenReturn(MessageEnvelope.publicMessage("ok"));

        aggregator.enqueue(p, PlayerEventService.PlayerState.JOIN);

        // 纯聚合：事件不立即发送，仅调度一次窗口冲刷（3000ms → 60 ticks）
        verify(notifier, never()).event(anyString(), any(MessageEnvelope.class));
        verify(server).runLater(any(Runnable.class), eq(60L));
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_join"), vars.capture());
        verify(notifier).event(eq("player_join"), any(MessageEnvelope.class));
        Map<String, String> v = vars.getValue();
        // name 沿用 OnlineListFormatter.line()（玩家名 + 游戏模式），与旧 PlayerEventService 行为一致
        assertTrue(v.get("name").startsWith("Alice"), "got: " + v.get("name"));
        assertEquals("1", v.get("online_count"));
        assertEquals("100", v.get("max_count"));
        assertTrue(v.get("online_list").contains("Alice"), "got: " + v.get("online_list"));
    }

    @Test
    void enqueue_singleQuit_flushUsesLiveOnlineCount() {
        Player quitter = mockPlayer("Alice");
        Player remaining = mockPlayer("Bob");
        // 冲刷时刻当事人已离开在线列表（与事件同步渲染的"减1修正"相反）
        doReturn(List.of(remaining)).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_quit"), anyMap())).thenReturn(MessageEnvelope.publicMessage("ok"));

        aggregator.enqueue(quitter, PlayerEventService.PlayerState.QUIT);
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_quit"), vars.capture());
        Map<String, String> v = vars.getValue();
        assertEquals("1", v.get("online_count"), "应取冲刷时刻实时在线数，不重复减 1");
        String onlineList = v.get("online_list");
        assertFalse(onlineList.contains("Alice"), "当事人不应出现在在线列表, got: " + onlineList);
        assertTrue(onlineList.contains("Bob"), "got: " + onlineList);
    }

    @Test
    void enqueue_singleKick_flushRendersPlayerKick() {
        Player p = mockPlayer("Alice");
        doReturn(List.of()).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_kick"), anyMap())).thenReturn(MessageEnvelope.publicMessage("ok"));

        aggregator.enqueue(p, PlayerEventService.PlayerState.KICK);
        runTail();

        verify(configs).renderEvent(eq("player_kick"), anyMap());
        verify(notifier).event(eq("player_kick"), any(MessageEnvelope.class));
    }

    @Test
    void enqueue_singleJoin_withRankService_includesGroupInList() {
        // 在线列表格式与权限组注入走真实 OnlineListFormatter（缺组名回归保护）
        RankService rankService = mock(RankService.class);
        OnlineListFormatter formatterWithRank = new OnlineListFormatter();
        formatterWithRank.setRankService(rankService);
        aggregator = new PlayerEventAggregator(server, configs, notifier, formatterWithRank);

        Player p1 = mockPlayer("Alice");
        Player p2 = mockPlayer("Bob");
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(p1.getUniqueId()).thenReturn(id1);
        when(p2.getUniqueId()).thenReturn(id2);
        when(rankService.currentGroup(id1)).thenReturn("admin");
        when(rankService.currentGroup(id2)).thenReturn("builder");

        doReturn(List.of(p1, p2)).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_join"), anyMap())).thenReturn(MessageEnvelope.publicMessage("ok"));

        aggregator.enqueue(p1, PlayerEventService.PlayerState.JOIN);
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_join"), vars.capture());
        String onlineList = vars.getValue().get("online_list");
        assertTrue(onlineList.contains("Alice"), "got: " + onlineList);
        assertTrue(onlineList.contains("管理员"), "Alice 应显示权限组 管理员, got: " + onlineList);
        assertTrue(onlineList.contains("Bob"), "got: " + onlineList);
        assertTrue(onlineList.contains("建造者"), "Bob 应显示权限组 建造者, got: " + onlineList);
    }

    // ---- 多发：窗口内多条事件，渲染聚合摘要（不丢消息，精确计数）----

    @Test
    void enqueue_multipleEvents_flushRendersDigestWithExactCounts() {
        Player a = mockPlayer("Alice");
        Player b = mockPlayer("Bob");
        Player c = mockPlayer("Carol");
        doReturn(List.of(a, b, c)).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_digest"), anyMap())).thenReturn(MessageEnvelope.publicMessage("digest"));

        aggregator.enqueue(a, PlayerEventService.PlayerState.JOIN);
        aggregator.enqueue(b, PlayerEventService.PlayerState.JOIN);
        aggregator.enqueue(c, PlayerEventService.PlayerState.QUIT);
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_digest"), vars.capture());
        verify(notifier).event(eq("player_digest"), any(MessageEnvelope.class));
        Map<String, String> v = vars.getValue();
        assertTrue(v.get("join_summary").startsWith("🟢 +2 上线"), "got: " + v.get("join_summary"));
        assertTrue(v.get("join_summary").contains("Alice"), "got: " + v.get("join_summary"));
        assertTrue(v.get("join_summary").contains("Bob"), "got: " + v.get("join_summary"));
        assertTrue(v.get("quit_summary").startsWith("🔴 -1 下线"), "got: " + v.get("quit_summary"));
        assertEquals("", v.get("kick_summary"));
        assertEquals("3", v.get("online_count"));
    }

    @Test
    void enqueue_manyEvents_truncatesNamesOnlyCountExact() {
        when(configs.playerNotify()).thenReturn(new PlayerNotifyConfig(true, true, true, 3000L, 6, false));
        doReturn(List.of()).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_digest"), anyMap())).thenReturn(MessageEnvelope.publicMessage("digest"));

        for (int i = 0; i < 10; i++) {
            aggregator.enqueue(mockPlayer("P" + i), PlayerEventService.PlayerState.JOIN);
        }
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_digest"), vars.capture());
        String joinSummary = vars.getValue().get("join_summary");
        // 计数精确（+10），名称仅显示前 6 个并截断
        assertTrue(joinSummary.startsWith("🟢 +10 上线"), "got: " + joinSummary);
        assertTrue(joinSummary.contains("P0") && joinSummary.contains("P5"), "got: " + joinSummary);
        assertFalse(joinSummary.contains("P6"), "P6 应被截断: " + joinSummary);
        assertTrue(joinSummary.contains("等4人"), "got: " + joinSummary);
    }

    @Test
    void enqueue_digestIncludeOnlineList_attachesList() {
        when(configs.playerNotify()).thenReturn(new PlayerNotifyConfig(true, true, true, 3000L, 6, true));
        Player a = mockPlayer("Alice");
        Player b = mockPlayer("Bob");
        doReturn(List.of(a, b)).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_digest"), anyMap())).thenReturn(MessageEnvelope.publicMessage("digest"));

        aggregator.enqueue(a, PlayerEventService.PlayerState.JOIN);
        aggregator.enqueue(b, PlayerEventService.PlayerState.JOIN);
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_digest"), vars.capture());
        String onlineList = vars.getValue().get("online_list");
        assertTrue(onlineList.startsWith("\n"), "应带换行前缀: " + onlineList);
        assertTrue(onlineList.contains("Alice") && onlineList.contains("Bob"), "got: " + onlineList);
    }

    @Test
    void enqueue_digestIncludeOnlineList_false_omitsList() {
        // 默认 includeOnlineList=false：摘要不含在线列表
        Player a = mockPlayer("Alice");
        doReturn(List.of(a)).when(bukkitServer).getOnlinePlayers();
        when(bukkitServer.getMaxPlayers()).thenReturn(100);
        when(configs.renderEvent(eq("player_digest"), anyMap())).thenReturn(MessageEnvelope.publicMessage("digest"));

        aggregator.enqueue(a, PlayerEventService.PlayerState.JOIN);
        aggregator.enqueue(mockPlayer("Bob"), PlayerEventService.PlayerState.QUIT);
        runTail();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(configs).renderEvent(eq("player_digest"), vars.capture());
        assertEquals("", vars.getValue().get("online_list"));
    }

    // ---- 限流上界与窗口行为 ----

    @Test
    void enqueue_multipleWithinWindow_singleScheduledFlush() {
        aggregator.enqueue(mockPlayer("A"), PlayerEventService.PlayerState.JOIN);
        aggregator.enqueue(mockPlayer("B"), PlayerEventService.PlayerState.JOIN);
        aggregator.enqueue(mockPlayer("C"), PlayerEventService.PlayerState.QUIT);

        verify(server, times(1)).runLater(any(Runnable.class), anyLong());
        verify(notifier, never()).event(anyString(), any(MessageEnvelope.class));
    }

    @Test
    void enqueue_windowMs_convertsToTicks() {
        when(configs.playerNotify()).thenReturn(new PlayerNotifyConfig(true, true, true, 5000L, 6, false));

        aggregator.enqueue(mockPlayer("A"), PlayerEventService.PlayerState.JOIN);

        verify(server).runLater(any(Runnable.class), eq(100L)); // 5000ms / 50
    }

    @Test
    void enqueue_configReloaded_usesNewWindow() {
        // 首个窗口 3000ms → 60 ticks
        aggregator.enqueue(mockPlayer("A"), PlayerEventService.PlayerState.JOIN);
        runTail();

        // 热重载：新窗口 5000ms → 100 ticks，不重建 service
        when(configs.playerNotify()).thenReturn(new PlayerNotifyConfig(true, true, true, 5000L, 6, false));
        aggregator.enqueue(mockPlayer("B"), PlayerEventService.PlayerState.JOIN);

        verify(server).runLater(any(Runnable.class), eq(100L));
    }

    @Test
    void enqueue_disabledState_suppressed() {
        when(configs.playerNotify()).thenReturn(new PlayerNotifyConfig(false, true, true, 3000L, 6, false));

        aggregator.enqueue(mockPlayer("Alice"), PlayerEventService.PlayerState.JOIN);

        verify(server, never()).runLater(any(Runnable.class), anyLong());
        verifyNoInteractions(notifier);
    }

    @Test
    void enqueue_renderFailure_flushClearsBatch_noOrphan() {
        doThrow(new IllegalStateException("template broken")).when(configs).renderEvent(anyString(), anyMap());
        aggregator.enqueue(mockPlayer("A"), PlayerEventService.PlayerState.JOIN);

        // 尾部冲刷渲染失败 → 异常冒出，但批次已清除不留孤儿（后续事件可正常聚合）
        assertThrows(IllegalStateException.class, () -> runTail());

        doReturn(MessageEnvelope.publicMessage("ok")).when(configs).renderEvent(anyString(), anyMap());
        aggregator.enqueue(mockPlayer("B"), PlayerEventService.PlayerState.JOIN);
        verify(server, times(2)).runLater(any(Runnable.class), anyLong());
    }
}
