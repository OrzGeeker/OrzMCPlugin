package com.jokerhub.paper.plugin.orzmc.features.maintenance;

import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceProgress;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceReason;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import org.bukkit.entity.Player;

/**
 * {@code /maintenance on|off|status} 命令的服务逻辑（与 Brigadier 注册解耦，便于测试）。
 *
 * <p>与备份/优化的互斥约定：备份/优化进行中拒绝手动进入（避免双重踢人/保存中断）；
 * 手动维护期间备份照常执行，reason 被备份/优化覆盖，任务结束后 {@link WorldMaintenanceService}
 * 恢复手动维护。手动维护踢人投递到玩家所在 region 线程（Folia 实体线程约束）。</p>
 */
public final class MaintenanceCommandService {

    /** 手动维护踢人文案（登录拦截/MOTD 文案可经 config 配置，踢人即时反馈保持简单固定）。 */
    static final String MANUAL_KICK_TEXT = "服务器维护中，请稍后再尝试登录。";

    private final ServerFacade server;
    private final OrzTextStyles styles;
    private final MaintenanceModeService mode;
    private final WorldMaintenanceService worldMaintenance;

    public MaintenanceCommandService(
            ServerFacade server,
            OrzTextStyles styles,
            MaintenanceModeService mode,
            WorldMaintenanceService worldMaintenance) {
        this.server = server;
        this.styles = styles;
        this.mode = mode;
        this.worldMaintenance = worldMaintenance;
    }

    /** 进入手动维护。返回 null 表示成功，否则为拒绝提示。 */
    public String enterManual() {
        if (worldMaintenance.isRunning()) {
            return "当前正在执行地图备份/优化，无法手动进入维护模式";
        }
        if (mode.isActive() && mode.reason() == MaintenanceReason.MANUAL) {
            return "服务器已处于手动维护模式";
        }
        mode.enter(MaintenanceReason.MANUAL);
        server.runSync(() -> {
            for (Player p : server.server().getOnlinePlayers()) {
                p.getScheduler().run(server.plugin(), t -> p.kick(styles.warn(MANUAL_KICK_TEXT)), () -> {});
            }
        });
        return null;
    }

    /** 退出手动维护。返回 null 表示成功，否则为拒绝提示。 */
    public String exitManual() {
        if (!mode.isActive()) {
            return "服务器当前未处于维护模式";
        }
        if (worldMaintenance.isRunning()) {
            return "当前正在执行地图备份/优化，无法退出维护模式";
        }
        if (mode.reason() != MaintenanceReason.MANUAL) {
            return "当前维护由地图备份/优化触发，任务结束后自动恢复";
        }
        mode.exit();
        return null;
    }

    /** 当前维护状态描述（用于 /maintenance status）。 */
    public String status() {
        if (!mode.isActive()) {
            return "服务器未处于维护模式";
        }
        String reasonCn =
                switch (mode.reason()) {
                    case BACKUP -> "地图备份中";
                    case OPTIMIZE -> "地图优化中";
                    case MANUAL -> "手动维护中";
                };
        MaintenanceProgress progress = mode.progress();
        if (progress != null) {
            return reasonCn + " " + progress.progressMessage();
        }
        return reasonCn;
    }
}
