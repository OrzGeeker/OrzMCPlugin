package com.jokerhub.paper.plugin.orzmc.infra.templates;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.infra.config.TemplateKeys;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.Templates;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nServiceHolder;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public final class TemplateService {
    private TemplateService() {}

    /**
     * 渲染事件通知包络（i18n P4b 起）。
     *
     * <p>正文来源分两类：</p>
     * <ul>
     *   <li>{@link TemplateKeys#isLangBacked(String)} 事件（player/geoip/whitelist/tnt/exception 等）：
     *       磁盘 templates.yml 正文若存在（存量服/服主定制）优先渲染——P4d 升级链统一迁移前保 zh 基线、
     *       不丢定制；磁盘缺失（全新安装）回落语言包 {@code event.<name>}（默认语言 R1，原文含占位符未渲染）。</li>
     *   <li>其余（maintenance_* / server_load / server_stop）仍走 {@link Templates} 记录默认（数据/直通壳模板）。</li>
     * </ul>
     * 格式（CODE_BLOCK/PLAIN）一律由 templates.format 表承担。
     */
    public static MessageEnvelope renderEvent(
            String eventKey, FileConfiguration templatesCfg, Templates templates, Map<String, String> vars) {
        String template;
        if (eventKey == null || eventKey.isEmpty()) {
            template = "";
        } else if (TemplateKeys.isLangBacked(eventKey)) {
            template = diskOrLangEventBody(eventKey, templatesCfg);
        } else {
            template = templateForEvent(eventKey, templates);
        }
        return TemplateRenderer.renderEnvelope(eventKey, template == null ? "" : template, vars, templatesCfg);
    }

    /** 语言包承载事件正文：磁盘正文优先（存量/定制），缺失回落语言包默认语言原文。 */
    private static String diskOrLangEventBody(String eventKey, FileConfiguration templatesCfg) {
        if (templatesCfg != null) {
            String disk = templatesCfg.getString("templates." + eventKey);
            if (disk != null && !disk.isEmpty()) {
                return disk;
            }
        }
        return I18nServiceHolder.msg("event." + eventKey);
    }

    private static String templateForEvent(String eventKey, Templates templates) {
        if (eventKey == null || eventKey.isEmpty()) {
            return "";
        }
        if ("maintenance_backup_stage".equals(eventKey)) return templates.maintenanceBackupStage();
        if ("maintenance_backup_done".equals(eventKey)) return templates.maintenanceBackupDone();
        if ("maintenance_backup_error".equals(eventKey)) return templates.maintenanceBackupError();
        if ("maintenance_optimize_stage".equals(eventKey)) return templates.maintenanceOptimizeStage();
        if ("maintenance_optimize_done".equals(eventKey)) return templates.maintenanceOptimizeDone();
        if ("maintenance_optimize_error".equals(eventKey)) return templates.maintenanceOptimizeError();
        if ("server_load".equals(eventKey)) return templates.serverLoad();
        if ("server_stop".equals(eventKey)) return templates.serverStop();
        return "";
    }
}
