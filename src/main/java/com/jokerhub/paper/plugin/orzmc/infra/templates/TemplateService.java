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
     * <p>正文来源分两类：</p>
     * <ul>
     *   <li>{@link TemplateKeys#isLangBacked(String)} 事件：磁盘 templates.yml 正文若存在
     *       （存量服/服主定制）优先渲染——P4d 升级链已把「磁盘正文 == 旧内置默认」的键清除，
     *       存量服正文缺失后回落语言包 {@code event.<name>}（默认语言 R1）；有定制仍磁盘优先。</li>
     *   <li>其余（server_load / server_stop）：{@code {message}} 直通壳——磁盘自定义正文优先，
     *       缺失回落 {@code {message}}（消息由调用方组装后经 {@code message} 变量注入）。</li>
     * </ul>
     * 格式（CODE_BLOCK/PLAIN）一律由 templates.format 表承担。
     */
    public static MessageEnvelope renderEvent(
            String eventKey, FileConfiguration templatesCfg, Map<String, String> vars) {
        String template;
        if (eventKey == null || eventKey.isEmpty()) {
            template = "";
        } else if (TemplateKeys.isLangBacked(eventKey)) {
            template = diskOrLangEventBody(eventKey, templatesCfg);
        } else if ("server_load".equals(eventKey) || "server_stop".equals(eventKey)) {
            template = shellOrDiskBody(eventKey, templatesCfg);
        } else {
            template = "";
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

    /** 直通壳事件正文（server_load / server_stop）：磁盘自定义正文优先，缺失回落 {@code {message}}。 */
    private static String shellOrDiskBody(String eventKey, FileConfiguration templatesCfg) {
        if (templatesCfg != null) {
            String disk = templatesCfg.getString("templates." + eventKey);
            if (disk != null && !disk.isEmpty()) {
                return disk;
            }
        }
        return "{message}";
    }
}
