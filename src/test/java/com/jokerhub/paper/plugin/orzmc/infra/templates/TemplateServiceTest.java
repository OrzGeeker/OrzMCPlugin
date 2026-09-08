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
    void serverLoad_langPackBody_whenNoDiskBody() {
        // P5-2：server_load 正文迁语言包 event.server_load（zh 主目录正文逐字回归）
        YamlConfiguration cfg = new YamlConfiguration();
        MessageEnvelope result = TemplateService.renderEvent(
                "server_load", cfg, Map.of("version", "1.21.4", "mode", "正版服", "status", "启动完成", "prompt_help", "$h"));
        assertNotNull(result);
        assertEquals(
                "Minecraft 1.21.4 正版服\n---------------------------------\n启动完成\n\n发送 \"$h\" 查看支持的命令消息",
                result.message(),
                "无磁盘正文应回落语言包 event.server_load");
    }

    @Test
    void serverStop_diskCustomBodyWins() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.server_stop", "服务器 {version} 再见");
        MessageEnvelope result = TemplateService.renderEvent("server_stop", cfg, Map.of("version", "维护"));
        assertNotNull(result);
        assertEquals("服务器 维护 再见", result.message(), "磁盘自定义正文优先");
    }

    @Test
    void serverStop_diskMessageShell_fallsBackToLangPack() {
        // P5-2：磁盘残留 {message} 直通壳视为无正文，回落语言包（历史升级/新装缺省场景）
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.server_stop", "{message}");
        MessageEnvelope result = TemplateService.renderEvent("server_stop", cfg, Map.of("version", "1.21.4"));
        assertNotNull(result);
        assertTrue(result.message().contains("服务停止"), "壳正文应回落语言包: " + result.message());
    }
}
