package com.jokerhub.paper.plugin.orzmc.features.security;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 玩家名匹配规则。
 *
 * <p>默认大小写不敏感。离线模式下玩家名由客户端上报，名称规则适合做风控/反滥用，
 * 不能替代 UUID 或 IP 作为强身份安全边界。</p>
 */
public final class PlayerNameRule {

    public enum MatchType {
        EXACT,
        PREFIX,
        SUFFIX,
        CONTAINS,
        GLOB,
        REGEX;

        public static MatchType from(String raw) {
            if (raw == null) return null;
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public String display() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final MatchType type;
    private final String value;
    private final Pattern pattern;

    private PlayerNameRule(MatchType type, String value) {
        this.type = type;
        this.value = value;
        this.pattern = compilePattern(type, value);
    }

    public static PlayerNameRule of(MatchType type, String value) {
        return new PlayerNameRule(type, value);
    }

    /**
     * 解析并校验玩家名规则参数（bot {@code $d} 与游戏内 {@code /blacklist} 共用，消除重复）。
     *
     * <p>{@code valid()} 为 true 时 {@code rule()} 非 null；{@code valid()} 为 false 时
     * 若 {@code type()} 为 null 表示匹配类型未知，否则表示正则非法。</p>
     */
    public static ParsedRule parse(String typeRaw, String value) {
        MatchType type = MatchType.from(typeRaw);
        if (type == null) {
            return new ParsedRule(null, null, false);
        }
        PlayerNameRule rule = of(type, value);
        return new ParsedRule(type, rule, rule.isValid());
    }

    /** 玩家名规则的解析结果：{@code valid()} 为 false 表示参数不合法（类型未知或正则非法）。 */
    public record ParsedRule(MatchType type, PlayerNameRule rule, boolean valid) {}

    public MatchType type() {
        return type;
    }

    public String value() {
        return value;
    }

    public boolean isValid() {
        return type != MatchType.REGEX || pattern != null;
    }

    public boolean matches(String name) {
        if (name == null || value == null || value.isEmpty()) return false;
        return switch (type) {
            case EXACT -> name.equalsIgnoreCase(value);
            case PREFIX -> name.regionMatches(true, 0, value, 0, value.length());
            case SUFFIX -> {
                int start = name.length() - value.length();
                yield start >= 0 && name.regionMatches(true, start, value, 0, value.length());
            }
            case CONTAINS -> containsIgnoreCase(name, value);
            case GLOB, REGEX -> pattern != null && pattern.matcher(name).matches();
        };
    }

    public String display() {
        return type.display() + ":" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerNameRule other)) return false;
        return type == other.type && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return display();
    }

    private static boolean containsIgnoreCase(String name, String needle) {
        int needleLength = needle.length();
        for (int i = 0; i <= name.length() - needleLength; i++) {
            if (name.regionMatches(true, i, needle, 0, needleLength)) return true;
        }
        return false;
    }

    private static Pattern compilePattern(MatchType type, String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String regex =
                    switch (type) {
                        case GLOB -> globToRegex(value);
                        case REGEX -> value;
                        default -> null;
                    };
            return regex == null ? null : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                default -> sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return sb.append('$').toString();
    }
}
