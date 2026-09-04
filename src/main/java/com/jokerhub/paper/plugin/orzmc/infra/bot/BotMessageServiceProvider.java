package com.jokerhub.paper.plugin.orzmc.infra.bot;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.ImGatewayConfig;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import com.jokerhub.paper.plugin.orzmc.infra.logging.ThrottledLogger;

public final class BotMessageServiceProvider {
    private BotMessageServiceProvider() {}

    public static BotMessageService create(
            ServerLogger logger,
            ConfigService configService,
            ThrottledLogger throttledLogger,
            BotInboundHandler inboundHandler,
            HealthRegistry healthRegistry) {
        // 按 im.yml 选择通道（方案 D1/D2/D3）：easybot=现状兜底；builtin=内置直连（未实现时停群+告警）
        ImGatewayConfig im = ImGatewayConfig.from(configService.getConfig("im"));
        if (im.isBuiltin()) {
            logger.logger().warning("IM backend=builtin 已选择，但内置直连通道尚未实现——将停用群功能（D3，可改回 easybot）。");
            return new UnavailableBotMessageService(logger, healthRegistry);
        }
        return new OrzEasyBot(
                logger, configService, inboundHandler, new PlainMessageFormatter(), throttledLogger, healthRegistry);
    }
}
