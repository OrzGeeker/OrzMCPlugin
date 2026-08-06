package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 玩家晋升状态 YAML 存储（ranks.yml）。
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     playtime_minutes: 600
 *     pending_application: false
 * </pre>
 */
public final class RankYamlStore implements RankStore {

    private static final String FILE = "ranks";

    private final ConfigService configService;

    public RankYamlStore(ConfigService configService) {
        this.configService = configService;
    }

    private String key(UUID id, String field) {
        return "players." + id + "." + field;
    }

    @Override
    public long getPlaytimeMinutes(UUID playerId) {
        FileConfiguration cfg = configService.getConfig(FILE);
        return cfg.getLong(key(playerId, "playtime_minutes"), 0L);
    }

    @Override
    public void setPlaytimeMinutes(UUID playerId, long minutes) {
        FileConfiguration cfg = configService.getConfig(FILE);
        cfg.set(key(playerId, "playtime_minutes"), minutes);
        configService.saveConfig(FILE);
    }

    @Override
    public boolean hasPendingApplication(UUID playerId) {
        FileConfiguration cfg = configService.getConfig(FILE);
        return cfg.getBoolean(key(playerId, "pending_application"), false);
    }

    @Override
    public void setPendingApplication(UUID playerId, boolean pending) {
        FileConfiguration cfg = configService.getConfig(FILE);
        cfg.set(key(playerId, "pending_application"), pending);
        configService.saveConfig(FILE);
    }
}
