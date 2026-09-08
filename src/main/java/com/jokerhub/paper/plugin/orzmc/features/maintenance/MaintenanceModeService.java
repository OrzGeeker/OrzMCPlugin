package com.jokerhub.paper.plugin.orzmc.features.maintenance;

import com.jokerhub.paper.plugin.orzmc.infra.config.configs.MaintenanceTexts;
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

    /** 进度快照（不可变 record）：阶段显示名 + 百分比 + 预计剩余秒数。 */
    public record MaintenanceProgress(String stage, int percent, long etaSeconds) {
        static MaintenanceProgress of(String stage, int percent, long etaSeconds) {
            long eta = Math.max(0, etaSeconds);
            return new MaintenanceProgress(stage, percent, eta);
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

    /** 更新进度快照（stage 为阶段显示名（默认语言），percent 0-100，etaSeconds 预计剩余秒数）。 */
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
     * 无进度快照（progress==null）时把三个进度占位符全部替换为空串——消除 manual/无进度场景
     * 自定义模板显示字面量 "{percent}" 的问题（有进度时行为不变）。
     */
    public static String renderTemplate(String template, MaintenanceProgress progress) {
        if (template == null) {
            return "";
        }
        if (progress == null) {
            return template.replace("{stage}", "").replace("{percent}", "").replace("{eta}", "");
        }
        String rendered = template;
        if (progress.stage() != null) {
            rendered = rendered.replace("{stage}", progress.stage());
        }
        rendered = rendered.replace("{percent}", String.valueOf(progress.percent()));
        rendered = rendered.replace("{eta}", String.valueOf(progress.etaSeconds()));
        return rendered;
    }

    /**
     * 按维护场景渲染统一提示文案（MOTD / 登录拦截 / 踢人三处共用；i18n P4c-2 起）。
     *
     * <p>场景文案与进度行来自 {@link MaintenanceTexts}（磁盘 {@code maintenance_motd_*} 正文优先 →
     * 语言包 {@code maintenance.motd.*} 回落，默认语言 R1；服主自定义磁盘模板继续生效）。
     * 场景模板默认纯文案（不含进度占位符）；当场景非 MANUAL、有进度、且场景模板未声明任何
     * 进度占位符时，追加换行 + 渲染后的进度行。若服主自定义场景模板自带
     * {@code {stage}/{percent}/{eta}}，则不追加（防两行进度重复）。</p>
     *
     * @param reason   维护场景；null 视为未知（返回手动场景文案，不渲染进度行）
     * @param texts    维护运维文案（非 null）
     * @param progress 进度快照；可能为 null（manual/刚进入无进度）
     */
    public static String renderMotdText(
            MaintenanceReason reason, MaintenanceTexts texts, MaintenanceProgress progress) {
        if (texts == null) {
            return "";
        }
        if (reason == null) {
            return texts.motdManual();
        }
        String scene =
                switch (reason) {
                    case BACKUP -> texts.motdBackup();
                    case OPTIMIZE -> texts.motdOptimize();
                    case MANUAL -> texts.motdManual();
                };
        String rendered = renderTemplate(scene, progress);
        if (progress != null && reason != MaintenanceReason.MANUAL && !hasProgressPlaceholders(scene)) {
            rendered = rendered + "\n" + renderTemplate(texts.motdProgressLine(), progress);
        }
        return rendered;
    }

    /** 场景模板是否声明了进度占位符：含任一则占位符已渲染进场景文案，无需再追加独立进度行。 */
    private static boolean hasProgressPlaceholders(String template) {
        return template != null
                && (template.contains("{stage}") || template.contains("{percent}") || template.contains("{eta}"));
    }
}
