package com.jokerhub.paper.plugin.orzmc.infra.bot;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.ports.server.ServerLogger;
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
    private BotInboundHandler inboundHandler;
    private ThrottledLogger throttledLogger;
    private HealthRegistry healthRegistry;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        serverLogger = mock(ServerLogger.class);
        when(serverLogger.logger()).thenReturn(Logger.getLogger("BotMessageServiceProviderTest"));
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

    @Test
    void missingImConfig_selectsEasybotDriver() {
        backend(null);

        BotMessageService svc = BotMessageServiceProvider.create(
                serverLogger, configService, throttledLogger, inboundHandler, healthRegistry);

        assertTrue(svc instanceof OrzEasyBot);
    }

    @Test
    void easybotBackend_selectsEasybotDriver() {
        backend("easybot");

        BotMessageService svc = BotMessageServiceProvider.create(
                serverLogger, configService, throttledLogger, inboundHandler, healthRegistry);

        assertTrue(svc instanceof OrzEasyBot);
    }

    @Test
    void builtinBackend_selectsUnavailableDriverWhenNotImplemented() {
        backend("builtin");

        BotMessageService svc = BotMessageServiceProvider.create(
                serverLogger, configService, throttledLogger, inboundHandler, healthRegistry);

        assertTrue(svc instanceof UnavailableBotMessageService);
    }

    @Test
    void invalidBackend_fallsBackToEasybotDriver() {
        backend("hybrid");

        BotMessageService svc = BotMessageServiceProvider.create(
                serverLogger, configService, throttledLogger, inboundHandler, healthRegistry);

        assertTrue(svc instanceof OrzEasyBot);
    }
}
