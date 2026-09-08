package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public record MaintenanceConfig(
        boolean optimizeEnabled, long optimizeTickTimeThreshold, int backupRetentionCount, long backupIntervalHours) {

    /**
     * 维护场景文案/进度行已迁语言包 maintenance.motd.*（MaintenanceTexts 承载，P4c-2），
     * 不再从此处读取 motd（2026-09-02 PR4）。本配置仅保留维护开关/阈值/保留数/自动备份间隔。
     */
    public static MaintenanceConfig from(ConfigurationSection cfg) {
        if (cfg == null) {
            return new MaintenanceConfig(false, 300L, 5, 0L);
        }
        boolean optimizeEnabled = cfg.getBoolean("optimize_enabled", false);
        long optimizeTickTimeThreshold = cfg.getLong("optimize_tick_time_threshold", 300L);
        int backupRetentionCount = cfg.getInt("backup_retention_count", 5);
        long backupIntervalHours = cfg.getLong("backup_interval_hours", 0L);
        return new MaintenanceConfig(
                optimizeEnabled, optimizeTickTimeThreshold, backupRetentionCount, backupIntervalHours);
    }

    /** 启动健康校验：{@code optimize_enabled} 类型、阈值/保留数非负；段缺失为硬缺失。 */
    public static void validate(ConfigurationSection section, List<String> issues) {
        if (section == null) {
            issues.add("config.yml 缺失 maintenance 配置段");
            return;
        }
        Object en = section.get("optimize_enabled");
        if (!(en instanceof Boolean)) issues.add("类型错误: maintenance.optimize_enabled 需为布尔值");
        int thr = section.getInt("optimize_tick_time_threshold", 300);
        if (thr <= 0) issues.add("非法: maintenance.optimize_tick_time_threshold 必须为正数");
        int retain = section.getInt("backup_retention_count", 5);
        if (retain < 0) issues.add("非法: maintenance.backup_retention_count 不得为负数");
        // 维护场景文案/进度行已迁语言包（maintenance.motd.* / MaintenanceTexts），config.yml maintenance 段
        // 不再含 motd 键，故此处不再校验（2026-09-02 PR4；P4c-2 语言包承载）
    }
}
