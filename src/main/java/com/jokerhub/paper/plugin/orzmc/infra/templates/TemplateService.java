package com.jokerhub.paper.plugin.orzmc.infra.templates;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.infra.config.TemplateKeys;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nServiceHolder;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public final class TemplateService {
    private TemplateService() {}

    /**
     * 渲染事件通知包络（i18n P4b 起）。
     *
     * <p>正文来源：所有事件已迁语言包 {@code event.<name>}（P4b-P5 逐域）；磁盘 templates.yml 正文若存在且非
     * {@code {message}} 直通壳（存量服/服主定制）优先渲染——P4d 升级链已把「磁盘正文 == 旧内置默认」的键清除，
     * 存量服正文缺失/壳后回落语言包（默认语言 R1）；有定制仍磁盘优先。
     * 格式（CODE_BLOCK/PLAIN）一律由 templates.format 表承担。</p>
     */
    public static MessageEnvelope renderEvent(
            String eventKey, FileConfiguration templatesCfg, Map<String, String> vars) {
        String template;
        if (eventKey == null || eventKey.isEmpty()) {
            template = "";
        } else if (TemplateKeys.isLangBacked(eventKey)) {
            template = diskOrLangEventBody(eventKey, templatesCfg);
        } else {
            template = "";
        }
        return TemplateRenderer.renderEnvelope(eventKey, template == null ? "" : template, vars, templatesCfg);
    }

    /** 语言包承载事件正文：磁盘正文优先（存量/定制），缺失回落语言包默认语言原文。 */
    private static String diskOrLangEventBody(String eventKey, FileConfiguration templatesCfg) {
        if (templatesCfg != null) {
            String disk = templatesCfg.getString("templates." + eventKey);
            // P5-2：磁盘 {message} 直通壳视为无正文（历史升级残留/新装缺省），回落语言包；非壳正文 = 服主定制仍优先
            if (disk != null && !disk.isEmpty() && !"{message}".equals(disk)) {
                return disk;
            }
        }
        return I18nServiceHolder.msg("event." + eventKey);
    }
}
