package com.jokerhub.paper.plugin.orzmc.commands;

public enum OrzUserCmd {
    SHOW_PLAYERS("/l", "查看当前在线玩家"),
    SHOW_WHITELIST("/w", "查看当前在白名单中的玩家"),
    SHOW_HELP("/h", "查看QQ群中可以使用的命令信息"),
    ADD_PLAYER_TO_WHITELIST("/a", "添加玩家到服务器白名单中"),
    REMOVE_PLAYER_FROM_WHITELIST("/r", "从服务器白名单中移除玩家");

    private final String cmdName;
    private final String description;

    OrzUserCmd(String cmdName, String description) {
        this.cmdName = cmdName;
        this.description = description;
    }

    public static String helpInfo() {
        return "👨‍💼 管理员命令：\n" +
                OrzUserCmd.ADD_PLAYER_TO_WHITELIST + "\n" +
                OrzUserCmd.REMOVE_PLAYER_FROM_WHITELIST + "\n" +
                "👨🏻‍💻 通用命令: \n" +
                OrzUserCmd.SHOW_PLAYERS + "\n" +
                OrzUserCmd.SHOW_WHITELIST + "\n" +
                OrzUserCmd.SHOW_HELP;
    }

    public String getCmdName() {
        return this.cmdName;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return this.cmdName + "\t" + this.description;
    }
}
