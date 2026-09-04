package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class QqPlatformConfigTest {

    @Test
    void missingSection_isDisabled() {
        assertFalse(QqPlatformConfig.from(null).usable());
    }

    @Test
    void disabledByDefault_whenKeysMissing() {
        assertFalse(QqPlatformConfig.from(new YamlConfiguration()).usable());
    }

    @Test
    void enabledButBlankCredentials_notUsable() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("enabled", true);

        QqPlatformConfig cfg = QqPlatformConfig.from(yaml);
        assertFalse(cfg.usable(), "凭据缺失不可用");
    }

    @Test
    void enabledWithCredentials_usable() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("enabled", true);
        yaml.set("app_id", "app-1");
        yaml.set("client_secret", "secret-1");

        assertTrue(QqPlatformConfig.from(yaml).usable());
    }
}
