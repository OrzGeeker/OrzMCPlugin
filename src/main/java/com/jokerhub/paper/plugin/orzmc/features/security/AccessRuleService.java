package com.jokerhub.paper.plugin.orzmc.features.security;

import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 访问规则服务：统一管理 IP 黑名单与玩家名规则。
 *
 * <p>IP 规则沿用精确 IP / CIDR / 通配符三种匹配；玩家名规则支持精确、前缀、后缀、
 * 包含、glob 与正则。运行时规则持久化到 {@code access_rules.yml}。</p>
 */
public final class AccessRuleService {

    private static final String CONFIG_NAME = "access_rules";
    private static final String IP_PATH = "ip_blacklist";
    private static final String PLAYER_NAME_PATH = "player_name_rules";

    private final ConfigService configService;
    private final java.util.logging.Logger logger;
    private volatile List<String> ipPatterns = List.of();
    private volatile List<PlayerNameRule> playerNameRules = List.of();

    public AccessRuleService(ConfigService configService) {
        this(configService, java.util.logging.Logger.getLogger("OrzMC"));
    }

    public AccessRuleService(ConfigService configService, java.util.logging.Logger logger) {
        this.configService = configService;
        this.logger = logger;
        reload();
    }

    // ---- IP rules ----

    public boolean isIpBlocked(String ip) {
        return matchedIpPattern(ip) != null;
    }

    /**
     * 返回第一个命中的 IP 规则；未命中返回 {@code null}。
     */
    public String matchedIpPattern(String ip) {
        if (ip == null || ip.isEmpty()) return null;
        for (String pattern : ipPatterns) {
            if (matches(ip, pattern)) return pattern;
        }
        return null;
    }

    public synchronized void addIpPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return;
        for (String existing : ipPatterns) {
            if (existing.equals(pattern)) return;
        }
        List<String> updated = new ArrayList<>(ipPatterns);
        updated.add(pattern);
        this.ipPatterns = Collections.unmodifiableList(updated);
        persist();
    }

    public synchronized void removeIpPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return;
        List<String> updated = new ArrayList<>(ipPatterns);
        if (updated.remove(pattern)) {
            this.ipPatterns = Collections.unmodifiableList(updated);
            persist();
        }
    }

    public List<String> getIpPatterns() {
        return ipPatterns;
    }

    // ---- player name rules ----

    public boolean isPlayerNameBlocked(String name) {
        return matchedPlayerNameRule(name) != null;
    }

    /** 返回第一个命中的玩家名规则；未命中返回 {@code null}。 */
    public PlayerNameRule matchedPlayerNameRule(String name) {
        if (name == null || name.isEmpty()) return null;
        for (PlayerNameRule rule : playerNameRules) {
            if (rule.matches(name)) return rule;
        }
        return null;
    }

    public synchronized void addPlayerNameRule(PlayerNameRule.MatchType type, String value) {
        if (type == null || value == null || value.isEmpty()) return;
        PlayerNameRule rule = PlayerNameRule.of(type, value);
        if (!rule.isValid()) return;
        if (containsRule(playerNameRules, rule)) return;
        List<PlayerNameRule> updated = new ArrayList<>(playerNameRules);
        updated.add(rule);
        this.playerNameRules = Collections.unmodifiableList(updated);
        persist();
    }

    public synchronized void removePlayerNameRule(PlayerNameRule.MatchType type, String value) {
        if (type == null || value == null || value.isEmpty()) return;
        PlayerNameRule target = PlayerNameRule.of(type, value);
        List<PlayerNameRule> updated = new ArrayList<>(playerNameRules);
        if (updated.removeIf(existing -> sameRule(existing, target))) {
            this.playerNameRules = Collections.unmodifiableList(updated);
            persist();
        }
    }

    public List<PlayerNameRule> getPlayerNameRules() {
        return playerNameRules;
    }

    private static boolean containsRule(List<PlayerNameRule> rules, PlayerNameRule target) {
        for (PlayerNameRule existing : rules) {
            if (sameRule(existing, target)) return true;
        }
        return false;
    }

    private static boolean sameRule(PlayerNameRule left, PlayerNameRule right) {
        return left.type() == right.type() && left.value().equalsIgnoreCase(right.value());
    }

    // ---- persistence ----

    /**
     * 从磁盘重载规则。synchronized 与 add/remove 互斥，避免「重载读旧快照」在变更间隙
     * 覆盖刚提交的内存规则（并发丢失更新）。
     */
    public synchronized void reload() {
        this.ipPatterns = loadIpPatterns();
        this.playerNameRules = loadPlayerNameRules();
    }

    private List<String> loadIpPatterns() {
        FileConfiguration cfg = configService.getConfig(CONFIG_NAME);
        if (cfg == null) return List.of();
        return Collections.unmodifiableList(cfg.getStringList(IP_PATH));
    }

    private List<PlayerNameRule> loadPlayerNameRules() {
        FileConfiguration cfg = configService.getConfig(CONFIG_NAME);
        if (cfg == null) return List.of();
        List<?> raw = cfg.getList(PLAYER_NAME_PATH);
        if (raw == null) return List.of();
        List<PlayerNameRule> rules = new ArrayList<>();
        for (Object item : raw) {
            String type = null;
            String value = null;
            if (item instanceof Map<?, ?> map) {
                type = stringValue(map.get("type"));
                value = stringValue(map.get("value"));
            } else if (item instanceof ConfigurationSection section) {
                type = section.getString("type");
                value = section.getString("value");
            } else if (item instanceof String text) {
                int colon = text.indexOf(':');
                if (colon > 0) {
                    type = text.substring(0, colon);
                    value = text.substring(colon + 1);
                }
            }
            PlayerNameRule.MatchType matchType = PlayerNameRule.MatchType.from(type);
            if (matchType != null && value != null && !value.isBlank()) {
                PlayerNameRule rule = PlayerNameRule.of(matchType, value);
                if (rule.isValid()) rules.add(rule);
            }
        }
        return Collections.unmodifiableList(rules);
    }

    /**
     * 原子落盘：经 {@link ConfigService#updateConfig} 在同步块内完成 set→save。
     *
     * <p>若先 {@code getConfig} 拿实例、在 get/set 间隙被 {@code reloadConfig} 替换实例，
     * set 会写进已废弃对象而丢失——因此 set+save 必须整体放入 updateConfig 的同步块。
     * 本方法自身也 synchronized，保证对同一 {@code access_rules} 的多次变更串行化。</p>
     */
    private synchronized void persist() {
        boolean saved = configService.updateConfig(CONFIG_NAME, cfg -> {
            cfg.set(IP_PATH, new ArrayList<>(ipPatterns));
            List<Map<String, String>> serialized = new ArrayList<>();
            for (PlayerNameRule rule : playerNameRules) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("type", rule.type().display());
                entry.put("value", rule.value());
                serialized.add(entry);
            }
            cfg.set(PLAYER_NAME_PATH, serialized);
        });
        if (!saved) {
            // 内存规则已生效但未落盘：下次 reload 会静默消失，须显式告警
            logger.warning("访问规则落盘失败：access_rules 配置未注册或写入失败，规则仅存于内存");
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    // ---- IP matching ----

    private static boolean matches(String ip, String pattern) {
        if (pattern.contains("/")) return cidrMatches(ip, pattern);
        if (pattern.contains("*")) return wildcardMatches(ip, pattern);
        return exactMatches(ip, pattern);
    }

    private static boolean exactMatches(String ip, String pattern) {
        if (ip.equals(pattern)) return true;
        if (ip.indexOf(':') < 0 && pattern.indexOf(':') < 0) return false;
        try {
            return Arrays.equals(
                    InetAddress.getByName(ip).getAddress(),
                    InetAddress.getByName(pattern).getAddress());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean cidrMatches(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/", 2);
            if (parts.length != 2) return false;
            String subnetStr = parts[0];
            int prefix;
            try {
                prefix = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }

            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            byte[] subnetBytes = InetAddress.getByName(subnetStr).getAddress();
            if (ipBytes.length != subnetBytes.length) return false;
            if (prefix < 0 || prefix > ipBytes.length * 8) return false;

            return matchesPrefix(ipBytes, subnetBytes, prefix);
        } catch (Exception e) {
            return false;
        }
    }

    /** 逐字节比较前 {@code prefix} 位（IPv4 最多 32 位，IPv6 最多 128 位）。 */
    private static boolean matchesPrefix(byte[] ip, byte[] subnet, int prefix) {
        int fullBytes = prefix / 8;
        int remBits = prefix % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (ip[i] != subnet[i]) return false;
        }
        if (remBits > 0) {
            int mask = 0xFF << (8 - remBits);
            if ((ip[fullBytes] & mask) != (subnet[fullBytes] & mask)) return false;
        }
        return true;
    }

    private static boolean wildcardMatches(String ip, String pattern) {
        // IPv4 专用通配：* 匹配 1 个或多个剩余网段。IPv6 请使用 CIDR。
        // 严格校验：IP 必须是恰好 4 段、每段 0-255 的合法 IPv4，
        // 拒绝非法 octet（如 10.999.999）与超 4 段的畸形地址（如 10.1.2.3.4）。
        String[] ipOctets = ip.split("\\.", -1);
        if (ipOctets.length != 4) return false;
        for (String octet : ipOctets) {
            if (!isValidIpv4Octet(octet)) return false;
        }
        String[] segments = pattern.split("\\.", -1);
        for (String segment : segments) {
            if (segment.isEmpty()) return false; // 拒绝 "10." / "10..*" 等畸形模式
        }
        return matchWildcardSegments(segments, 0, ipOctets, 0);
    }

    /** 逐段匹配：{@code *} 匹配 1 个或多个剩余网段，字面段与 IP 段严格相等。 */
    private static boolean matchWildcardSegments(String[] segments, int si, String[] ipOctets, int oi) {
        if (si == segments.length) return oi == ipOctets.length;
        if (oi >= ipOctets.length) return false;
        String segment = segments[si];
        if ("*".equals(segment)) {
            for (int take = 1; oi + take <= ipOctets.length; take++) {
                if (matchWildcardSegments(segments, si + 1, ipOctets, oi + take)) return true;
            }
            return false;
        }
        return segment.equals(ipOctets[oi]) && matchWildcardSegments(segments, si + 1, ipOctets, oi + 1);
    }

    /** 单段合法性：1-3 位十进制数，值域 0-255。 */
    private static boolean isValidIpv4Octet(String octet) {
        if (octet.isEmpty() || octet.length() > 3) return false;
        for (int i = 0; i < octet.length(); i++) {
            if (!Character.isDigit(octet.charAt(i))) return false;
        }
        try {
            return Integer.parseInt(octet) <= 255;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
