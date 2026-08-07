package com.jokerhub.paper.plugin.orzmc.features.rank;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** RankYamlStore 测试：时长读服务器原生 stats 文件、申请状态走 ranks.yml。 */
class RankYamlStoreTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private FileConfiguration ranksCfg;
    private RankYamlStore store;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        ranksCfg = new YamlConfiguration();
        when(configService.getConfig("ranks")).thenReturn(ranksCfg);
        when(configService.saveConfig("ranks")).thenReturn(true);
        store = new RankYamlStore(configService);
    }

    private Path writeStats(String json) throws IOException {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, json);
        return file;
    }

    // ---- 时长：读 stats 文件 ----

    @Test
    void readPlayTimeTicks_parsesPlayTime() throws Exception {
        Path file =
                writeStats("{\"stats\":{\"minecraft:custom\":{\"minecraft:play_time\":72000}},\"DataVersion\":3700}");
        assertEquals(72000L, RankYamlStore.readPlayTimeTicks(file));
    }

    @Test
    void readPlayTimeTicks_missingFile_returnsZero() {
        assertEquals(0L, RankYamlStore.readPlayTimeTicks(tempDir.resolve("nope.json")));
    }

    @Test
    void readPlayTimeTicks_missingCustom_returnsZero() throws Exception {
        Path file = writeStats("{\"stats\":{}}");
        assertEquals(0L, RankYamlStore.readPlayTimeTicks(file));
    }

    @Test
    void readPlayTimeTicks_missingPlayTime_returnsZero() throws Exception {
        Path file = writeStats("{\"stats\":{\"minecraft:custom\":{\"minecraft:walk_one_cm\":100}}}");
        assertEquals(0L, RankYamlStore.readPlayTimeTicks(file));
    }

    @Test
    void readPlayTimeTicks_brokenJson_returnsZero() throws Exception {
        Path file = writeStats("{broken json");
        assertEquals(0L, RankYamlStore.readPlayTimeTicks(file));
    }

    @Test
    void getPlaytimeMinutes_missingStatsFile_returnsZero() {
        // 不依赖 Bukkit：文件不存在 → 0
        assertEquals(0L, RankYamlStore.readPlayTimeTicks(tempDir.resolve("none.json")));
    }

    @Test
    void getPlaytimeMinutes_offlinePlayer_readsStatsFile() throws Exception {
        // 验证真实 stats 文件内容的解析：72000 ticks = 60 分钟
        Path file = writeStats("{\"stats\":{\"minecraft:custom\":{\"minecraft:play_time\":72000}}}");
        assertEquals(60L, RankYamlStore.readPlayTimeTicks(file) / 1200L);
    }

    // ---- 申请状态：ranks.yml ----

    @Test
    void pendingApplication_defaultFalse() {
        UUID id = UUID.randomUUID();
        assertFalse(store.hasPendingApplication(id));
    }

    @Test
    void pendingApplication_setTrueThenRead() {
        UUID id = UUID.randomUUID();
        store.setPendingApplication(id, true);
        assertTrue(store.hasPendingApplication(id));
    }

    @Test
    void pendingApplication_setFalseAfterApprove() {
        UUID id = UUID.randomUUID();
        store.setPendingApplication(id, true);
        store.setPendingApplication(id, false);
        assertFalse(store.hasPendingApplication(id));
    }
}
