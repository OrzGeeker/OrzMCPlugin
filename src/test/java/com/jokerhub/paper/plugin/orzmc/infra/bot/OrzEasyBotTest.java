package com.jokerhub.paper.plugin.orzmc.infra.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import com.jokerhub.paper.plugin.orzmc.infra.logging.ThrottledLogger;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WebSocketClientFactory;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WebSocketEventListener;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WsClient;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    void processInboundEvent_rejectsOversizedPayload() {
        bot.processInboundEvent("x".repeat(64 * 1024 + 1));

        verifyNoInteractions(inboundHandler);
    }

    @Test
    void setupWebSocketClient_reloadsWhenConnectionConfigChanges() throws Exception {
        YamlConfiguration config = gatewayConfig();
        ConfigService configService = mock(ConfigService.class);
        when(configService.getConfig("easybot")).thenReturn(config);
        ServerLogger serverLogger = logger("OrzEasyBotReloadTest");
        WsClient first = mock(WsClient.class);
        WsClient second = mock(WsClient.class);
        List<WsClient> clients = List.of(first, second);
        AtomicReference<WebSocketEventListener> listenerRef = new AtomicReference<>();
        AtomicReference<Integer> creates = new AtomicReference<>(0);
        WebSocketClientFactory factory = factoryReturning(clients, listenerRef, creates);
        OrzEasyBot reloadBot = new OrzEasyBot(
                serverLogger,
                configService,
                inboundHandler,
                new PlainMessageFormatter(),
                throttledLogger,
                new HealthRegistry(),
                factory);

        reloadBot.setupWebSocketClient();
        reloadBot.setupWebSocketClient();
        assertEquals(1, creates.get());

        config.set("api_key", "changed-secret");
        reloadBot.reloadConfig();

        assertEquals(2, creates.get());
        verify(first).disconnect();
        verify(second).connect();
    }

    @Test
    void send_routesPublicAndPrivateAndAccepts202() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        List<String> bodies = new ArrayList<>();
        CountDownLatch requests = new CountDownLatch(2);
        server.createContext("/api/v1/messages/send", exchange -> {
            try (InputStream input = exchange.getRequestBody()) {
                bodies.add(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] response = "accepted".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(202, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
            requests.countDown();
        });
        server.start();
        try {
            YamlConfiguration config = gatewayConfig();
            config.set("api_server", "http://127.0.0.1:" + server.getAddress().getPort());
            ConfigService configService = mock(ConfigService.class);
            when(configService.getConfig("easybot")).thenReturn(config);
            HealthRegistry health = new HealthRegistry();
            OrzEasyBot outboundBot = new OrzEasyBot(
                    logger("OrzEasyBotHttpTest"),
                    configService,
                    inboundHandler,
                    new PlainMessageFormatter(),
                    throttledLogger,
                    health,
                    mock(WebSocketClientFactory.class));

            outboundBot.send(MessageEnvelope.publicMessage("public"));
            outboundBot.send(MessageEnvelope.privateMessage("private"));

            assertTrue(requests.await(5, TimeUnit.SECONDS));
            assertTrue(bodies.stream().anyMatch(body -> body.contains("qq:player-chat") && body.contains("public")));
            assertTrue(bodies.stream().anyMatch(body -> body.contains("qq:admin-dm") && body.contains("private")));
            for (int i = 0; i < 50 && !health.getRaw("easybot").httpChecked; i++) {
                Thread.sleep(20);
            }
            assertTrue(health.getRaw("easybot").httpChecked);
            assertTrue(health.getRaw("easybot").httpOk);
            assertTrue(health.getRaw("easybot").apiReady);
        } finally {
            server.stop(0);
        }
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

    private static YamlConfiguration gatewayConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("api_server", "http://127.0.0.1:8080");
        config.set("ws_server", "ws://127.0.0.1:8080");
        config.set("api_key", "secret");
        config.set("http_connect_timeout_seconds", 1);
        config.set("http_request_timeout_seconds", 1);
        config.set("http_max_retries", 0);
        config.set("platforms.qq.enabled", true);
        config.set("platforms.qq.admin_group", "qq:admin-chat");
        config.set("platforms.qq.player_group", "qq:player-chat");
        config.set("platforms.qq.admin_dm", "qq:admin-dm");
        return config;
    }

    private static ServerLogger logger(String name) {
        ServerLogger serverLogger = mock(ServerLogger.class);
        when(serverLogger.logger()).thenReturn(Logger.getLogger(name));
        return serverLogger;
    }

    private static WebSocketClientFactory factoryReturning(
            List<WsClient> clients,
            AtomicReference<WebSocketEventListener> listenerRef,
            AtomicReference<Integer> creates) {
        return (server,
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
            int index = creates.getAndUpdate(value -> value + 1);
            listenerRef.set(listener);
            return clients.get(index);
        };
    }
}
