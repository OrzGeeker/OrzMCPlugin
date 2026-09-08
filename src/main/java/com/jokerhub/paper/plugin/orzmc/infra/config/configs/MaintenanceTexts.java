package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nServiceHolder;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 维护域运维文案（MOTD / 登录拦截 / 踢人 / 进度行与阶段显示名；默认语言 R1）。
 *
 * <p>i18n P4c-2：正文从 templates.yml 迁入语言包 {@code maintenance.motd.*} 与
 * {@code maintenance.stage.*}（预登录/全服共享场景无玩家 locale，按 default_lang 渲染一次）。
 * 与事件正文（{@code TemplateService.renderEvent}）同一「磁盘优先 → 语言包回落」机制：磁盘
 * templates.yml 的 {@code maintenance_motd_*} / {@code stage_cn} 段若存在（存量服/服主定制）优先
 * 渲染——P4d 升级链统一迁移存量盘前保持 zh/不丢定制；磁盘缺失（全新安装）回落语言包。</p>
 */
public record MaintenanceTexts(
        String motdBackup,
        String motdOptimize,
        String motdManual,
        String motdProgressLine,
        Map<String, String> stageDisplay) {

    /** 阶段显示名常量（与语言包 maintenance.stage.* 键对应；Running 为未知/空兜底）。 */
    private static final String[] STAGE_CANONICAL = {"Region", "Chunk", "File", "Done", "Running"};

    public static MaintenanceTexts from(FileConfiguration templatesCfg) {
        return new MaintenanceTexts(
                motdText("maintenance_motd_backup", "maintenance.motd.backup", templatesCfg),
                motdText("maintenance_motd_optimize", "maintenance.motd.optimize", templatesCfg),
                motdText("maintenance_motd_manual", "maintenance.motd.manual", templatesCfg),
                motdText("maintenance_motd_progress_line", "maintenance.motd.progress_line", templatesCfg),
                stageMap(templatesCfg));
    }

    /** 磁盘模板正文优先（存量/定制），缺失回落语言包 key（default_lang）。 */
    private static String motdText(String diskKey, String langKey, FileConfiguration templatesCfg) {
        if (templatesCfg != null) {
            String disk = templatesCfg.getString("templates." + diskKey);
            if (disk != null && !disk.isEmpty()) {
                return disk;
            }
        }
        return I18nServiceHolder.msg(langKey);
    }

    /** 阶段显示名：磁盘 {@code stage_cn} 自定义优先，缺失回落语言包 {@code maintenance.stage.*}。 */
    private static Map<String, String> stageMap(FileConfiguration templatesCfg) {
        Map<String, String> map = new HashMap<>();
        if (templatesCfg != null) {
            Object raw = templatesCfg.get("templates.stage_cn");
            if (raw instanceof ConfigurationSection sec) {
                for (String k : sec.getKeys(false)) {
                    String v = sec.getString(k);
                    if (v != null && !v.isEmpty()) map.put(k, v);
                }
            }
        }
        for (String name : STAGE_CANONICAL) {
            map.putIfAbsent(name, I18nServiceHolder.msg("maintenance.stage." + name));
        }
        return Map.copyOf(map);
    }

    /** 阶段显示名：磁盘映射（含自定义）优先；未知/空阶段名 → Running（进行中）。 */
    public String stage(String stageName) {
        String canonical = canonicalStage(stageName);
        String v = stageDisplay.get(canonical);
        if (v != null) return v;
        return stageDisplay.getOrDefault("Running", I18nServiceHolder.msg("maintenance.stage.Running"));
    }

    /** 阶段名规范化：Region/Chunk/File/Done（忽略大小写）→ 标准写法；null/其他 → Running。 */
    private static String canonicalStage(String stageName) {
        if (stageName == null) return "Running";
        for (String name : STAGE_CANONICAL) {
            if (name.equalsIgnoreCase(stageName)) {
                return name;
            }
        }
        return "Running";
    }
}
