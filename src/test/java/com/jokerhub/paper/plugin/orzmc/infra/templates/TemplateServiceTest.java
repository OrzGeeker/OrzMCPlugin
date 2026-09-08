package com.jokerhub.paper.plugin.orzmc.infra.templates;

import static org.junit.jupiter.api.Assertions.*;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class TemplateServiceTest {

    @Test
    void playerJoin_returnsMessageEnvelope() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("player_join", cfg, Map.of("name", "Test"));
        assertNotNull(result);
        assertEquals(MessageEnvelope.TargetType.PUBLIC, result.targetType());
    }

    @Test
    void playerQuit_returnsMessageEnvelope() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("player_quit", cfg, Map.of("name", "Test"));
        assertNotNull(result);
    }

    @Test
    void playerKick_returnsMessageEnvelope() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("player_kick", cfg, Map.of("name", "Test"));
        assertNotNull(result);
    }

    @Test
    void playerDigest_returnsMessageEnvelope() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent(
                "player_digest", cfg, Map.of("join_summary", "🥰 上线(2)：\nA 生存模式\nB 生存模式\n"));
        assertNotNull(result);
        assertTrue(result.message().contains("🥰 上线(2)："), "摘要模板应渲染 join_summary: " + result.message());
    }

    @Test
    void exceptionAlert_returnsMessageEnvelope() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("exception_alert", cfg, Map.of("message", "err"));
        assertNotNull(result);
    }

    @Test
    void tntAlert_returnsMessageEnvelope() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("tnt_alert", cfg, Map.of("msg", "boom"));
        assertNotNull(result);
    }

    @Test
    void unknownEventKey_returnsEmpty() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("unknown_key", cfg, Map.of());
        assertNotNull(result);
        assertTrue(result.message().isEmpty());
    }

    @Test
    void nullEventKey_returnsEmpty() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent(null, cfg, Map.of());
        assertNotNull(result);
        assertTrue(result.message().isEmpty());
    }

    @Test
    void emptyEventKey_returnsEmpty() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("", cfg, Map.of());
        assertNotNull(result);
        assertTrue(result.message().isEmpty());
    }

    @Test
    void serverLoad_defaultsToMessageShell_whenNoDiskBody() {
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent("server_load", cfg, Map.of("message", "Minecraft 1.21"));
        assertNotNull(result);
        assertEquals("Minecraft 1.21", result.message(), "无磁盘正文应走 {message} 直通壳");
    }

    @Test
    void serverStop_diskCustomBodyWins() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.server_stop", "服务器 {message} 再见");
        MessageEnvelope result = TemplateService.renderEvent("server_stop", cfg, Map.of("message", "维护"));
        assertNotNull(result);
        assertEquals("服务器 维护 再见", result.message(), "磁盘自定义正文优先");
    }
}
