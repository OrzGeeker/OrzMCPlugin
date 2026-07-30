package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class EasyBotConfigTest {

    @Test
    void platformWithoutEnabledFlagDefaultsToDisabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("platforms.qq.admin_group", "qq:123");

        EasyBotConfig easyBot = EasyBotConfig.from(config);

        assertFalse(easyBot.platforms().get("qq").enabled());
    }

    @Test
    void defaultGatewayPortsMatchDistributedConfig() {
        EasyBotConfig easyBot = EasyBotConfig.from(new YamlConfiguration());

        assertEquals("http://127.0.0.1:8080", easyBot.apiServer());
        assertEquals("ws://127.0.0.1:8080", easyBot.wsServer());
    }
}
