package com.jokerhub.paper.plugin.orzmc.features.maintenance;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 维护模式独立状态机。
 *
 * <p>与 {@link WorldMaintenanceService} 的生命周期解耦：备份/优化（经
 * {@code WorldMaintenanceService}）驱动它进入 {@code BACKUP}/{@code OPTIMIZE}，
 * 服主可经 {@code /maintenance on} 手动进入 {@code MANUAL}；MOTD / 登录拦截 /
 * 群消息统一读取这里的状态渲染文案。</p>
 *
 * <p>线程安全（Folia 多线程）：{@code active} 用原子布尔，其余字段 volatile；
 * 进度快照 record 不可变，任何线程读到的是完整一致的快照。</p>
 */
public final class MaintenanceModeService {

    /** 维护场景：地图备份 / 地图优化 / 服主手动进入。 */
    public enum MaintenanceReason {
        BACKUP,
        OPTIMIZE,
        MANUAL
    }

    /** 进度快照（不可变 record）：阶段中文名 + 百分比 + 预计剩余秒数 + 完整进度行文案。 */
    public record MaintenanceProgress(String stage, int percent, long etaSeconds, String progressMessage) {
        static MaintenanceProgress of(String stage, int percent, long etaSeconds) {
            long eta = Math.max(0, etaSeconds);
            String message = "进度：" + stage + " " + percent + "% 预计剩余 " + eta + "秒";
            return new MaintenanceProgress(stage, percent, eta, message);
        }
    }

    private final AtomicBoolean active = new AtomicBoolean(false);
    private volatile MaintenanceReason reason;
    private volatile long startedAtMillis = 0L;
    private volatile MaintenanceProgress progress;

    /** 进入维护模式（记录开始时间，清空上一次进度）。 */
    public void enter(MaintenanceReason reason) {
        this.reason = reason;
        this.startedAtMillis = System.currentTimeMillis();
        this.progress = null;
        active.set(true);
    }

    /** 更新进度快照（stage 为中文阶段名，percent 0-100，etaSeconds 预计剩余秒数）。 */
    public void updateProgress(String stage, int percent, long etaSeconds) {
        progress = MaintenanceProgress.of(stage, percent, etaSeconds);
    }

    /** 退出维护模式（清空 reason 与进度）。 */
    public void exit() {
        active.set(false);
        reason = null;
        progress = null;
    }

    public boolean isActive() {
        return active.get();
    }

    /** 当前维护原因；未激活时为 null（调用方应先判 isActive）。 */
    public MaintenanceReason reason() {
        return reason;
    }

    public long startedAt() {
        return startedAtMillis;
    }

    public MaintenanceProgress progress() {
        return progress;
    }

    /** 原子状态快照：active/reason/progress 同锁一次读取，避免多次读拼接不一致。 */
    public synchronized MaintenanceStatus status() {
        return new MaintenanceStatus(active.get(), reason, progress);
    }

    public record MaintenanceStatus(boolean active, MaintenanceReason reason, MaintenanceProgress progress) {}

    /**
     * 将文案模板中的 {@code {stage}}/{@code {percent}}/{@code {eta}} 占位符替换为进度值；
     * 无进度快照时原样返回（占位符保留，交给调用方按场景兜底）。
     */
    public static String renderTemplate(String template, MaintenanceProgress progress) {
        if (template == null) {
            return "";
        }
        if (progress == null) {
            return template;
        }
        String rendered = template;
        if (progress.stage() != null) {
            rendered = rendered.replace("{stage}", progress.stage());
        }
        rendered = rendered.replace("{percent}", String.valueOf(progress.percent()));
        rendered = rendered.replace("{eta}", String.valueOf(progress.etaSeconds()));
        return rendered;
    }
}
