package com.jokerhub.paper.plugin.orzmc.infra.bot.builtin;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerScheduler;
import com.jokerhub.paper.plugin.orzmc.infra.bot.BotMessageService;
import com.jokerhub.paper.plugin.orzmc.infra.bot.ImBindings;
import com.jokerhub.paper.plugin.orzmc.infra.bot.ImConversation;
import com.jokerhub.paper.plugin.orzmc.infra.bot.ImDiscoveryCandidates;
import com.jokerhub.paper.plugin.orzmc.infra.bot.ImMessageRouter;
import com.jokerhub.paper.plugin.orzmc.infra.bot.MessageFormatter;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.QqPlatformConfig;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * builtin 双通道驱动（方案 §2/§3：BotMessageService 的第二个实现，backend=builtin 时由 Provider 返回）。
 *
 * <p>按 im.yml 平台配置持有各平台 {@link PlatformSlot}（单平台槽，每平台一个，批次 4 起逐平台注册）；
 * 会话绑定每次实时读 im_bindings.yml（S8 命令写入后 reload 即生效，无需重启）：</p>
 * <ul>
 *   <li>出站：{@link #send} 按信封 PUBLIC/PRIVATE 经共享路由（ImMessageRouter）解析目标并逐平台投递；
 *       无可用平台或无可投递目标 → 静默跳过（广播语义与 EasyBot 一致，R10：单平台失败不阻塞其他平台）；</li>
 *   <li>入站：平台适配器内部接 BotInboundHandler（QQ 经 QqInboundProcessor，R12 调度到服务器线程）；
 *       本驱动把 im_bindings 会话按需喂给适配器做门槛判定；</li>
 *   <li>生命周期：{@link #reloadConfig} reconcile 所有槽——配置可用则（重建并）启动，不可用则停止并降级
 *       健康（D3：无任何可用平台时 Provider 直接返回 Unavailable 停群，不自动回退）。</li>
 * </ul>
 */
public final class BuiltinImDriver implements BotMessageService {

    private final ServerLogger logger;
    private final ConfigService configService;
    private final MessageFormatter formatter;
    /** 未绑定会话自动发现候选（D11：status 候选/控制台日志提示）。 */
    private final ImDiscoveryCandidates discovery = new ImDiscoveryCandidates();

    private final List<PlatformSlot<?>> slots = new ArrayList<>();

    /** 生产入口：注册 QQ 平台槽（工厂闭包引用本实例方法，会话绑定实时读）。 */
    public BuiltinImDriver(
            ServerLogger logger,
            ServerScheduler scheduler,
            ConfigService configService,
            BotInboundHandler inbound,
            MessageFormatter formatter,
            HealthRegistry health) {
        this(logger, configService, formatter);
        registerQq(cfg -> new QqBuiltinAdapter(
                logger,
                scheduler,
                inbound,
                formatter,
                () -> this.bindings().conversation("qq"),
                health,
                cfg,
                this.discovery));
    }

    /** 测试用：注入替身 QQ 平台工厂（避免单元测试触发真实网络）。 */
    BuiltinImDriver(
            ServerLogger logger,
            ConfigService configService,
            MessageFormatter formatter,
            Function<QqPlatformConfig, BuiltinPlatform> qqFactory) {
        this(logger, configService, formatter);
        registerQq(qqFactory);
    }

    /** 测试用：不注册任何平台（后续经 {@link #register} 增补，用于多平台路由用例）。 */
    BuiltinImDriver(ServerLogger logger, ConfigService configService, MessageFormatter formatter) {
        this.logger = logger;
        this.configService = configService;
        this.formatter = formatter;
    }

    private void registerQq(Function<QqPlatformConfig, BuiltinPlatform> factory) {
        register(new PlatformSlot<>("qq", cs -> readQqConfig(), QqPlatformConfig::usable, factory, logger));
    }

    /** 注册平台槽（幂等：同名替换）。测试与后续平台（飞书等）经此挂载。 */
    void register(PlatformSlot<?> slot) {
        synchronized (slots) {
            slots.removeIf(existing -> existing.platform().equals(slot.platform()));
            slots.add(slot);
        }
    }

    // =====================================================================
    // BotMessageService
    // =====================================================================

    @Override
    public void setup() {
        reloadConfig(); // setup = 首次 reconcile（对齐 OrzEasyBot.setup → reloadConfig）
    }

    @Override
    public void send(MessageEnvelope envelope) {
        if (envelope == null || envelope.targetType() == null) {
            return;
        }
        List<ImConversation> conversations = bindings().conversations();
        List<String> targets = ImMessageRouter.resolveTargets(envelope.targetType(), conversations);
        if (targets.isEmpty()) {
            return;
        }
        MessageEnvelope.Format fmt = envelope.format() == null ? MessageEnvelope.Format.DEFAULT : envelope.format();
        for (String part : formatter.format(envelope.message(), fmt)) {
            for (String target : targets) {
                BuiltinPlatform platform = platformFor(target);
                if (platform == null) {
                    logger.logger().warning("[builtin] 无可用平台处理 target: " + target + "（跳过，R10）");
                    continue;
                }
                platform.send(target, part);
            }
        }
    }

    @Override
    public void tryReconnectIfDisconnected() {
        for (PlatformSlot<?> slot : snapshot()) {
            BuiltinPlatform platform = slot.current();
            if (platform != null) {
                platform.reconnectIfNeeded();
            }
        }
    }

    /** 配置重载：reconcile 所有平台槽（backend 切换由 Provider/外层负责）。 */
    @Override
    public void reloadConfig() {
        for (PlatformSlot<?> slot : snapshot()) {
            slot.reconcile(configService);
        }
    }

    @Override
    public void tearDown() {
        for (PlatformSlot<?> slot : snapshot()) {
            slot.stop();
        }
    }

    // =====================================================================
    // 内部
    // =====================================================================

    private List<PlatformSlot<?>> snapshot() {
        synchronized (slots) {
            return List.copyOf(slots);
        }
    }

    private QqPlatformConfig readQqConfig() {
        if (configService.getConfig("im") == null) {
            return QqPlatformConfig.DISABLED;
        }
        return QqPlatformConfig.from(configService.getConfig("im").getConfigurationSection("platforms.qq"));
    }

    private ImBindings bindings() {
        return ImBindings.from(configService.getConfig("im_bindings"));
    }

    /** 向指定 target（平台前缀会话串）投递文本（/config im test 用）。@return 已投递 true；无可用平台 false */
    public boolean sendTo(String target, String text) {
        BuiltinPlatform platform = platformFor(target);
        if (platform == null) {
            logger.logger().warning("[builtin] 无可用平台发送 target=" + target);
            return false;
        }
        platform.send(target, text);
        return true;
    }

    /** 未绑定会话自动发现候选（D11：status 展示用）。 */
    public ImDiscoveryCandidates candidates() {
        return discovery;
    }

    /** target 前缀平台（如 qq:... → qq）；无对应已启用适配器 → null。 */
    private BuiltinPlatform platformFor(String target) {
        if (target == null) {
            return null;
        }
        String platform = target.indexOf(':') > 0 ? target.substring(0, target.indexOf(':')) : target;
        for (PlatformSlot<?> slot : snapshot()) {
            if (slot.platform().equals(platform)) {
                return slot.current();
            }
        }
        return null;
    }
}
