package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.features.review.ReviewRequest;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewStore;
import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 权限模块统一存储（permission.yml，三段式）。
 *
 * <pre>
 * permission.yml
 * ├── config:  member-threshold-hours: 10    # 静态配置节
 * ├── ranks:   players.&lt;uuid&gt;.promoted      # 晋升状态节（运行时）
 * └── reviews: requests.&lt;id&gt;: {...}         # 审核记录节（运行时）
 * </pre>
 *
 * <p>同时实现 {@link RankStore}（ranks 节 + stats 时长）与 {@link ReviewStore}
 * （reviews 节），一个文件统一管理，替代原 ranks.yml 单文件存储。
 * config 节静态读取，ranks/reviews 节 markAlwaysSave 保证频繁写不丢。</p>
 *
 * <p>时长数据直接读 Minecraft 服务器原生统计文件
 * {@code <主世界>/players/stats/<uuid>.json} 的 {@code minecraft:play_time}（tick），
 * 与服务器同源、玩家离线也可读、无自计算误差。</p>
 */
public final class PermissionStore implements RankStore, ReviewStore {

    private static final String FILE = "permission";
    private static final String CONFIG_SECTION = "config";
    private static final String RANKS_SECTION = "ranks.players";
    private static final String REVIEWS_SECTION = "reviews.requests";
    private static final String PLAY_TIME_KEY = "minecraft:play_time";
    private static final int DEFAULT_MEMBER_THRESHOLD_HOURS = 10;

    private final ConfigService configService;

    public PermissionStore(ConfigService configService) {
        this.configService = configService;
    }

    // ---- 静态配置节 ----

    /** 晋升 member 阈值（小时），从 config 节读取，缺省 10。 */
    public int memberThresholdHours() {
        FileConfiguration cfg = configService.getConfig(FILE);
        return cfg.getInt(CONFIG_SECTION + ".member-threshold-hours", DEFAULT_MEMBER_THRESHOLD_HOURS);
    }

    // ---- RankStore：ranks 节 + stats 时长 ----

    @Override
    public boolean hasPromoted(UUID playerId) {
        FileConfiguration cfg = configService.getConfig(FILE);
        return cfg.getBoolean(RANKS_SECTION + "." + playerId + ".promoted", false);
    }

    @Override
    public void markPromoted(UUID playerId) {
        FileConfiguration cfg = configService.getConfig(FILE);
        cfg.set(RANKS_SECTION + "." + playerId + ".promoted", true);
        configService.saveConfig(FILE);
    }

    @Override
    public long getPlaytimeMinutes(UUID playerId) {
        Path statsFile = statsDirectory().resolve(playerId + ".json");
        return readPlayTimeTicks(statsFile) / 1200L; // 20 tick/s × 60 s/min
    }

    // ---- ReviewStore：reviews 节 ----

    @Override
    public void save(ReviewRequest request) {
        FileConfiguration cfg = configService.getConfig(FILE);
        writeRequest(cfg, request);
        configService.saveConfig(FILE);
    }

    @Override
    public Optional<ReviewRequest> findById(String id) {
        FileConfiguration cfg = configService.getConfig(FILE);
        String path = REVIEWS_SECTION + "." + id;
        if (!cfg.contains(path + ".type")) {
            return Optional.empty();
        }
        return Optional.of(readRequest(cfg, path));
    }

    @Override
    public List<ReviewRequest> listPending() {
        FileConfiguration cfg = configService.getConfig(FILE);
        ConfigurationSection section = cfg.getConfigurationSection(REVIEWS_SECTION);
        if (section == null) {
            return List.of();
        }
        List<ReviewRequest> pending = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ReviewRequest request = readRequest(cfg, REVIEWS_SECTION + "." + id);
            if (request.status() == ReviewRequest.Status.PENDING) {
                pending.add(request);
            }
        }
        pending.sort(Comparator.comparingLong(ReviewRequest::createdAt));
        return pending;
    }

    @Override
    public List<ReviewRequest> listByApplicant(UUID applicantId) {
        FileConfiguration cfg = configService.getConfig(FILE);
        ConfigurationSection section = cfg.getConfigurationSection(REVIEWS_SECTION);
        if (section == null) {
            return List.of();
        }
        List<ReviewRequest> found = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ReviewRequest request = readRequest(cfg, REVIEWS_SECTION + "." + id);
            if (request.applicantId().equals(applicantId)) {
                found.add(request);
            }
        }
        found.sort(Comparator.comparingLong(ReviewRequest::createdAt));
        return found;
    }

    @Override
    public Optional<ReviewRequest> pendingFor(String typeId, UUID applicantId) {
        return listPending().stream()
                .filter(r -> r.typeId().equals(typeId) && r.applicantId().equals(applicantId))
                .findFirst();
    }

    @Override
    public boolean hasPending(String typeId, UUID applicantId) {
        return pendingFor(typeId, applicantId).isPresent();
    }

    // ---- 数据迁移（一期 ranks.yml → permission.yml）----

    /**
     * 启动时迁移一期 ranks.yml 遗留数据到 permission.yml（幂等，可重复执行）：
     * <ul>
     *   <li>{@code players.<uuid>.promoted} → ranks 节</li>
     *   <li>{@code players.<uuid>.pending_application=true} → reviews 节（BUILDER_PROMOTION, PENDING）</li>
     * </ul>
     * 已迁移的玩家（permission.yml 已有 promoted 标记或待审记录）跳过，避免重复。
     */
    public void migrateLegacyRanks() {
        FileConfiguration legacy = configService.loadFile("ranks.yml");
        if (legacy == null) {
            return;
        }
        ConfigurationSection players = legacy.getConfigurationSection("players");
        if (players == null || players.getKeys(false).isEmpty()) {
            return;
        }
        FileConfiguration cfg = configService.getConfig(FILE);
        boolean changed = false;
        for (String uuidStr : players.getKeys(false)) {
            String rankPath = RANKS_SECTION + "." + uuidStr;
            if (legacy.getBoolean("players." + uuidStr + ".promoted", false) && !cfg.contains(rankPath + ".promoted")) {
                cfg.set(rankPath + ".promoted", true);
                changed = true;
            }
            if (legacy.getBoolean("players." + uuidStr + ".pending_application", false)) {
                UUID applicant = UUID.fromString(uuidStr);
                if (!hasPending("builder-promotion", applicant)) {
                    ReviewRequest migrated = new ReviewRequest(
                            newRequestId(),
                            "builder-promotion",
                            applicant,
                            java.util.Map.of("target-group", "builder"),
                            ReviewRequest.Status.PENDING,
                            System.currentTimeMillis(),
                            0L,
                            null);
                    // 攒批：直接写 cfg，统一在迁移末尾一次落盘
                    writeRequest(cfg, migrated);
                    changed = true;
                }
            }
        }
        if (changed) {
            configService.saveConfig(FILE);
            // 迁移成功：旧文件改名 .bak，避免残留误导（幂等：已迁移的玩家跳过）
            java.io.File legacyFile = new java.io.File(configService.dataFolder(), "ranks.yml");
            if (legacyFile.exists()) {
                legacyFile.renameTo(new java.io.File(legacyFile.getParentFile(), "ranks.yml.bak"));
            }
        }
    }

    /** 写一条审核记录到 cfg（不落盘，调用方负责 saveConfig）。 */
    private void writeRequest(FileConfiguration cfg, ReviewRequest request) {
        String path = REVIEWS_SECTION + "." + request.id();
        cfg.set(path + ".type", request.typeId());
        cfg.set(path + ".applicant", request.applicantId().toString());
        if (request.data() != null && !request.data().isEmpty()) {
            ConfigurationSection dataSection = cfg.createSection(path + ".data");
            request.data().forEach(dataSection::set);
        }
        cfg.set(path + ".status", request.status().name());
        cfg.set(path + ".created-at", request.createdAt());
        cfg.set(path + ".reviewed-at", request.reviewedAt());
        cfg.set(path + ".reviewer", request.reviewerName());
    }

    private static String newRequestId() {
        return Long.toHexString(System.currentTimeMillis()) + "-"
                + Integer.toHexString(UUID.randomUUID().hashCode());
    }

    private ReviewRequest readRequest(FileConfiguration cfg, String path) {
        String typeId = cfg.getString(path + ".type", "");
        UUID applicant = UUID.fromString(cfg.getString(
                path + ".applicant", UUID.nameUUIDFromBytes(new byte[0]).toString()));
        Map<String, String> data = new HashMap<>();
        ConfigurationSection dataSection = cfg.getConfigurationSection(path + ".data");
        if (dataSection != null) {
            dataSection.getKeys(false).forEach(k -> data.put(k, dataSection.getString(k)));
        }
        ReviewRequest.Status status = ReviewRequest.Status.valueOf(cfg.getString(path + ".status", "PENDING"));
        long createdAt = cfg.getLong(path + ".created-at", 0L);
        long reviewedAt = cfg.getLong(path + ".reviewed-at", 0L);
        String reviewer = cfg.getString(path + ".reviewer");
        return new ReviewRequest(
                path.substring(path.lastIndexOf('.') + 1),
                typeId,
                applicant,
                data,
                status,
                createdAt,
                reviewedAt,
                reviewer);
    }

    // ---- stats 时长读取（与一期同源逻辑） ----

    /** 服务器原生 stats 目录（动态解析，世界加载后有效）。 */
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
}
