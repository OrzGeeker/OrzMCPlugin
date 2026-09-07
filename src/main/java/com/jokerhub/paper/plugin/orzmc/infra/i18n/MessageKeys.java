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
}
