package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import org.bukkit.configuration.ConfigurationSection;

public record BotConfig(String cmdPromptChar, String discordServerLink, String qqGroupId) {

    /**
     * 从单个配置段读取（旧用法：easybot.yml 顶层；保留给测试与旧装配路径）。
     * 生产请用 {@link #from(ConfigurationSection, ConfigurationSection)}（config.yml {@code bot:} 段优先）。
     */
    public static BotConfig from(ConfigurationSection cfg) {
        if (cfg == null) {
            return new BotConfig("$", null, null);
        }
        String cmdPromptChar = cfg.getString("cmd_prompt_char", "$");
        String discordServerLink = cfg.getString("discord_server_link");
        String qqGroupId = cfg.getString("qq_group_id");
        return new BotConfig(cmdPromptChar, discordServerLink, qqGroupId);
    }

    /**
     * 业务层 bot 参数（v12 起权威在 config.yml {@code bot:} 段，IM 通道无关）：
     * 优先读 {@code bot} 段键；键缺失回退 {@code easybotFallback}（easybot.yml）旧键（老装兼容，v12 前的
     * 位置）；两处均无 → 默认（前缀 {@code $}、联系方式 null）。空串视为显式清空，不回退。
     */
    public static BotConfig from(ConfigurationSection bot, ConfigurationSection easybotFallback) {
        return new BotConfig(
                pick(bot, "cmd_prompt_char", easybotFallback, "$"),
                pick(bot, "discord_server_link", easybotFallback, null),
                pick(bot, "qq_group_id", easybotFallback, null));
    }

    /** 键存在即取（含空串，显式清空）；缺 → fallback 段同键；仍缺 → 默认。 */
    private static String pick(ConfigurationSection primary, String key, ConfigurationSection fallback, String dflt) {
        if (primary != null && primary.contains(key)) {
            return primary.getString(key);
        }
        if (fallback != null && fallback.contains(key)) {
            return fallback.getString(key);
        }
        return dflt;
    }
}
