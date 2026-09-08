package com.jokerhub.paper.plugin.orzmc.infra.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * i18n P4d：templates.yml 存量盘旧正文自动迁移（config-version 13 → 14 升级触发，幂等）。
 *
 * <p>P4a/P4b/P4c 已把命令回复/事件/维护正文从 bundled templates.yml 迁入语言包（bot.* / event.* /
 * maintenance.motd.* / maintenance.stage.*），但「补缺不改值」升级不动磁盘现值——存量服磁盘仍持有
 * 迁移前的 zh 字面正文，被「磁盘正文优先」渲染机制永远显示为 zh。本类在升级（磁盘版本 &lt; 14）时
 * 识别「磁盘正文 == 迁移前的内置默认」的键并处理，让存量服随 default_lang 双语：</p>
 *
 * <ul>
 *   <li>事件正文（{@link TemplateKeys#EVENT_LANG_BACKED} 30 键）+ {@code maintenance_motd_*} 4 键：
 *       等于旧内置默认（= zh 语言包 event.* / maintenance.motd.* 值，迁出时逐字一致）
 *       → 删除键（磁盘缺失后 renderEvent / {@code MaintenanceTexts} 回落语言包）；</li>
 *   <li>{@code stage_cn} 段（Region/Chunk/File/Done 默认映射 = zh maintenance.stage.*）：逐条删除
 *       默认相等项，段清空后整体移除（MaintenanceTexts.stageMap 回落语言包）；</li>
 *   <li>{@code command_*} 4 键（P4a 迁出为 {@code {message}} 直通壳，正文无语言包对应，用 git 快照
 *       硬编码旧默认）：等于旧默认 → 翻转为 {@code {message}}（P4a 渲染路径要求正文存在，不可删）。</li>
 * </ul>
 *
 * <p>与旧默认不等（服主真实定制 / 历史更早文案变体）一律保留磁盘值（磁盘优先语义，零回归），
 * 记录在案供升级报告。旧默认比对基准：事件/维护/阶段取 bundled zh 语言包（单一事实源，避免快照
 * 漂移）；语言包缺失键或类路径不可用时按「定制保留」安全处理（fail-safe，不误删）。</p>
 */
public final class TemplatesBodyMigration {
    private TemplatesBodyMigration() {}

    /** 迁移结果（键名不带 {@code templates.} 前缀，供升级报告）。 */
    public record MigrationSummary(List<String> removedKeys, List<String> flippedKeys, List<String> keptCustomKeys) {
        public MigrationSummary {
            removedKeys = List.copyOf(removedKeys);
            flippedKeys = List.copyOf(flippedKeys);
            keptCustomKeys = List.copyOf(keptCustomKeys);
        }

        public boolean isEmpty() {
            return removedKeys.isEmpty() && flippedKeys.isEmpty() && keptCustomKeys.isEmpty();
        }
    }

    public static final MigrationSummary NONE = new MigrationSummary(List.of(), List.of(), List.of());

    // command_*（P4a {message} 直通壳）迁移前的内置默认正文（git 快照 97ad99b^；语言包无对应）。
    private static final String OLD_COMMAND_PLAYERS = "------当前在线({online_count}/{max_count})------\n{online_list}";
    private static final String OLD_COMMAND_WHITELIST_HEADER = "------当前白名单玩家({count})------";
    private static final String OLD_COMMAND_WHITELIST_PAGE = "{header}\n第{page}/{total}页\n{body}";
    private static final String OLD_COMMAND_WHITELIST_CLEANUP = "------白名单清理------\n{removed_list}";

    /** 事件/维护键 → zh 语言包 key（旧内置默认的单一事实源）。 */
    private record LangRef(String templateKey, String langKey) {}

    private static final LangRef[] EVENT_REFS = buildEventRefs();

    private static LangRef[] buildEventRefs() {
        List<LangRef> refs = new ArrayList<>();
        for (String key : TemplateKeys.EVENT_LANG_BACKED) {
            refs.add(new LangRef(key, "event." + key));
        }
        refs.add(new LangRef("maintenance_motd_backup", "maintenance.motd.backup"));
        refs.add(new LangRef("maintenance_motd_optimize", "maintenance.motd.optimize"));
        refs.add(new LangRef("maintenance_motd_manual", "maintenance.motd.manual"));
        refs.add(new LangRef("maintenance_motd_progress_line", "maintenance.motd.progress_line"));
        return refs.toArray(new LangRef[0]);
    }

    /** command_* → 旧默认（翻转目标 {@code {message}}）。 */
    private static final Map<String, String> OLD_COMMAND_DEFAULTS = commandDefaults();

    private static Map<String, String> commandDefaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("command_players", OLD_COMMAND_PLAYERS);
        m.put("command_whitelist_header", OLD_COMMAND_WHITELIST_HEADER);
        m.put("command_whitelist_page", OLD_COMMAND_WHITELIST_PAGE);
        m.put("command_whitelist_cleanup", OLD_COMMAND_WHITELIST_CLEANUP);
        return m;
    }

    /** stage_cn 段默认映射（Region/Chunk/File/Done；Running 为兜底不入磁盘段）。 */
    private static final String[] STAGE_KEYS = {"Region", "Chunk", "File", "Done"};

    /**
     * 迁移磁盘 templates.yml（调用方保证已备份）。仅处理「磁盘正文 == 迁移前旧默认」的键；
     * 磁盘缺失或已为 {@code {message}} 直通壳的键自动跳过（幂等，重复调用零操作）。
     */
    public static MigrationSummary migrate(FileConfiguration cfg) {
        if (cfg == null) {
            return NONE;
        }
        Map<String, String> zh = loadZhDefaults();
        List<String> removed = new ArrayList<>();
        List<String> flipped = new ArrayList<>();
        List<String> kept = new ArrayList<>();

        // 1) 事件正文 + 维护场景正文：等于 zh 默认 → 删键（回落语言包）
        for (LangRef ref : EVENT_REFS) {
            String path = "templates." + ref.templateKey();
            String disk = cfg.getString(path);
            if (disk == null) {
                continue; // 全新安装/已迁移：无正文，跳过
            }
            if ("{message}".equals(disk)) {
                continue; // P5-2：直通壳残留（server_load/server_stop 旧壳）等同无正文，跳过不迁
            }
            String def = zh.get(ref.langKey());
            if (def != null && def.equals(disk)) {
                cfg.set(path, null);
                removed.add(ref.templateKey());
            } else {
                kept.add(ref.templateKey());
            }
        }

        // 2) command_*（P4a {message} 直通壳）：等于 git 快照旧默认 → 翻 {message}
        for (Map.Entry<String, String> entry : OLD_COMMAND_DEFAULTS.entrySet()) {
            String path = "templates." + entry.getKey();
            String disk = cfg.getString(path);
            if (disk == null) {
                continue;
            }
            if (entry.getValue().equals(disk)) {
                cfg.set(path, "{message}");
                flipped.add(entry.getKey());
            } else {
                kept.add(entry.getKey());
            }
        }

        // 3) stage_cn 段：逐条默认相等删除；段空后整体移除
        ConfigurationSection stageSection = cfg.getConfigurationSection("templates.stage_cn");
        if (stageSection != null) {
            for (String stage : STAGE_KEYS) {
                String path = "templates.stage_cn." + stage;
                if (!cfg.contains(path)) {
                    continue;
                }
                String disk = cfg.getString(path);
                String def = zh.get("maintenance.stage." + stage);
                if (def != null && def.equals(disk)) {
                    cfg.set(path, null);
                    removed.add("stage_cn." + stage);
                } else {
                    kept.add("stage_cn." + stage);
                }
            }
            ConfigurationSection after = cfg.getConfigurationSection("templates.stage_cn");
            if (after != null && after.getKeys(false).isEmpty()) {
                cfg.set("templates.stage_cn", null);
            }
        }

        return new MigrationSummary(removed, flipped, kept);
    }

    /** 读 bundled zh 主语言包并展平为 dotted key → 值（单测同包可注入覆盖）。 */
    static Map<String, String> loadZhDefaults() {
        Map<String, String> out = new LinkedHashMap<>();
        try (InputStream in =
                TemplatesBodyMigration.class.getClassLoader().getResourceAsStream("messages/messages_zh-CN.yml")) {
            if (in == null) {
                return out; // 类路径缺 zh 包（异常环境）：空表 → 全部按定制保留（fail-safe）
            }
            FileConfiguration yml =
                    YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            flatten(yml, "", out);
        } catch (IOException e) {
            return out;
        }
        return out;
    }

    private static void flatten(ConfigurationSection section, String prefix, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                flatten(child, path, out);
            } else if (value != null) {
                out.put(path, String.valueOf(value));
            }
        }
    }
}
