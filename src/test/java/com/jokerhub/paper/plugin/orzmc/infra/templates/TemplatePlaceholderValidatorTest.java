package com.jokerhub.paper.plugin.orzmc.infra.templates;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;

class TemplatePlaceholderValidatorTest {

    private FileConfiguration mockTemplates(String key, String body) {
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(anyString(), anyString())).thenReturn("");
        when(cfg.getString(eq("templates." + key), anyString())).thenReturn(body);
        when(cfg.get(anyString())).thenReturn(null);
        when(cfg.contains(anyString())).thenReturn(false);
        return cfg;
    }

    @Test
    void nullConfig_returnsError() {
        List<String> issues = TemplatePlaceholderValidator.validate(null);
        assertFalse(issues.isEmpty());
        assertTrue(issues.contains("templates.yml 未加载"));
    }

    @Test
    void validConfig_passes() {
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(cfg.getString(anyString(), anyString())).thenReturn("");
        when(cfg.get(anyString())).thenReturn(null);
        when(cfg.contains(anyString())).thenReturn(false);

        List<String> issues = TemplatePlaceholderValidator.validate(cfg);
        assertTrue(issues.isEmpty());
    }

    @Test
    void unknownPlaceholderOnShellTemplate_reportsError() {
        // command_*（{message} 直通壳等）仍校验磁盘 body 变量集
        List<String> issues =
                TemplatePlaceholderValidator.validate(mockTemplates("command_output", "{message} {bogus_var}"));
        assertTrue(issues.stream().anyMatch(i -> i.contains("模板变量未知") && i.contains("command_output")));
    }

    @Test
    void knownPlaceholdersOnShellTemplate_pass() {
        List<String> issues = TemplatePlaceholderValidator.validate(
                mockTemplates("command_whitelist_page", "{header}\n{page}/{total}\n{body}"));
        assertTrue(issues.isEmpty());
    }

    @Test
    void langBackedDiskBody_isNotValidated() {
        // P5-2：语言包承载正文（event.*）的键跳过磁盘 body 变量校验——磁盘残留 {message} 直通壳/旧正文/
        // 服主定制均回落或原样渲染，不经过该白名单（防存量升级后的误告警，如 server_load/server_stop 壳）
        List<String> issues = TemplatePlaceholderValidator.validate(mockTemplates("player_join", "{bogus_var}"));
        assertTrue(issues.isEmpty(), "lang-backed 键不应校验磁盘 body: " + issues);
    }
}
