package com.jokerhub.paper.plugin.orzmc.infra.i18n;

/**
 * 消息 key 常量（镜像 {@code TemplateKeys} 做法，禁散落字面量）。
 *
 * <p>值必须与语言包（messages/messages_zh-CN.yml 为完整性基线）中的真实 key 一致；
 * 跨语言 key 集/占位符一致性由 {@link I18nHealth} 与一致性单测守护。</p>
 */
public final class MessageKeys {

    private MessageKeys() {}

    // ---- common.*：跨渠道通用反馈（游戏内命令拦截器/通用提示） ----
    public static final String COMMON_COOLDOWN = "common.cooldown";
    public static final String COMMON_ADMIN_REQUIRED = "common.admin_required";
    public static final String COMMON_PLAYER_REQUIRED = "common.player_required";
    public static final String COMMON_PRISON_DENIED = "common.prison_denied";

    // ---- teleport.bow.*：传送弓（P2a） ----
    public static final String TELEPORT_BOW_NAME = "teleport.bow.name";
    public static final String TELEPORT_BOW_TAG = "teleport.bow.tag";
    public static final String TELEPORT_BOW_LORE = "teleport.bow.lore";
    public static final String TELEPORT_BOW_GIVEN = "teleport.bow.given";
    public static final String TELEPORT_BOW_DISABLED = "teleport.bow.disabled";
    public static final String TELEPORT_BOW_HIT_WATER = "teleport.bow.hit_water";
    public static final String TELEPORT_BOW_HIT_LAVA = "teleport.bow.hit_lava";
    public static final String TELEPORT_BOW_CROSS_WORLD = "teleport.bow.cross_world";
    public static final String TELEPORT_BOW_BAD_HEIGHT = "teleport.bow.bad_height";
    public static final String TELEPORT_BOW_NO_LANDING = "teleport.bow.no_landing";
    public static final String TELEPORT_BOW_DONE = "teleport.bow.done";

    // ---- whitelist.*：登录踢出提示/群通知（P2b） ----
    public static final String WHITELIST_KICK_JOIN_HINT_PREFIX = "whitelist.kick.join_hint_prefix";
    public static final String WHITELIST_KICK_JOIN_HINT_SUFFIX = "whitelist.kick.join_hint_suffix";
    public static final String WHITELIST_KICK_DISCORD_JOIN = "whitelist.kick.discord_join";
    public static final String WHITELIST_NOTIFY_BLOCKED = "whitelist.notify.blocked";
    public static final String WHITELIST_NOTIFY_TOGGLE_OFF = "whitelist.notify.toggle_off";

    // ---- portal.*：/portal 命令反馈（P2c） ----
    public static final String PORTAL_USAGE = "portal.usage";
    public static final String PORTAL_USAGE_REMOVE = "portal.usage_remove";
    public static final String PORTAL_PORT_REQUIRED = "portal.port_required";
    public static final String PORTAL_NOT_FOUND = "portal.not_found";
    public static final String PORTAL_REMOVED = "portal.removed";
    public static final String PORTAL_CREATED = "portal.created";
    public static final String PORTAL_COMMAND_DESC = "portal.command_desc";

    // ---- tnt.*：TNT 防护提示/告警标签（P2c） ----
    public static final String TNT_ANCHOR_DISABLED = "tnt.anchor_disabled";
    public static final String TNT_PLACE_COOLDOWN = "tnt.place_cooldown";
    public static final String TNT_COOLDOWN_SECONDS = "tnt.cooldown_seconds";
    public static final String TNT_PLACE_DISABLED = "tnt.place_disabled";
    public static final String TNT_PRIME_DENIED = "tnt.prime_denied";
    public static final String TNT_PRIME = "tnt.prime";
    public static final String TNT_DISPENSE_FORBIDDEN = "tnt.dispense_forbidden";
    public static final String TNT_EXPLODE = "tnt.explode";
    public static final String TNT_BLOCK_EXPLODE = "tnt.block_explode";
    public static final String TNT_PLACEMENT_MSG = "tnt.placement_msg";
    public static final String TNT_AT = "tnt.at";
    public static final String TNT_PLACED = "tnt.placed";

    // ---- player.notify.*：上下线聚合摘要版块标签（P2d） ----
    public static final String PLAYER_NOTIFY_JOIN_LABEL = "player.notify.join_label";
    public static final String PLAYER_NOTIFY_QUIT_LABEL = "player.notify.quit_label";
    public static final String PLAYER_NOTIFY_KICK_LABEL = "player.notify.kick_label";
    public static final String PLAYER_NOTIFY_HIDDEN_MORE = "player.notify.hidden_more";

    // ---- geoip.*：地区白名单提示/告警（P2d） ----
    public static final String GEOIP_KICK_REGION = "geoip.kick_region";
    public static final String GEOIP_KICK_UNVERIFIABLE = "geoip.kick_unverifiable";
    public static final String GEOIP_OUTCOME_ALLOWED_FAILOPEN = "geoip.outcome_allowed_failopen";
    public static final String GEOIP_OUTCOME_DENIED_FAILCLOSE = "geoip.outcome_denied_failclose";
    public static final String GEOIP_OUTCOME_ALLOWED = "geoip.outcome_allowed";
    public static final String GEOIP_OUTCOME_DENIED = "geoip.outcome_denied";
    public static final String GEOIP_ALERT_EXCEPTION = "geoip.alert_exception";
    public static final String GEOIP_ALERT_LOOKUP_FAILED = "geoip.alert_lookup_failed";
    public static final String GEOIP_ALERT_TIMEOUT = "geoip.alert_timeout";

    // ---- login.*：登录访问控制（P2d2；pre-login 无客户端 locale → 默认语言） ----
    public static final String LOGIN_UNKNOWN_PLAYER = "login.unknown_player";
    public static final String LOGIN_IP_BANNED = "login.ip_banned";
    public static final String LOGIN_NAME_RULE_DENIED = "login.name_rule_denied";
    public static final String LOGIN_ALERT_IP_BLOCK = "login.alert_ip_block";
    public static final String LOGIN_ALERT_NAME_BLOCK = "login.alert_name_block";

    // ---- guard.*：危险命令拦截（P2e；安全提示默认语言 R1） ----
    public static final String GUARD_DENY_REASON = "guard.deny_reason";
    public static final String GUARD_SELECTOR_WARN = "guard.selector_warn";
    public static final String GUARD_SOURCE_PLAYER = "guard.source_player";
    public static final String GUARD_SOURCE_CONSOLE = "guard.source_console";
    public static final String GUARD_ALERT_BLOCKED = "guard.alert_blocked";

    // ---- exploit.*：漏洞利用加固（P2e） ----
    public static final String EXPLOIT_NOTIFY_PAGES = "exploit.notify_pages";
    public static final String EXPLOIT_NOTIFY_ATTRIBUTES = "exploit.notify_attributes";
    public static final String EXPLOIT_ALERT_BLOCKED = "exploit.alert_blocked";

    // ---- ratelimit.*：登录限流（P2e） ----
    public static final String RATELIMIT_REASON_FREQUENCY = "ratelimit.reason_frequency";
    public static final String RATELIMIT_REASON_CONCURRENT = "ratelimit.reason_concurrent";
    public static final String RATELIMIT_ALERT_BLOCKED = "ratelimit.alert_blocked";
}
