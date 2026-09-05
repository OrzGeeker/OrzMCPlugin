package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

class BotConfigTest {

    @Test
    void fromNull_returnsDefaults() {
        BotConfig config = BotConfig.from(null);
        assertEquals("$", config.cmdPromptChar());
        assertNull(config.discordServerLink());
        assertNull(config.qqGroupId());
    }

    @Test
    void fromEmpty_returnsDefaults() {
        ConfigurationSection cfg = mock(ConfigurationSection.class);
        // Mockito 默认返回 null 而非参数默认值
        when(cfg.getString(anyString(), anyString())).thenReturn("$");
        when(cfg.getString(anyString())).thenReturn(null);

        BotConfig config = BotConfig.from(cfg);
        assertEquals("$", config.cmdPromptChar());
        assertNull(config.discordServerLink());
        assertNull(config.qqGroupId());
    }

    @Test
    void fromFullSection_returnsCorrectValues() {
        ConfigurationSection cfg = mock(ConfigurationSection.class);
        when(cfg.getString("cmd_prompt_char", "$")).thenReturn("!");
        when(cfg.getString("discord_server_link")).thenReturn("https://discord.gg/example");
        when(cfg.getString("qq_group_id")).thenReturn("12345");

        BotConfig config = BotConfig.from(cfg);
        assertEquals("!", config.cmdPromptChar());
        assertEquals("https://discord.gg/example", config.discordServerLink());
        assertEquals("12345", config.qqGroupId());
    }

    // =====================================================================
    // v12 双读（config.yml bot: 段优先 + easybot.yml 旧键回退）
    // =====================================================================

    private static ConfigurationSection section(String prompt, String link, String qq) {
        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
        if (prompt != null) y.set("cmd_prompt_char", prompt);
        if (link != null) y.set("discord_server_link", link);
        if (qq != null) y.set("qq_group_id", qq);
        return y;
    }

    @Test
    void botSection_preferredOverEasybotFallback() {
        ConfigurationSection bot = section("!", null, "123");
        ConfigurationSection easybot = section("+", "https://discord.gg/old", "456");
        BotConfig c = BotConfig.from(bot, easybot);
        assertEquals("!", c.cmdPromptChar());
        assertEquals("https://discord.gg/old", c.discordServerLink(), "bot 段缺键 → 回退 easybot");
        assertEquals("123", c.qqGroupId());
    }

    @Test
    void botBlankValue_isExplicitClear_doesNotFallBack() {
        ConfigurationSection bot = section("!", "", "");
        ConfigurationSection easybot = section("+", "https://discord.gg/old", "456");
        BotConfig c = BotConfig.from(bot, easybot);
        assertEquals("", c.discordServerLink(), "bot 段显式空串清空，不回退 easybot");
        assertEquals("", c.qqGroupId());
    }

    @Test
    void botMissing_bothMissing_fallsBackToDefaults() {
        assertEquals("$", BotConfig.from(null, null).cmdPromptChar());
        ConfigurationSection bot = new org.bukkit.configuration.file.YamlConfiguration(); // 空段
        BotConfig c = BotConfig.from(bot, null);
        assertEquals("$", c.cmdPromptChar());
        assertNull(c.discordServerLink());
        assertNull(c.qqGroupId());
    }

    @Test
    void easybotCustomValue_usedWhenBotKeyMissing() {
        ConfigurationSection bot = section("!", null, null);
        ConfigurationSection easybot = section(null, "https://discord.gg/legacy", null);
        BotConfig c = BotConfig.from(bot, easybot);
        assertEquals("!", c.cmdPromptChar());
        assertEquals("https://discord.gg/legacy", c.discordServerLink(), "老装自定义经 easybot 回退保留");
    }
}
