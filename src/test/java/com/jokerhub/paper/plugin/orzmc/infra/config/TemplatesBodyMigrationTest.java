package com.jokerhub.paper.plugin.orzmc.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** TemplatesBodyMigration（P4d）：磁盘正文 == 迁移前旧默认 → 删键/翻 {message}；定制/变体保留。 */
class TemplatesBodyMigrationTest {

    private static final Map<String, String> ZH = TemplatesBodyMigration.loadZhDefaults();

    @Test
    void migrate_removesEventAndMotdBodiesEqualToZhDefault() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.player_join", ZH.get("event.player_join"));
        cfg.set("templates.whitelist_block", ZH.get("event.whitelist_block"));
        cfg.set("templates.maintenance_motd_manual", ZH.get("maintenance.motd.manual"));
        cfg.set("templates.maintenance_motd_progress_line", ZH.get("maintenance.motd.progress_line"));
        cfg.set("templates.stage_cn.Region", ZH.get("maintenance.stage.Region"));
        cfg.set("templates.stage_cn.Done", ZH.get("maintenance.stage.Done"));

        TemplatesBodyMigration.MigrationSummary r = TemplatesBodyMigration.migrate(cfg);

        assertFalse(cfg.contains("templates.player_join"));
        assertFalse(cfg.contains("templates.whitelist_block"));
        assertFalse(cfg.contains("templates.maintenance_motd_manual"));
        assertFalse(cfg.contains("templates.maintenance_motd_progress_line"));
        assertFalse(cfg.contains("templates.stage_cn"), "默认映射全删后空段应整体移除");
        assertTrue(r.removedKeys().contains("player_join"));
        assertTrue(r.removedKeys().contains("stage_cn.Region"));
        assertTrue(r.keptCustomKeys().isEmpty());
    }

    @Test
    void migrate_flipsCommandShellsEqualToOldDefault() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.command_players", "------当前在线({online_count}/{max_count})------\n{online_list}");
        cfg.set("templates.command_whitelist_page", "{header}\n第{page}/{total}页\n{body}");
        cfg.set("templates.command_whitelist_header", "------当前白名单玩家({count})------");
        cfg.set("templates.command_whitelist_cleanup", "------白名单清理------\n{removed_list}");

        TemplatesBodyMigration.MigrationSummary r = TemplatesBodyMigration.migrate(cfg);

        assertEquals("{message}", cfg.getString("templates.command_players"));
        assertEquals("{message}", cfg.getString("templates.command_whitelist_page"));
        assertEquals("{message}", cfg.getString("templates.command_whitelist_header"));
        assertEquals("{message}", cfg.getString("templates.command_whitelist_cleanup"));
        assertEquals(4, r.flippedKeys().size());
    }

    @Test
    void migrate_keepsCustomizedBodiesAndStageEntries() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.player_join", "自定义上线消息 {name}");
        cfg.set("templates.command_players", "自定义列表");
        cfg.set("templates.stage_cn.Chunk", "区块扫描");

        TemplatesBodyMigration.MigrationSummary r = TemplatesBodyMigration.migrate(cfg);

        assertEquals("自定义上线消息 {name}", cfg.getString("templates.player_join"));
        assertEquals("自定义列表", cfg.getString("templates.command_players"));
        assertEquals("区块扫描", cfg.getString("templates.stage_cn.Chunk"));
        assertTrue(r.keptCustomKeys().contains("player_join"));
        assertTrue(r.keptCustomKeys().contains("command_players"));
        assertTrue(r.keptCustomKeys().contains("stage_cn.Chunk"));
        assertTrue(r.removedKeys().isEmpty());
        assertTrue(r.flippedKeys().isEmpty());
    }

    @Test
    void migrate_idempotentOnFreshOrMigratedConfig() {
        // 全新安装（无正文，仅数据键/直通壳）与已迁移状态 → 零操作
        YamlConfiguration fresh = new YamlConfiguration();
        fresh.set("templates.server_stop", "{message}");
        fresh.set("templates.command_output", "{message}");
        fresh.set("templates.coord.scale", 1.0);
        fresh.set("templates.world_alias.world", "主世界");

        TemplatesBodyMigration.MigrationSummary r = TemplatesBodyMigration.migrate(fresh);

        assertTrue(r.isEmpty(), "无旧正文时不应有任何迁移/保留动作");
        assertEquals("{message}", fresh.getString("templates.server_stop"));
        assertEquals("主世界", fresh.getString("templates.world_alias.world"), "数据键不受影响");
    }

    @Test
    void migrate_keepsDiskValueWhenZhPackKeyMissing() {
        // 语言包缺该键（异常/未来键）→ fail-safe：按定制保留，不误删
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.some_future_event", "内容");

        TemplatesBodyMigration.MigrationSummary r = TemplatesBodyMigration.migrate(cfg);

        assertTrue(cfg.contains("templates.some_future_event"));
        assertTrue(r.isEmpty());
    }
}
