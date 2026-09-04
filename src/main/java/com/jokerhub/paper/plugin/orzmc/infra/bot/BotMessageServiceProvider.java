package com.jokerhub.paper.plugin.orzmc.infra.bot;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerScheduler;
import com.jokerhub.paper.plugin.orzmc.infra.bot.builtin.BuiltinImDriver;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.FeishuPlatformConfig;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.ImGatewayConfig;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.QqPlatformConfig;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import com.jokerhub.paper.plugin.orzmc.infra.logging.ThrottledLogger;

public final class BotMessageServiceProvider {
    private BotMessageServiceProvider() {}

    public static BotMessageService create(
            ServerLogger logger,
            ServerScheduler scheduler,
            ConfigService configService,
            ThrottledLogger throttledLogger,
            BotInboundHandler inboundHandler,
            HealthRegistry healthRegistry) {
        // 按 im.yml 选择通道（方案 D1/D2/D3）：easybot=现状兜底；builtin=内置直连（平台可配时启用）
        ImGatewayConfig im = ImGatewayConfig.from(configService.getConfig("im"));
        if (im.isBuiltin()) {
            if (anyPlatformUsable(configService)) {
                return new BuiltinImDriver(
                        logger, scheduler, configService, inboundHandler, new PlainMessageFormatter(), healthRegistry);
            }
            logger.logger()
                    .warning("IM backend=builtin 已选择，但无任何可用平台（QQ/飞书需 enabled 且凭据齐备）" + "——已停用群功能（D3，可改回 easybot）。");
            return new UnavailableBotMessageService(logger, healthRegistry);
        }
        return new OrzEasyBot(
                logger, configService, inboundHandler, new PlainMessageFormatter(), throttledLogger, healthRegistry);
    }

    /** backend=builtin 时是否有任一可用平台（builtin 内部逐平台 reconcile；全部不可用才返回 Unavailable）。 */
    private static boolean anyPlatformUsable(ConfigService configService) {
        return qqPlatform(configService).usable()
                || feishuPlatform(configService).usable();
    }

    private static QqPlatformConfig qqPlatform(ConfigService configService) {
        if (configService.getConfig("im") == null) {
            return QqPlatformConfig.DISABLED;
        }
        return QqPlatformConfig.from(configService.getConfig("im").getConfigurationSection("platforms.qq"));
    }

    private static FeishuPlatformConfig feishuPlatform(ConfigService configService) {
        if (configService.getConfig("im") == null) {
            return FeishuPlatformConfig.DISABLED;
        }
        return FeishuPlatformConfig.from(configService.getConfig("im").getConfigurationSection("platforms.feishu"));
    }
}
