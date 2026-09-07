package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import java.util.Locale;

/**
 * 语言值类型：携带归一化语言码（{@code zh-CN} / {@code en-US}）。
 *
 * <p>归一化规则：{@code _} → {@code -}，语言子码小写、区域子码大写（{@code zh_CN} → {@code zh-CN}、
 * {@code EN-us} → {@code en-US}）。构造即归一，{@code equals}/{@code hashCode} 按归一化后的码成立，
 * 可安全作为 {@link java.util.Map} 键。未安装的语言码同样允许构造——决议（{@link I18nService}）时回落。
 */
public record Lang(String code) {

    /** 内置兜底语言（zh 主目录 = key 完整性基线）。 */
    public static final Lang ZH_CN = new Lang("zh-CN");

    public Lang {
        code = normalize(code);
    }

    /** 归一化后构造；{@code null}/空白返回 {@code null}。 */
    public static Lang of(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : new Lang(trimmed);
    }

    /** 由客户端/服务端 {@link Locale} 构造（语言为空返回 {@code null}）。 */
    public static Lang fromLocale(Locale locale) {
        if (locale == null) {
            return null;
        }
        String lang = locale.getLanguage();
        if (lang.isEmpty()) {
            return null;
        }
        if (!locale.getCountry().isEmpty()) {
            lang = lang + "-" + locale.getCountry();
        }
        return new Lang(lang);
    }

    /**
     * 归一化语言码字符串；{@code null}/空白返回 {@code null}。
     * 宽容处理任意输入：非 {@code [A-Za-z0-9-]} 字符不剔除（仅做大小写与分隔符归一），
     * 保证服务端配置/客户端 locale 均可安全进入匹配流程。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        s = s.replace('_', '-');
        int dash = s.indexOf('-');
        String base = (dash < 0 ? s : s.substring(0, dash)).toLowerCase(Locale.ROOT);
        if (dash < 0) {
            return base;
        }
        String rest = s.substring(dash + 1).toUpperCase(Locale.ROOT);
        return base + "-" + rest;
    }

    @Override
    public String toString() {
        return code;
    }
}
