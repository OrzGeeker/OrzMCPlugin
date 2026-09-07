package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 单语言语料表：扁平 {@code dot.key → 文案} 的不可变快照。
 *
 * <p>语言包 YAML 按域分组嵌套，读取时递归展平为 dot key（与 Bukkit 配置文件惯例一致），
 * 业务代码按完整 dot key 取值。值允许为空串——覆盖层以空串表达「屏蔽该消息」（见
 * {@link I18nService}）；内置语料的空值由一致性校验拦截。</p>
 */
public final class MessageTable {

    private final Map<String, String> entries;

    private MessageTable(Map<String, String> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /** 空语料表（未加载 / 文件不存在）。 */
    public static final MessageTable EMPTY = new MessageTable(Map.of());

    /** 从配置段（语言包根节点）展平构建；{@code null} → {@link #EMPTY}。 */
    public static MessageTable from(ConfigurationSection section) {
        if (section == null) {
            return EMPTY;
        }
        Map<String, String> flat = new LinkedHashMap<>();
        flatten(section, "", flat);
        return new MessageTable(flat);
    }

    /** 以 {@code overlay} 覆盖 {@code base}（逐 key putAll 语义）；overlay 为空时原样返回 {@code base}。 */
    public static MessageTable merge(MessageTable base, MessageTable overlay) {
        if (overlay == null || overlay.entries.isEmpty()) {
            return base;
        }
        Map<String, String> merged = new LinkedHashMap<>(base.entries);
        merged.putAll(overlay.entries);
        return new MessageTable(merged);
    }

    private static void flatten(ConfigurationSection section, String prefix, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                flatten(child, path, out);
            } else if (value instanceof String text) {
                out.put(path, text);
            }
            // 非字符串叶子（数字/布尔/列表）不属于语料，忽略
        }
    }

    public boolean has(String key) {
        return entries.containsKey(key);
    }

    /** 取文案；key 缺失返回 {@code null}（区分「缺失」与「空串屏蔽」）。 */
    public String get(String key) {
        return entries.get(key);
    }

    /** 全部 key（排序，稳定输出便于校验/测试）。 */
    public Set<String> keys() {
        return Collections.unmodifiableSet(new TreeSet<>(entries.keySet()));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** 空值 key 清单（一致性校验用；排序输出）。 */
    public List<String> emptyValueKeys() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (e.getValue() != null && e.getValue().isEmpty()) {
                out.add(e.getKey());
            }
        }
        Collections.sort(out);
        return out;
    }
}
