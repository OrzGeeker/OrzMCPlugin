package com.jokerhub.paper.plugin.orzmc.features.maintenance;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.MaintenanceConfig;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;

/**
 * 定时自动备份（安全加固 P1-1）。
 *
 * <p>按 {@code maintenance.backup_interval_hours}（默认 0 = 关闭）周期触发
 * {@link WorldMaintenanceService#backup(long, int, java.util.function.Consumer)}，
 * 把文章 26 §5「备份不要依赖手动」落地为自动调度。</p>
 *
 * <p>并发安全：重复 tick 不会叠加 —— {@link WorldMaintenanceService#runExclusive} 内部
 * 用 {@code AtomicBoolean} 互斥，前一次备份尚未结束时再次触发直接跳过；错误通知沿用
 * 现有 PRIVATE 私信路由（由 {@code Notifier} 的 MAINTENANCE_BACKUP_ERROR 处理）。</p>
 */
public final class ScheduledBackupService {

    /** 1 小时的 tick 数（20 tick/s）。 */
    private static final long HOUR_TICKS = 20L * 60L * 60L;

    private final ServerFacade server;
    private final TypedConfigProvider configs;
    private final WorldMaintenanceService maintenance;
    private org.bukkit.scheduler.BukkitTask task;

    public ScheduledBackupService(
            ServerFacade server, TypedConfigProvider configs, WorldMaintenanceService maintenance) {
        this.server = server;
        this.configs = configs;
        this.maintenance = maintenance;
    }

    /** 按配置调度定时备份；间隔 ≤ 0 表示关闭，不调度任何任务。 */
    public void setup() {
        long intervalHours = configs.maintenance().backupIntervalHours();
        if (intervalHours <= 0) {
            return;
        }
        long periodTicks = Math.multiplyExact(intervalHours, HOUR_TICKS);
        this.task = server.runTaskTimer(this::tick, periodTicks, periodTicks);
    }

    /** 取消定时任务（插件卸载时）。 */
    public void tearDown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** 触发一次备份；进行中的备份互斥跳过，进度仅落服务器日志（不打扰群聊）。 */
    void tick() {
        MaintenanceConfig cfg = configs.maintenance();
        maintenance.backup(
                cfg.optimizeTickTimeThreshold(),
                cfg.backupRetentionCount(),
                msg -> server.logger().info("[定时备份] " + msg));
    }
}
