package com.jokerhub.paper.plugin.orzmc.features.botcommands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.player.PlayerDisplayNames;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * $l/$w 列表类回复组装测试（i18n P4a：正文经 bot.list.* 组装为 {message}，模板仅直通）。
 *
 * <p>语言包走 {@link I18nServiceHolder} 未注入时的 zh 默认回落（bot.list.* 已备），断言按 zh 原文。</p>
 */
class BotCommandListFeedbackServiceTest {

    private ServerFacade serverFacade;
    private TypedConfigProvider configs;
    private Server server;
    private BotCommandListFeedbackService service;

    private MockedStatic<PlayerDisplayNames> displayNamesMock;

    @BeforeEach
    void setUp() {
        serverFacade = mock(ServerFacade.class);
        server = mock(Server.class);
        configs = mock(TypedConfigProvider.class);
        when(serverFacade.server()).thenReturn(server);

        displayNamesMock = mockStatic(PlayerDisplayNames.class);

        service = new BotCommandListFeedbackService(serverFacade, configs);
    }

    @AfterEach
    void tearDown() {
        displayNamesMock.close();
    }

    @Test
    void onlineVars_returnsCorrectMapping() {
        var online = new BotCommandListFeedbackService.OnlineList("Alice\nBob", "fallback", "header", "2", "20");
        Map<String, String> vars = service.onlineVars(online);

        assertEquals("2", vars.get("online_count"));
        assertEquals("20", vars.get("max_count"));
        assertEquals("Alice\nBob", vars.get("online_list"));
        // {message} = 语言包组装的完整文案（record.fallback），供模板直通渲染
        assertEquals("fallback", vars.get("message"));
    }

    @Test
    void buildOnlineList_withPlayers_formatsNames() {
        Player alice = mock(Player.class);
        Player bob = mock(Player.class);
        ArrayList<Player> players = new ArrayList<>();
        players.add(alice);
        players.add(bob);

        displayNamesMock.when(() -> PlayerDisplayNames.format(alice, null)).thenReturn("§aAlice");
        displayNamesMock.when(() -> PlayerDisplayNames.format(bob, null)).thenReturn("§bBob");

        BotCommandListFeedbackService.OnlineList result = service.buildOnlineList(players, 20);

        assertEquals("§aAlice\n§bBob", result.list());
        assertEquals("2", result.onlineCount());
        assertEquals("20", result.maxCount());
        // fallback = 完整文案：zh 标题 + 在线列表（无需模板即可构成直通输出）
        assertTrue(result.fallback().contains("------当前在线(2/20)------"));
        assertTrue(result.fallback().contains("§aAlice"));
        assertTrue(result.fallback().contains("§bBob"));
        // message 变量即完整文案
        assertEquals(result.fallback(), service.onlineVars(result).get("message"));
    }

    @Test
    void buildOnlineList_emptyPlayers_returnsEmptyList() {
        ArrayList<Player> players = new ArrayList<>();

        BotCommandListFeedbackService.OnlineList result = service.buildOnlineList(players, 10);

        assertEquals("", result.list());
        assertEquals("0", result.onlineCount());
        assertEquals("10", result.maxCount());
        // 空列表时完整文案 = 标题本身（无尾随空列表行）
        assertTrue(result.fallback().contains("------当前在线(0/10)------"));
        assertFalse(result.fallback().endsWith("\n"));
    }

    @Test
    void buildWhitelistHeader_returnsHeaderWithCount() {
        when(configs.resolveTemplate(eq("command_whitelist_header"), anyString()))
                .thenAnswer(i -> i.getArgument(1));

        BotCommandListFeedbackService.WhitelistHeader result = service.buildWhitelistHeader(42);

        // 模板直通（fallback 原样）→ header = 语言包 zh 白名单标题
        assertTrue(result.header().contains("------当前白名单玩家(42)------"));
    }

    @Test
    void whitelistHeaderVars_containsCountAndMessage() {
        Map<String, String> vars = service.whitelistHeaderVars(7);
        assertEquals("7", vars.get("count"));
        assertEquals("------当前白名单玩家(7)------", vars.get("message"));
    }

    @Test
    void buildCleanupNotice_includesRemovedNames() {
        Set<String> removed = Set.of("Alice", "Bob");

        BotCommandListFeedbackService.CleanupNotice result = service.buildCleanupNotice(removed);

        assertTrue(result.fallback().contains("------白名单清理------"));
        assertTrue(result.fallback().contains("Alice"));
        assertTrue(result.fallback().contains("Bob"));
        // message 变量 = 标题 + 移除名单完整文案
        assertEquals(result.fallback(), service.cleanupVars(result).get("message"));
        assertTrue(service.cleanupVars(result).get("removed_list").contains("Alice"));
    }

    @Test
    void buildWhitelistPage_containsHeaderAndPage() {
        BotCommandListFeedbackService.WhitelistPage result =
                service.buildWhitelistPage("白名单列表", 2, 3, "player1\nplayer2");

        // 完整文案 = 头 + 页 meta（语言包 zh）+ 正文，供模板 {message} 直通
        assertTrue(result.fallback().contains("白名单列表"));
        assertTrue(result.fallback().contains("第2/3页"));
        assertTrue(result.fallback().contains("player1"));
        assertEquals("player1\nplayer2", result.vars().get("body"));
        assertEquals("2", result.vars().get("page"));
        assertEquals("3", result.vars().get("total"));
        assertEquals("白名单列表", result.vars().get("header"));
        assertEquals(result.fallback(), result.vars().get("message"));
    }

    @Test
    void currentOnlinePlayers_returnsPlayerList() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        HashSet<Player> onlinePlayers = new HashSet<>();
        onlinePlayers.add(p1);
        onlinePlayers.add(p2);
        doReturn(onlinePlayers).when(server).getOnlinePlayers();

        ArrayList<Player> result = service.currentOnlinePlayers();

        assertEquals(2, result.size());
    }
}
