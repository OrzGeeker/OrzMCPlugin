package com.jokerhub.paper.plugin.orzmc.features.botcommands;

/**
 * 群指令枚举（i18n 后不再内嵌中文描述）。
 *
 * <p>展示文案（描述/用法/示例）全部来自语言包 {@code bot.*}（P3a），
 * 由 {@link BotCommandFeedbackService} 经 {@code cmdName}（字母）取 key 渲染。</p>
 */
public enum OrzUserCmd {
    SHOW_PLAYERS("l", false),
    SHOW_WHITELIST("w", false),
    SHOW_HELP("h", false),
    ADD_PLAYER_TO_WHITELIST("a", true),
    REMOVE_PLAYER_FROM_WHITELIST("r", true),
    BACKUP("b", true),
    OPTIMIZE_WORLD("o", true),
    EXECUTE_CONSOLE_COMMAND("e", true),
    BLACKLIST("d", true),
    REVIEW("v", true),
    PERMISSION("p", true);

    private final String cmdName;
    private final boolean needAdminPermission;

    OrzUserCmd(String cmdName, boolean needAdminPermission) {
        this.cmdName = cmdName;
        this.needAdminPermission = needAdminPermission;
    }

    public String cmdName() {
        return cmdName;
    }

    public boolean needAdminPermission() {
        return needAdminPermission;
    }
}
