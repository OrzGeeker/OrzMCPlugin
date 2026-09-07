package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内置语言包一致性健康检查（纯静态，资源级）。
 *
 * <p>zh-CN 是 key 完整性基线：其余 bundled 包必须与 zh 的 key 集完全一致（缺/多都报）、
 * 每个共有 key 的 {@code {var}} 占位符集一致（防漏占位符导致译文出现 {@code {player}} 字面量）、
 * bundled 值不得为空（空串是 custom 覆盖层的「屏蔽」语义，内置包不允许）。</p>
 */
public final class I18nHealth {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]*)\\}");

    private I18nHealth() {}

    /** 对内置包执行一致性检查；一致返回空列表。 */
    public static List<String> check(ClassLoader resources) {
        List<String> issues = new ArrayList<>();
        MessageTable zh = loadMaster(resources);
        if (zh == null) {
            issues.add("缺少内置主语言包: messages/messages_zh-CN.yml");
            return issues;
        }
        if (!zh.emptyValueKeys().isEmpty()) {
            issues.add("内置 zh-CN 存在空值 key: " + zh.emptyValueKeys());
        }
        for (String code : I18nLoader.CODES) {
            if ("zh-CN".equals(code)) {
                continue;
            }
            MessageTable other = I18nLoader.bundled(resources, code);
            if (other == null) {
                issues.add("缺少内置语言包: messages/messages_" + code + ".yml");
                continue;
            }
            if (!other.emptyValueKeys().isEmpty()) {
                issues.add("内置 " + code + " 存在空值 key: " + other.emptyValueKeys());
            }
            Set<String> missing = new TreeSet<>(zh.keys());
            missing.removeAll(other.keys());
            if (!missing.isEmpty()) {
                issues.add("语言包 " + code + " 缺失 key（相对 zh-CN）: " + missing);
            }
            Set<String> extra = new TreeSet<>(other.keys());
            extra.removeAll(zh.keys());
            if (!extra.isEmpty()) {
                issues.add("语言包 " + code + " 含多余 key（zh-CN 无）: " + extra);
            }
            for (String key : zh.keys()) {
                if (!other.has(key)) {
                    continue;
                }
                Set<String> zhVars = placeholderSet(zh.get(key));
                Set<String> otherVars = placeholderSet(other.get(key));
                if (!zhVars.equals(otherVars)) {
                    issues.add("语言包 " + code + " key=" + key + " 占位符不一致: zh=" + zhVars + " vs " + otherVars);
                }
            }
        }
        return issues;
    }

    private static MessageTable loadMaster(ClassLoader resources) {
        return I18nLoader.bundled(resources, "zh-CN");
    }

    /** 提取模板中的 {@code {var}} 集合（含 var 顺序无关的等值语义）。 */
    static Set<String> placeholderSet(String template) {
        Set<String> vars = new TreeSet<>();
        if (template == null || template.isEmpty()) {
            return vars;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            vars.add(m.group(1));
        }
        return vars;
    }
}
