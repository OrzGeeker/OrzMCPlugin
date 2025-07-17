package com.jokerhub.paper.plugin.orzmc.commands;

public enum OrzUserCmd {
    SHOW_PLAYERS("/l", "查看当前在线玩家", false), SHOW_WHITELIST("/w", "查看当前在白名单中的玩家", false), SHOW_HELP("/h", "查看QQ群中可以使用的命令信息", false), ADD_PLAYER_TO_WHITELIST("/a", "添加玩家到服务器白名单中", true), REMOVE_PLAYER_FROM_WHITELIST("/r", "从服务器白名单中移除玩家", true);

    private final String cmdName;
    private final String description;
    private final boolean needAdminPermission;

    OrzUserCmd(String cmdName, String description, boolean needAdminPermission) {
        this.cmdName = cmdName;
        this.description = description;
        this.needAdminPermission = needAdminPermission;
    }

    public static String helpInfo() {
        return "👨‍💼 管理员命令：\n" + OrzUserCmd.ADD_PLAYER_TO_WHITELIST + "\n" + OrzUserCmd.REMOVE_PLAYER_FROM_WHITELIST + "\n" + "👨🏻‍💻 通用命令: \n" + OrzUserCmd.SHOW_PLAYERS + "\n" + OrzUserCmd.SHOW_WHITELIST + "\n" + OrzUserCmd.SHOW_HELP;
    }

    public String getCmdName() {
        return this.cmdName;
    }

    @Override
    public String toString() {
        return this.cmdName + "\t" + this.description;
    }

    public String adminPermissionRequiredTip() {
        if (this.needAdminPermission) {
            return this.cmdName + "命令需要群管理员权限";
        } else {
            return "";
        }
    }

    public String usageTip() {
        return switch (this) {
            case OrzUserCmd.ADD_PLAYER_TO_WHITELIST, OrzUserCmd.REMOVE_PLAYER_FROM_WHITELIST ->
                    "用法：\n" + this.cmdName + " " + "[玩家]\n" + this.cmdName + " " + "[玩家1] [玩家2] [玩家3]\n" + this.cmdName + " " + "[玩家1],[玩家2],[玩家3]\n";
            default -> "";
        };
    }
}
