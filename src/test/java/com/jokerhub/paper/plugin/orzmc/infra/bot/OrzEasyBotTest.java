package com.jokerhub.paper.plugin.orzmc.infra.bot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import com.jokerhub.paper.plugin.orzmc.infra.logging.ThrottledLogger;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WebSocketClientFactory;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WebSocketEventListener;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WsClient;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrzEasyBotTest {
    private BotInboundHandler inboundHandler;
    private ThrottledLogger throttledLogger;
    private OrzEasyBot bot;

    @BeforeEach
    void setUp() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("platforms.qq.enabled", true);
        config.set("platforms.qq.admin_group", "qq:admin-chat");
        config.set("platforms.qq.player_group", "qq:player-chat");
        config.set("platforms.qq.admin_dm", "qq:admin-dm");

        ConfigService configService = mock(ConfigService.class);
        when(configService.getConfig("easybot")).thenReturn(config);
        ServerLogger serverLogger = mock(ServerLogger.class);
        when(serverLogger.logger()).thenReturn(Logger.getLogger("OrzEasyBotTest"));
        inboundHandler = mock(BotInboundHandler.class);
        throttledLogger = mock(ThrottledLogger.class);

        bot = new OrzEasyBot(
                serverLogger,
                configService,
                inboundHandler,
                new PlainMessageFormatter(),
                throttledLogger,
                new HealthRegistry(),
                mock(WebSocketClientFactory.class));
    }

    @Test
    void processInboundEvent_allowsConfiguredPlatformConversation() {
        bot.processInboundEvent(event("qq", "player-chat", "$h", "Member"));

        verify(inboundHandler).handleMessage(eq("$h"), eq(false), any());
    }

    @Test
    void processInboundEvent_allowsConfiguredAdminConversationAndRole() {
        bot.processInboundEvent(event("qq", "qq:admin-chat", "$b", "Admin"));

        verify(inboundHandler).handleMessage(eq("$b"), eq(true), any());
    }

    @Test
    void processInboundEvent_rejectsUnconfiguredConversation() {
        bot.processInboundEvent(event("qq", "unknown-chat", "$h", "Owner"));

        verify(inboundHandler, never()).handleMessage(any(), eq(true), any());
        verify(throttledLogger)
                .warning(eq("easybot-inbound-target"), eq("EasyBot 忽略未授权会话消息: platform=qq, target=qq:unknown-chat"));
    }

    @Test
    void processInboundEvent_normalizesPlatformCase() {
        bot.processInboundEvent(event("QQ", "player-chat", "$h", "Member"));

        verify(inboundHandler).handleMessage(eq("$h"), eq(false), any());
    }

    @Test
    void webSocketBecomesHealthyOnlyAfterAuthentication() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("platforms.qq.enabled", true);
        config.set("platforms.qq.admin_group", "qq:admin-chat");
        config.set("ws_server", "ws://127.0.0.1:8080");
        config.set("api_key", "secret");
        ConfigService configService = mock(ConfigService.class);
        when(configService.getConfig("easybot")).thenReturn(config);
        ServerLogger serverLogger = mock(ServerLogger.class);
        when(serverLogger.logger()).thenReturn(Logger.getLogger("OrzEasyBotAuthTest"));
        HealthRegistry health = new HealthRegistry();
        WsClient wsClient = mock(WsClient.class);
        AtomicReference<WebSocketEventListener> listenerRef = new AtomicReference<>();
        WebSocketClientFactory factory =
                (server,
                        url,
                        logs,
                        retries,
                        baseRetry,
                        maxRetry,
                        jitter,
                        stableReset,
                        logMessages,
                        logThrottle,
                        headers,
                        heartbeat,
                        listener,
                        handler) -> {
                    listenerRef.set(listener);
                    return wsClient;
                };
        OrzEasyBot authBot = new OrzEasyBot(
                serverLogger,
                configService,
                inboundHandler,
                new PlainMessageFormatter(),
                throttledLogger,
                health,
                factory);

        authBot.setupWebSocketClient();
        listenerRef.get().onOpen();
        assertFalse(health.getRaw("easybot").wsConnected);

        authBot.processInboundEvent("{\"type\":\"auth_ok\"}");
        assertTrue(health.getRaw("easybot").wsConnected);

        authBot.processInboundEvent("{\"type\":\"auth_failed\",\"message\":\"bad token\"}");
        assertFalse(health.getRaw("easybot").wsConnected);
        verify(wsClient).disconnect();
    }

    private static String event(String platform, String chatId, String text, String role) {
        return """
                {
                  "type": "event",
                  "event": "message.inbound",
                  "data": {
                    "platform": "%s",
                    "chat_id": "%s",
                    "text": "%s",
                    "sender": {"role": "%s"}
                  }
                }
                """.formatted(platform, chatId, text, role);
    }
}
