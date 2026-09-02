package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import org.bukkit.configuration.ConfigurationSection;

public record MaintenanceConfig(
        boolean optimizeEnabled,
        long optimizeTickTimeThreshold,
        int backupRetentionCount,
        String backupMaintenanceMotd,
        String optimizeMaintenanceMotd,
        String manualMaintenanceMotd,
        long backupIntervalHours) {

    public static final String DEFAULT_BACKUP_MOTD = "服务器维护中，稍后再试";
    public static final String DEFAULT_OPTIMIZE_MOTD = "服务器地图优化中，请稍后再试";
    public static final String DEFAULT_MANUAL_MOTD = "服务器维护中，请稍后再试";

    public static MaintenanceConfig from(ConfigurationSection cfg) {
        if (cfg == null) {
            return new MaintenanceConfig(
                    false, 300L, 5, DEFAULT_BACKUP_MOTD, DEFAULT_OPTIMIZE_MOTD, DEFAULT_MANUAL_MOTD, 0L);
        }
        boolean optimizeEnabled = cfg.getBoolean("optimize_enabled", false);
        long optimizeTickTimeThreshold = cfg.getLong("optimize_tick_time_threshold", 300L);
        int backupRetentionCount = cfg.getInt("backup_retention_count", 5);
        String backupMaintenanceMotd = cfg.getString("backup_maintenance_motd", DEFAULT_BACKUP_MOTD);
        String optimizeMaintenanceMotd = cfg.getString("optimize_maintenance_motd", DEFAULT_OPTIMIZE_MOTD);
        String manualMaintenanceMotd = cfg.getString("manual_maintenance_motd", DEFAULT_MANUAL_MOTD);
        long backupIntervalHours = cfg.getLong("backup_interval_hours", 0L);
        return new MaintenanceConfig(
                optimizeEnabled,
                optimizeTickTimeThreshold,
                backupRetentionCount,
                backupMaintenanceMotd,
                optimizeMaintenanceMotd,
                manualMaintenanceMotd,
                backupIntervalHours);
    }
}
