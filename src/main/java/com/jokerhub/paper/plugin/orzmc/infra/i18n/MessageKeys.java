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
}
