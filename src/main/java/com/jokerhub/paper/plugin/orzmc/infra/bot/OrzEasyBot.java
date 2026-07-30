package com.jokerhub.paper.plugin.orzmc.infra.bot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.EasyBotConfig;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import com.jokerhub.paper.plugin.orzmc.infra.logging.ThrottledLogger;
import com.jokerhub.paper.plugin.orzmc.infra.net.AsyncHttp;
import com.jokerhub.paper.plugin.orzmc.infra.ws.DefaultWebSocketClientFactory;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WebSocketClientFactory;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WebSocketEventListener;
import com.jokerhub.paper.plugin.orzmc.infra.ws.WsClient;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * EasyBot IM Gateway 适配器。
 *
 * <p>单一适配器处理所有平台（QQ / Telegram / Discord / 飞书 / 微信），
 * EasyBot 已屏蔽各平台协议差异，业务层只需感知 {@code platform}、{@code text}、{@code sender.role}、{@code chat_id}。
 *
 * <p>入站：单一 WebSocket 连接接收所有平台的事件。
 * 出站：根据 {@link MessageEnvelope.TargetType} 和 {@link EasyBotConfig} 的路由规则确定目标。
 *
 * <p>路由规则：
 * <ul>
 *   <li>PUBLIC → 遍历所有平台的 {@code player_group}（空则降级 {@code admin_group}）</li>
 *   <li>PRIVATE → 遍历所有平台的 {@code admin_dm}</li>
 *   <li>CHANNEL → 查 {@code channels.{key}.{platform}} 映射</li>
 * </ul>
 */
public class OrzEasyBot implements BotMessageService {

    private static final String HEALTH_KEY = "easybot";

    private final ServerLogger logger;
    private final ConfigService configService;
    private final BotInboundHandler inboundHandler;
    private final MessageFormatter formatter;
    private final ThrottledLogger throttledLogger;
    private final HealthRegistry healthRegistry;
    private final WebSocketClientFactory wsFactory;
    private final AtomicBoolean reconnectInFlight = new AtomicBoolean(false);
    private final AtomicInteger httpRequestsInFlight = new AtomicInteger();
    private final AtomicReference<String> pendingHttpError = new AtomicReference<>();

    private WsClient webSocketClient;

    // ---- 构造器 -----------------------------------------------------------

    public OrzEasyBot(
            ServerLogger logger,
            ConfigService configService,
            BotInboundHandler inboundHandler,
            MessageFormatter formatter,
            ThrottledLogger throttledLogger,
            HealthRegistry healthRegistry) {
        this.logger = logger;
        this.configService = configService;
        this.inboundHandler = inboundHandler;
        this.formatter = formatter;
        this.throttledLogger = throttledLogger;
        this.healthRegistry = healthRegistry;
        this.wsFactory = new DefaultWebSocketClientFactory();
    }

    /** 测试用构造器，允许注入模拟的 {@link WebSocketClientFactory}。 */
    OrzEasyBot(
            ServerLogger logger,
            ConfigService configService,
            BotInboundHandler inboundHandler,
            MessageFormatter formatter,
            ThrottledLogger throttledLogger,
            HealthRegistry healthRegistry,
            WebSocketClientFactory wsFactory) {
        this.logger = logger;
        this.configService = configService;
        this.inboundHandler = inboundHandler;
        this.formatter = formatter;
        this.throttledLogger = throttledLogger;
        this.healthRegistry = healthRegistry;
        this.wsFactory = wsFactory == null ? new DefaultWebSocketClientFactory() : wsFactory;
    }

    public boolean isEnable() {
        EasyBotConfig cfg = loadConfig();
        return cfg.enabled();
    }

    @Override
    public void setup() {
        boolean enabled = isEnable();
        healthRegistry.setEnabled(HEALTH_KEY, enabled);
        if (!enabled) {
            return;
        }
        setupWebSocketClient();
    }

    @Override
    public void tearDown() {
        shutdownWebSocketClient();
        healthRegistry.setWsConnected(HEALTH_KEY, false);
        healthRegistry.setHttpOk(HEALTH_KEY, false);
    }

    @Override
    public void tryReconnectIfDisconnected() {
        if (!isEnable() || healthRegistry.getRaw(HEALTH_KEY).wsConnected) {
            return;
        }
        if (!reconnectInFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            healthRegistry.setLastError(HEALTH_KEY, "reconnecting...");
            setupWebSocketClient();
        } finally {
            reconnectInFlight.set(false);
        }
    }

    /**
     * 出站消息路由。
     *
     * <p>根据 {@link MessageEnvelope.TargetType} 确定目标并发送：
     * <ul>
     *   <li>PUBLIC → 各平台 {@code player_group}（空则降级 {@code admin_group}）</li>
     *   <li>PRIVATE → 各平台 {@code admin_dm}（空则跳过）</li>
     *   <li>CHANNEL → {@code channels.{channelKey}} 映射</li>
     * </ul>
     */
    @Override
    public void send(MessageEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        EasyBotConfig cfg = loadConfig();
        if (!cfg.enabled()) {
            return;
        }
        MessageEnvelope.Format fmt = envelope.format() == null ? MessageEnvelope.Format.DEFAULT : envelope.format();
        List<String> parts = formatter.format(envelope.message(), fmt);

        switch (envelope.targetType()) {
            case PUBLIC -> sendPublic(cfg, parts);
            case PRIVATE -> sendPrivate(cfg, parts);
            case CHANNEL -> sendChannel(cfg, envelope.channelKey(), parts);
        }
    }

    // ---- 出站路由 ----------------------------------------------------------

    private void sendPublic(EasyBotConfig cfg, List<String> parts) {
        for (var entry : cfg.platforms().entrySet()) {
            if (!entry.getValue().enabled()) {
                continue;
            }
            String target = resolvePublicTarget(entry.getValue());
            if (target != null && !target.isEmpty()) {
                sendParts(cfg, target, parts);
            }
        }
    }

    private void sendPrivate(EasyBotConfig cfg, List<String> parts) {
        for (var entry : cfg.platforms().entrySet()) {
            if (!entry.getValue().enabled()) {
                continue;
            }
            String target = entry.getValue().adminDm();
            if (target != null && !target.isEmpty()) {
                sendParts(cfg, target, parts);
            }
        }
    }

    private void sendChannel(EasyBotConfig cfg, String channelKey, List<String> parts) {
        if (channelKey == null || channelKey.isEmpty()) {
            throttledLogger.warning("easybot-channel", "EasyBot CHANNEL 消息缺少 channelKey，已拒绝发送");
            return;
        }
        Map<String, String> targets = cfg.channels().get(channelKey);
        if (targets == null || targets.isEmpty()) {
            throttledLogger.warning("easybot-channel", "EasyBot 渠道未配置，已拒绝发送: " + channelKey);
            return;
        }
        for (var entry : targets.entrySet()) {
            // 只发送到已启用的平台
            EasyBotConfig.PlatformEntry platform = cfg.platforms().get(entry.getKey());
            if (platform == null || !platform.enabled()) {
                continue;
            }
            String target = entry.getValue();
            if (target != null && !target.isEmpty()) {
                sendParts(cfg, target, parts);
            }
        }
    }

    /**
     * 解析 PUBLIC 消息的目标 target。
     * 优先使用 {@code playerGroup}，空则降级为 {@code adminGroup}。
     */
    private static String resolvePublicTarget(EasyBotConfig.PlatformEntry entry) {
        String target = entry.playerGroup();
        if (target == null || target.isEmpty()) {
            target = entry.adminGroup();
        }
        return target;
    }

    // ---- HTTP 发送 ---------------------------------------------------------

    private void sendParts(EasyBotConfig cfg, String target, List<String> parts) {
        for (String part : parts) {
            sendToTarget(cfg, target, part);
        }
    }

    private void sendToTarget(EasyBotConfig cfg, String target, String message) {
        if (httpRequestsInFlight.getAndIncrement() == 0) {
            pendingHttpError.set(null);
        }
        healthRegistry.setHttpOk(HEALTH_KEY, false);
        try {
            String url = cfg.apiServer() + "/api/v1/messages/send";
            Map<String, Object> body = new HashMap<>();
            body.put("target", target);
            body.put("text", message);
            body.put("parse_mode", cfg.parseMode());
            String json = new Gson().toJson(body);

            Map<String, String> headers = new HashMap<>();
            if (cfg.apiKey() != null && !cfg.apiKey().isEmpty()) {
                headers.put("Authorization", "Bearer " + cfg.apiKey());
            }

            AsyncHttp.postJson(
                            url,
                            json,
                            headers,
                            Duration.ofSeconds(cfg.httpConnectTimeoutSec() <= 0 ? 3 : cfg.httpConnectTimeoutSec()),
                            Duration.ofSeconds(cfg.httpRequestTimeoutSec() <= 0 ? 3 : cfg.httpRequestTimeoutSec()),
                            Math.max(0, cfg.httpMaxRetries()))
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            completeHttpRequest(null);
                        } else {
                            String error = "HTTP " + response.statusCode() + ": " + response.body();
                            throttledLogger.error(
                                    "easybot-http",
                                    "EasyBot 发送失败, target=" + target + ", status=" + response.statusCode());
                            completeHttpRequest(error);
                        }
                    })
                    .exceptionally(e -> {
                        throttledLogger.error("easybot-http", "EasyBot 发送异常: " + e);
                        completeHttpRequest(e.toString());
                        return null;
                    });
        } catch (Exception e) {
            completeHttpRequest(e.toString());
            logger.logger().info("EasyBot sendToTarget error: " + e);
        }
    }

    private void completeHttpRequest(String error) {
        if (error != null) {
            pendingHttpError.compareAndSet(null, error);
        }
        if (httpRequestsInFlight.decrementAndGet() != 0) {
            return;
        }
        String aggregateError = pendingHttpError.getAndSet(null);
        healthRegistry.setHttpOk(HEALTH_KEY, aggregateError == null);
        healthRegistry.setLastError(HEALTH_KEY, aggregateError);
    }

    // ---- WebSocket 生命周期 ------------------------------------------------

    void setupWebSocketClient() {
        EasyBotConfig cfg = loadConfig();
        if (!cfg.enabled() || cfg.wsServer() == null || cfg.wsServer().isEmpty()) {
            return;
        }
        if (webSocketClient != null) {
            shutdownWebSocketClient();
        }
        try {
            String wsUrl = cfg.wsServer() + "/api/v1/ws";
            int wsRetries = cfg.wsMaxRetries();
            long wsBaseMs = cfg.wsBaseRetryMs();
            long wsMaxMs = cfg.wsMaxDelayMs();
            int wsJitterPercent = cfg.wsJitterPercent();
            long wsStableResetMs = cfg.wsStableResetMs();
            boolean wsMessageLogEnabled = cfg.wsMessageLogEnabled();
            long wsMessageLogThrottleMs = cfg.wsMessageLogThrottleMs();

            // EasyBot 使用 WebSocket PING/PONG 检测存活，无需应用层心跳
            String heartbeatPayload = "";

            String authApiKey = cfg.apiKey();
            webSocketClient = wsFactory.create(
                    logger,
                    wsUrl,
                    throttledLogger,
                    Math.max(0, wsRetries),
                    wsBaseMs <= 0 ? 5000 : wsBaseMs,
                    wsMaxMs <= 0 ? 60000 : wsMaxMs,
                    Math.max(0, wsJitterPercent),
                    wsStableResetMs <= 0 ? 20000 : wsStableResetMs,
                    wsMessageLogEnabled,
                    wsMessageLogThrottleMs <= 0 ? 60000 : wsMessageLogThrottleMs,
                    Collections.emptyMap(),
                    heartbeatPayload,
                    new WebSocketEventListener() {
                        @Override
                        public void onOpen() {
                            healthRegistry.setWsConnected(HEALTH_KEY, false);
                            // EasyBot WS 认证：连接后立即发送 token 帧
                            if (authApiKey != null && !authApiKey.isEmpty()) {
                                String authFrame = new Gson().toJson(Map.of("token", authApiKey));
                                webSocketClient.send(authFrame);
                            }
                        }

                        @Override
                        public void onClose(int code, String reason, boolean remote) {
                            healthRegistry.setWsConnected(HEALTH_KEY, false);
                        }

                        @Override
                        public void onError(Exception ex) {
                            healthRegistry.setWsConnected(HEALTH_KEY, false);
                            healthRegistry.setLastError(HEALTH_KEY, ex.toString());
                            throttledLogger.error("easybot-ws", "EasyBot WebSocket 异常: " + ex);
                        }
                    },
                    this::processInboundEvent);

            webSocketClient.connect();
        } catch (Exception e) {
            healthRegistry.setLastError(HEALTH_KEY, e.toString());
            logger.logger().info("EasyBot WS setup failed: " + e);
        }
    }

    void shutdownWebSocketClient() {
        if (webSocketClient == null) {
            return;
        }
        webSocketClient.disconnect();
        webSocketClient = null;
    }

    // ---- 入站消息处理 -------------------------------------------------------

    /**
     * 处理来自 EasyBot WebSocket 的入站事件。
     *
     * <p>所有平台的消息都通过此单一方法处理。EasyBot 已屏蔽协议差异，
     * 统一为 {platform, text, sender.role, chat_id} 格式。
     *
     * <p>系统帧（auth_ok / auth_failed / lagged）在此直接处理，不会传递到业务层。
     */
    void processInboundEvent(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            if (!root.has("type")) {
                return;
            }
            String type = root.get("type").getAsString();

            // ---- 系统帧处理 ----
            if ("auth_ok".equals(type)) {
                healthRegistry.setWsConnected(HEALTH_KEY, true);
                healthRegistry.setLastError(HEALTH_KEY, null);
                throttledLogger.info("easybot-ws-auth", "EasyBot WebSocket 认证成功");
                return;
            }
            if ("auth_failed".equals(type)) {
                healthRegistry.setWsConnected(HEALTH_KEY, false);
                String msg = root.has("message") ? root.get("message").getAsString() : "unknown";
                healthRegistry.setLastError(HEALTH_KEY, "WS auth failed: " + msg);
                throttledLogger.error("easybot-ws-auth", "EasyBot WebSocket 认证失败: " + msg);
                shutdownWebSocketClient();
                return;
            }
            if ("lagged".equals(type)) {
                int dropped = root.has("dropped") ? root.get("dropped").getAsInt() : 0;
                throttledLogger.warning("easybot-ws-lag", "EasyBot WS 事件丢失: " + dropped);
                return;
            }

            // ---- 只处理事件帧 ----
            if (!"event".equals(type)) {
                return;
            }
            if (!root.has("event")) {
                return;
            }
            String eventType = root.get("event").getAsString();
            if (!"message.inbound".equals(eventType)) {
                return;
            }

            // ---- 解析消息数据 ----
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return;
            }
            JsonObject data = root.getAsJsonObject("data");

            // platform: 标识来源平台，如 "qq", "discord", "telegram"
            if (!data.has("platform")) {
                return;
            }
            String platform = data.get("platform").getAsString().toLowerCase(java.util.Locale.ROOT);
            EasyBotConfig cfg = loadConfig();

            // 跳过已禁用平台的消息
            if (!isPlatformEnabled(cfg, platform)) {
                return;
            }

            // text: 消息内容
            String text = data.has("text") && !data.get("text").isJsonNull()
                    ? data.get("text").getAsString().trim()
                    : "";
            if (text.isEmpty()) {
                return;
            }

            // chat_id: 来源会话标识
            String chatId = data.has("chat_id") && !data.get("chat_id").isJsonNull()
                    ? data.get("chat_id").getAsString()
                    : "";
            if (chatId.isEmpty()) {
                return;
            }
            String replyTarget = normalizeTarget(platform, chatId);
            if (!isInboundTargetAllowed(cfg, platform, replyTarget)) {
                throttledLogger.warning(
                        "easybot-inbound-target",
                        "EasyBot 忽略未授权会话消息: platform=" + platform + ", target=" + replyTarget);
                return;
            }

            // sender.role: 发送者角色（EasyBot 已各平台标准化）
            boolean isAdmin = false;
            if (data.has("sender") && data.get("sender").isJsonObject()) {
                JsonObject sender = data.getAsJsonObject("sender");
                if (sender.has("role") && !sender.get("role").isJsonNull()) {
                    String role = sender.get("role").getAsString();
                    isAdmin = "Owner".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role);
                }
            }

            // 关键：sink 捕获来源平台和会话，确保回复定向到正确的位置
            Consumer<MessageEnvelope> sink = env -> {
                if (env != null) {
                    MessageEnvelope.Format replyFormat =
                            env.format() == null ? MessageEnvelope.Format.DEFAULT : env.format();
                    for (String part : formatter.format(env.message(), replyFormat)) {
                        sendToTarget(cfg, replyTarget, part);
                    }
                }
            };

            BotInboundDispatcher.dispatch(inboundHandler, text, isAdmin, sink);
        } catch (Exception e) {
            healthRegistry.setLastError(HEALTH_KEY, e.toString());
            logger.logger().info("EasyBot inbound parse error: " + e);
        }
    }

    // ---- 辅助方法 ----------------------------------------------------------

    private EasyBotConfig loadConfig() {
        return EasyBotConfig.from(configService.getConfig("easybot"));
    }

    /**
     * 检查指定平台是否已在配置中启用。
     * 未找到配置的平台（如未注册的测试平台）视为禁用。
     */
    private boolean isPlatformEnabled(EasyBotConfig cfg, String platform) {
        EasyBotConfig.PlatformEntry entry = cfg.platforms().get(platform);
        return entry != null && entry.enabled();
    }

    private boolean isInboundTargetAllowed(EasyBotConfig cfg, String platform, String target) {
        EasyBotConfig.PlatformEntry entry = cfg.platforms().get(platform);
        if (entry == null || !entry.enabled()) {
            return false;
        }
        if (target.equals(entry.adminGroup()) || target.equals(entry.playerGroup()) || target.equals(entry.adminDm())) {
            return true;
        }
        return cfg.channels().values().stream()
                .map(targets -> targets.get(platform))
                .anyMatch(target::equals);
    }

    private static String normalizeTarget(String platform, String chatId) {
        String prefix = platform + ":";
        return chatId.startsWith(prefix) ? chatId : prefix + chatId;
    }
}
