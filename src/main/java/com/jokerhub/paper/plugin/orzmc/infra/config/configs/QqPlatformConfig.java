package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import org.bukkit.configuration.ConfigurationSection;

/**
 * builtin QQ 平台配置（im.yml {@code platforms.qq}，方案 §4.1）。
 *
 * <p>backend=builtin 时以此判断 QQ 平台是否可启用：{@code enabled} 且 app_id / client_secret 齐备
 * （凭据为空 = 不可用，D3 语义——无任何可用平台时整体停群）。凭据安全（R5）：本类不打印任何值。</p>
 *
 * @param enabled 是否启用 QQ 平台
 * @param appId QQ 开放平台 app_id
 * @param clientSecret QQ 开放平台 client_secret
 */
public record QqPlatformConfig(boolean enabled, String appId, String clientSecret) {

    /** 默认（禁用，无凭据）。 */
    public static final QqPlatformConfig DISABLED = new QqPlatformConfig(false, "", "");

    /** 是否「已启用且凭据齐备」（可作为 builtin 可用平台）。 */
    public boolean usable() {
        return enabled && !isBlank(appId) && !isBlank(clientSecret);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 从 {@code platforms.qq} 段创建；null/缺失 → 禁用。 */
    public static QqPlatformConfig from(ConfigurationSection section) {
        if (section == null) {
            return DISABLED;
        }
        String appId = section.getString("app_id", "");
        String secret = section.getString("client_secret", "");
        return new QqPlatformConfig(
                section.getBoolean("enabled", false), appId == null ? "" : appId, secret == null ? "" : secret);
    }
}
