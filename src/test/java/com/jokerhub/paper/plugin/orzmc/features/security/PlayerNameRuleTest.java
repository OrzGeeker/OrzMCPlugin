package com.jokerhub.paper.plugin.orzmc.features.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerNameRuleTest {

    @ParameterizedTest
    @CsvSource({
        "player, true", // 裸 player 关键词
        "player list, true",
        "Player exact foo, true", // 大小写不敏感
        "-player exact foo, true",
        "exact foo, true",
        "PREFIX bot_, true", // 匹配类型首词大小写不敏感
        "Regex .*, true",
        "contains a, true",
        "1.2.3.4, false", // 纯 IP
        "10.0.0.0/8, false", // CIDR
        "192.168.*.*, false", // 通配符
        "-1.2.3.4, false" // IP 移除简写（去 `-` 后首词非匹配类型）
    })
    void looksLikePlayerRuleSyntax(String raw, boolean expected) {
        assertEquals(expected, PlayerNameRule.looksLikePlayerRuleSyntax(raw), "raw=" + raw);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "1.2.3.4", "server.example.com"})
    void looksLikePlayerRuleSyntax_nonRuleInput_returnsFalse(String raw) {
        assertFalse(PlayerNameRule.looksLikePlayerRuleSyntax(raw));
    }

    @Test
    void looksLikePlayerRuleSyntax_null_returnsFalse() {
        assertFalse(PlayerNameRule.looksLikePlayerRuleSyntax(null));
    }

    @Test
    void parse_invalidType_invalid() {
        PlayerNameRule.ParsedRule parsed = PlayerNameRule.parse("bogus", "foo");
        assertFalse(parsed.valid());
        assertNull(parsed.type());
    }

    @Test
    void parse_invalidRegex_invalid() {
        PlayerNameRule.ParsedRule parsed = PlayerNameRule.parse("regex", "[");
        assertFalse(parsed.valid());
    }
}
