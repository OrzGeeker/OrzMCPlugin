package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** MaintenanceTexts（P4c-2）：磁盘正文优先（存量/服主定制），缺失回落语言包 zh 默认；阶段名规范化。 */
class MaintenanceTextsTest {

    @Test
    void from_noDisk_usesLanguagePackDefaults() {
        MaintenanceTexts texts = MaintenanceTexts.from(new YamlConfiguration());
        // I18nServiceHolder 未注入时回落 bundled zh 主目录（与旧 templates.yml 默认逐字一致）
        Assertions.assertEquals("服务器地图备份中，请稍后再试", texts.motdBackup());
        Assertions.assertEquals("服务器地图优化中，请稍后再试", texts.motdOptimize());
        Assertions.assertEquals("服务器维护中，请稍后再试", texts.motdManual());
        Assertions.assertEquals("进度：{stage} {percent}% 预计剩余 {eta}秒", texts.motdProgressLine());
    }

    @Test
    void from_diskCustomBodyWins() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.maintenance_motd_backup", "自定义维护公告");
        cfg.set("templates.maintenance_motd_progress_line", "剩余 {eta}s");
        MaintenanceTexts texts = MaintenanceTexts.from(cfg);
        Assertions.assertEquals("自定义维护公告", texts.motdBackup());
        Assertions.assertEquals("剩余 {eta}s", texts.motdProgressLine());
        // 未覆盖键仍走语言包默认
        Assertions.assertEquals("服务器地图优化中，请稍后再试", texts.motdOptimize());
    }

    @Test
    void stage_knownAndUnknownNames() {
        MaintenanceTexts texts = MaintenanceTexts.from(new YamlConfiguration());
        Assertions.assertEquals("区域", texts.stage("Region"));
        Assertions.assertEquals("区域", texts.stage("region"));
        Assertions.assertEquals("区块", texts.stage("Chunk"));
        Assertions.assertEquals("文件", texts.stage("File"));
        Assertions.assertEquals("完成", texts.stage("Done"));
        Assertions.assertEquals("进行中", texts.stage("ChunkProgress"));
        Assertions.assertEquals("进行中", texts.stage(null));
    }

    @Test
    void stage_diskCustomWinsOverLang() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("templates.stage_cn.Region", "加载区域");
        MaintenanceTexts texts = MaintenanceTexts.from(cfg);
        Assertions.assertEquals("加载区域", texts.stage("Region"));
        Assertions.assertEquals("进行中", texts.stage("Unknown"), "未知阶段回落 Running 默认");
    }
}
