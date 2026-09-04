package com.jokerhub.paper.plugin.orzmc.infra.bot;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerScheduler;
import com.jokerhub.paper.plugin.orzmc.infra.bot.builtin.BuiltinImDriver;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import com.jokerhub.paper.plugin.orzmc.infra.health.HealthRegistry;
import com.jokerhub.paper.plugin.orzmc.infra.logging.ThrottledLogger;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BotMessageServiceProviderTest {

    private ConfigService configService;
    private ServerLogger serverLogger;
    private ServerScheduler scheduler;
    private BotInboundHandler inboundHandler;
    private ThrottledLogger throttledLogger;
    private HealthRegistry healthRegistry;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        serverLogger = mock(ServerLogger.class);
        when(serverLogger.logger()).thenReturn(Logger.getLogger("BotMessageServiceProviderTest"));
        scheduler = mock(ServerScheduler.class);
        inboundHandler = mock(BotInboundHandler.class);
        throttledLogger = mock(ThrottledLogger.class);
        healthRegistry = new HealthRegistry();
    }

    private void backend(String value) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (value != null) {
            yaml.set("backend", value);
        }
        when(configService.getConfig("im")).thenReturn(yaml);
    }

    private BotMessageService create() {
        return BotMessageServiceProvider.create(
                serverLogger, scheduler, configService, throttledLogger, inboundHandler, healthRegistry);
    }

    @Test
    void missingImConfig_selectsEasybotDriver() {
        backend(null);

        assertTrue(create() instanceof OrzEasyBot);
    }

    @Test
    void easybotBackend_selectsEasybotDriver() {
        backend("easybot");

        assertTrue(create() instanceof OrzEasyBot);
    }

    @Test
    void builtinBackend_withoutUsablePlatform_selectsUnavailableDriver() {
        // backend=builtin 但 im.yml 无 platforms.qq 或凭据缺失 → 无可用平台，停群（D3）
        backend("builtin");

        assertTrue(create() instanceof UnavailableBotMessageService);
    }

    @Test
    void builtinBackend_withUsableQq_selectsBuiltinDriver() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("backend", "builtin");
        yaml.set("platforms.qq.enabled", true);
        yaml.set("platforms.qq.app_id", "app-1");
        yaml.set("platforms.qq.client_secret", "secret-1");
        when(configService.getConfig("im")).thenReturn(yaml);

        assertTrue(create() instanceof BuiltinImDriver);
    }

    @Test
    void invalidBackend_fallsBackToEasybotDriver() {
        backend("hybrid");

        assertTrue(create() instanceof OrzEasyBot);
    }
}
