package com.jokerhub.paper.plugin.orzmc.features.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.infra.config.ConfigService;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccessRuleServiceTest {

    private ConfigService configService;
    private FileConfiguration fileConfig;
    private AccessRuleService service;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        fileConfig = mock(FileConfiguration.class);

        when(configService.getConfig("access_rules")).thenReturn(fileConfig);
        when(fileConfig.getStringList("ip_blacklist")).thenReturn(List.of());
        doReturn(List.of()).when(fileConfig).getList("player_name_rules");

        service = new AccessRuleService(configService);
    }

    // ---- IP exact matching ----

    @Test
    void ipExactMatch_blocksExactIp() {
        setupIpPatterns("192.168.1.1");
        assertTrue(service.isIpBlocked("192.168.1.1"));
    }

    @Test
    void ipExactMatch_allowsDifferentIp() {
        setupIpPatterns("192.168.1.1");
        assertFalse(service.isIpBlocked("192.168.1.2"));
    }

    // ---- IP CIDR matching ----

    @ParameterizedTest
    @CsvSource({
        "10.0.0.0/8,     10.0.0.1,      true",
        "10.0.0.0/8,     10.255.255.255, true",
        "10.0.0.0/8,     11.0.0.1,      false",
        "192.168.1.0/24, 192.168.1.100, true",
        "192.168.1.0/24, 192.168.2.1,   false",
        "0.0.0.0/0,      1.2.3.4,        true",
        "203.0.113.0/24, 203.0.113.1,   true",
    })
    void ipCidrMatch(String pattern, String ip, boolean expected) {
        setupIpPatterns(pattern);
        assertEquals(expected, service.isIpBlocked(ip));
    }

    // ---- IP wildcard matching ----

    @ParameterizedTest
    @CsvSource({
        "192.168.1.*,   192.168.1.100,   true",
        "192.168.1.*,   192.168.2.100,   false",
        "10.*,          10.0.0.1,        true",
        "10.*,          10.255.255.255,  true",
        "10.*,          11.0.0.1,        false",
        "203.0.113.*,   203.0.113.55,    true",
        "203.0.113.*,   203.0.114.55,    false",
    })
    void ipWildcardMatch(String pattern, String ip, boolean expected) {
        setupIpPatterns(pattern);
        assertEquals(expected, service.isIpBlocked(ip));
    }

    // ---- IP wildcard strictness ----

    @Test
    void ipWildcardMatch_rejectsInvalidOctet() {
        setupIpPatterns("10.*");
        assertFalse(service.isIpBlocked("10.999.999")); // 非法 octet > 255
        assertFalse(service.isIpBlocked("10.256.0.1"));
        assertFalse(service.isIpBlocked("10.0.0.abc")); // 非数字段
        assertFalse(service.isIpBlocked("10.1.2.3.4")); // 超 4 段
    }

    @Test
    void ipWildcardMatch_middleWildcard() {
        setupIpPatterns("10.*.1");
        assertTrue(service.isIpBlocked("10.5.6.1"));
        assertFalse(service.isIpBlocked("10.5.1.2"));
    }

    // ---- IPv6 ----

    @Test
    void ipExactMatch_ipv6_canonicalFormsEqual() {
        setupIpPatterns("2001:db8::1");
        assertTrue(service.isIpBlocked("2001:db8::1"));
        assertTrue(service.isIpBlocked("2001:0db8:0:0:0:0:0:1"));
    }

    @ParameterizedTest
    @CsvSource({
        "2001:db8::/32,     2001:db8:1::5,   true",
        "2001:db8::/32,     2001:db9::1,     false",
        "fd00::/8,          fd12:3456::1,    true",
        "fd00::/8,          2001:db8::1,     false",
        "::/0,              ::1,             true",
        "::1/128,           ::1,             true",
        "::1/128,           ::2,             false",
    })
    void ipCidrMatch_ipv6(String pattern, String ip, boolean expected) {
        setupIpPatterns(pattern);
        assertEquals(expected, service.isIpBlocked(ip));
    }

    @Test
    void ipCidrMatch_v4PatternVsV6Ip_notBlocked() {
        setupIpPatterns("10.0.0.0/8");
        assertFalse(service.isIpBlocked("2001:db8::1"));
    }

    @Test
    void ipCidrMatch_v6PatternVsV4Ip_notBlocked() {
        setupIpPatterns("2001:db8::/32");
        assertFalse(service.isIpBlocked("10.0.0.1"));
    }

    // ---- IP matched pattern ----

    @Test
    void matchedIpPattern_returnsHitPattern() {
        setupIpPatterns("1.2.3.4", "10.0.0.0/8", "2001:db8::/32");
        assertEquals("10.0.0.0/8", service.matchedIpPattern("10.1.2.3"));
        assertEquals("2001:db8::/32", service.matchedIpPattern("2001:db8:abcd::5"));
        assertEquals("1.2.3.4", service.matchedIpPattern("1.2.3.4"));
        assertNull(service.matchedIpPattern("5.6.7.8"));
    }

    // ---- IP add/remove ----

    @Test
    void addIpPattern_increasesBlocked() {
        assertFalse(service.isIpBlocked("10.0.0.5"));
        service.addIpPattern("10.0.0.0/8");
        assertTrue(service.isIpBlocked("10.0.0.5"));
        service.addIpPattern("10.0.0.0/8");
        assertEquals(1, service.getIpPatterns().size());
    }

    @Test
    void removeIpPattern_clearsBlocked() {
        setupIpPatterns("10.0.0.0/8");
        service.removeIpPattern("10.0.0.0/8");
        assertFalse(service.isIpBlocked("10.0.0.5"));
        service.removeIpPattern("nonexistent");
        assertTrue(service.getIpPatterns().isEmpty());
    }

    @Test
    void addNullIpPattern_noChange() {
        service.addIpPattern(null);
        service.addIpPattern("");
        assertTrue(service.getIpPatterns().isEmpty());
    }

    @Test
    void addIpPattern_persists() {
        when(fileConfig.get("ip_blacklist")).thenReturn(null);
        service.addIpPattern("1.2.3.4");
        verify(fileConfig).set(eq("ip_blacklist"), anyList());
        verify(configService).saveConfig("access_rules");
    }

    // ---- player name rules ----

    @Test
    void playerNameRule_exactMatchesIgnoringCase() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.EXACT, "Steve");
        assertTrue(service.isPlayerNameBlocked("steve"));
        assertFalse(service.isPlayerNameBlocked("steve2"));
    }

    @Test
    void playerNameRule_prefixSuffixContains() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.PREFIX, "bot_");
        service.addPlayerNameRule(PlayerNameRule.MatchType.SUFFIX, "_test");
        service.addPlayerNameRule(PlayerNameRule.MatchType.CONTAINS, "admin");
        assertTrue(service.isPlayerNameBlocked("Bot_Alice"));
        assertTrue(service.isPlayerNameBlocked("Alice_Test"));
        assertTrue(service.isPlayerNameBlocked("the_admin_x"));
        assertFalse(service.isPlayerNameBlocked("Alice"));
    }

    @Test
    void playerNameRule_globMatchesWildcards() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.GLOB, "Steve*");
        assertTrue(service.isPlayerNameBlocked("steve2026"));
        assertFalse(service.isPlayerNameBlocked("alice"));
    }

    @Test
    void playerNameRule_regexMatchesIgnoringCase() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.REGEX, "^bot\\d+$");
        assertTrue(service.isPlayerNameBlocked("BOT42"));
        assertFalse(service.isPlayerNameBlocked("botx"));
    }

    @Test
    void playerNameRule_invalidRegexNotAdded() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.REGEX, "[");
        assertTrue(service.getPlayerNameRules().isEmpty());
    }

    @Test
    void playerNameRule_matchedRule_returnsRule() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.PREFIX, "bot_");
        PlayerNameRule hit = service.matchedPlayerNameRule("Bot_Alice");
        assertNotNull(hit);
        assertEquals(PlayerNameRule.MatchType.PREFIX, hit.type());
        assertEquals("bot_", hit.value());
    }

    @Test
    void playerNameRule_duplicateAndRemove() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.PREFIX, "bot_");
        service.addPlayerNameRule(PlayerNameRule.MatchType.PREFIX, "BOT_");
        assertEquals(1, service.getPlayerNameRules().size());
        service.removePlayerNameRule(PlayerNameRule.MatchType.PREFIX, "BOT_");
        assertTrue(service.getPlayerNameRules().isEmpty());
    }

    @Test
    void playerNameRule_persists() {
        service.addPlayerNameRule(PlayerNameRule.MatchType.SUFFIX, "_alt");
        verify(fileConfig).set(eq("player_name_rules"), anyList());
        verify(configService).saveConfig("access_rules");
    }

    @Test
    void playerNameRules_reloadFromMaps() {
        doReturn(List.of(Map.of("type", "prefix", "value", "bot_")))
                .when(fileConfig)
                .getList("player_name_rules");
        service.reload();
        assertEquals(1, service.getPlayerNameRules().size());
        assertTrue(service.isPlayerNameBlocked("Bot_Alice"));
    }

    // ---- helpers ----

    private void setupIpPatterns(String... patterns) {
        when(fileConfig.getStringList("ip_blacklist")).thenReturn(List.of(patterns));
        service.reload();
    }
}
