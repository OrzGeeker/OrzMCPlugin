package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 玩家晋升状态存储（ranks.yml + 服务器原生 stats 文件）。
 *
 * <p>时长数据直接读 Minecraft 服务器原生统计文件
 * {@code <主世界>/players/stats/<uuid>.json} 的 {@code minecraft:play_time}（tick），
 * 与服务器同源、玩家离线也可读、无自计算误差。
 *
 * <p>说明：不使用 {@code OfflinePlayer.getStatistic()}——Paper 1.20.2+ 中
 * {@code PLAY_ONE_MINUTE} 枚举生成键 {@code play_one_minute}，但服务器内部统计键
 * 已改名为 {@code play_time}，映射断裂导致恒返回 0（PaperMC/Paper#9507）。
 *
 * <p>stats 目录每次读取时动态解析（主世界 world folder），避免插件构造时
 * 世界尚未加载导致路径错误。</p>
 */
public final class RankYamlStore implements RankStore {

    private static final String FILE = "ranks";
    private static final String PLAY_TIME_KEY = "minecraft:play_time";

    private final ConfigService configService;

    public RankYamlStore(ConfigService configService) {
        this.configService = configService;
    }

    /** 服务器原生 stats 目录（动态解析，世界加载后有效）。
     *
     * <p>Paper 26+ 中 {@code getWorldFolder()} 返回维度子目录
     * {@code world/dimensions/minecraft/overworld}，stats 在世界根
     * {@code world/players/stats}。从维度目录向上遍历，找含 {@code players/stats} 的目录。</p>
     */
    static Path statsDirectory() {
        var worlds = Bukkit.getWorlds();
        if (!worlds.isEmpty()) {
            Path p = worlds.get(0).getWorldFolder().getAbsoluteFile().toPath();
            while (p != null) {
                if (Files.exists(p.resolve("players").resolve("stats"))) {
                    return p.resolve("players").resolve("stats");
                }
                p = p.getParent();
            }
        }
        return Bukkit.getWorldContainer()
                .getAbsoluteFile()
                .toPath()
                .resolve("world")
                .resolve("players")
                .resolve("stats");
    }

    @Override
    public long getPlaytimeMinutes(UUID playerId) {
        Path statsFile = statsDirectory().resolve(playerId + ".json");
        return readPlayTimeTicks(statsFile) / 1200L; // 20 tick/s × 60 s/min
    }

    /** 读取 stats 文件的 minecraft:play_time（tick），文件缺失/异常返回 0。 */
    static long readPlayTimeTicks(Path statsFile) {
        if (!Files.exists(statsFile)) {
            return 0;
        }
        try {
            var content = Files.readString(statsFile);
            var json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            var custom = json.getAsJsonObject("stats").getAsJsonObject("minecraft:custom");
            var playTime = custom == null ? null : custom.get(PLAY_TIME_KEY);
            return playTime == null ? 0 : playTime.getAsLong();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean hasPendingApplication(UUID playerId) {
        FileConfiguration cfg = configService.getConfig(FILE);
        return cfg.getBoolean("players." + playerId + ".pending_application", false);
    }

    @Override
    public void setPendingApplication(UUID playerId, boolean pending) {
        FileConfiguration cfg = configService.getConfig(FILE);
        cfg.set("players." + playerId + ".pending_application", pending);
        configService.saveConfig(FILE);
    }
}
