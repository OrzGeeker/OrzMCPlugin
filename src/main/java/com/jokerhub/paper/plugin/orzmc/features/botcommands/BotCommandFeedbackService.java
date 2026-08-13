package com.jokerhub.paper.plugin.orzmc.features.botcommands;

/**
 * 群指令帮助信息生成（统一模板：标题行 + 用法行 + 示例块）。
 *
 * <p>所有 {@link OrzUserCmd} 都必须有 usageTip —— {@code $cmd ?} 一律展示用法，
 * 不再有「无帮助降级为执行」的路径（防 $b ? / $o ? 误触发重量级操作）。
 */
public final class BotCommandFeedbackService {

    /** 帮助文本中的分隔线（等宽字符，群消息代码块内渲染稳定）。 */
    private static final String DIVIDER = "━━━━━━━━━━━━━━━━";

    public String helpInfo(String promptChar) {
        return "🤖 OrzMC 群指令帮助\n"
                + "发送「"
                + promptChar
                + "x ?」可查看单个指令的用法示例\n"
                + DIVIDER
                + "\n"
                + "【管理员指令】\n"
                + OrzUserCmd.ADD_PLAYER_TO_WHITELIST.display(promptChar)
                + "\n"
                + OrzUserCmd.REMOVE_PLAYER_FROM_WHITELIST.display(promptChar)
                + "\n"
                + OrzUserCmd.BLACKLIST.display(promptChar)
                + "\n"
                + OrzUserCmd.BACKUP.display(promptChar)
                + "\n"
                + OrzUserCmd.OPTIMIZE_WORLD.display(promptChar)
                + "\n"
                + OrzUserCmd.EXECUTE_CONSOLE_COMMAND.display(promptChar)
                + "\n"
                + OrzUserCmd.REVIEW.display(promptChar)
                + "\n"
                + OrzUserCmd.PERMISSION.display(promptChar)
                + "\n"
                + DIVIDER
                + "\n"
                + "【通用指令】\n"
                + OrzUserCmd.SHOW_PLAYERS.display(promptChar)
                + "\n"
                + OrzUserCmd.SHOW_WHITELIST.display(promptChar)
                + "\n"
                + OrzUserCmd.SHOW_HELP.display(promptChar);
    }

    public String adminRequiredTip(OrzUserCmd cmd, String promptChar) {
        if (cmd.needAdminPermission()) {
            return promptChar + cmd.cmdName() + " 需要管理员权限";
        }
        return "";
    }

    /**
     * 单命令帮助：标题 + 用法 + 示例。全部 11 个命令均有定义，
     * default 兜底返回基础用法，保证 {@code $cmd ?} 永不降级执行。
     */
    public String usageTip(OrzUserCmd cmd, String promptChar) {
        return switch (cmd) {
            case ADD_PLAYER_TO_WHITELIST, REMOVE_PLAYER_FROM_WHITELIST ->
                "【"
                        + promptChar
                        + cmd.cmdName()
                        + " "
                        + (cmd == OrzUserCmd.ADD_PLAYER_TO_WHITELIST ? "添加玩家到白名单" : "移除白名单玩家")
                        + "】\n"
                        + "用法："
                        + promptChar
                        + cmd.cmdName()
                        + " [玩家] [玩家2] ...（空格或逗号分隔，可批量）\n"
                        + "示例：\n"
                        + promptChar
                        + cmd.cmdName()
                        + " Steve\n"
                        + promptChar
                        + cmd.cmdName()
                        + " Steve Alex Bob\n"
                        + promptChar
                        + cmd.cmdName()
                        + " Steve,Alex,Bob";
            case EXECUTE_CONSOLE_COMMAND ->
                "【"
                        + promptChar
                        + cmd.cmdName()
                        + " 执行控制台命令】\n"
                        + "用法："
                        + promptChar
                        + cmd.cmdName()
                        + " [控制台命令]\n"
                        + "示例：\n"
                        + promptChar
                        + cmd.cmdName()
                        + " plugins\n"
                        + promptChar
                        + cmd.cmdName()
                        + " say 大家好";
            case BLACKLIST ->
                "【"
                        + promptChar
                        + cmd.cmdName()
                        + " IP黑名单管理】\n"
                        + "用法："
                        + promptChar
                        + cmd.cmdName()
                        + "         查看黑名单\n"
                        + "      "
                        + promptChar
                        + cmd.cmdName()
                        + " [IP]    添加黑名单\n"
                        + "      "
                        + promptChar
                        + cmd.cmdName()
                        + " -[IP]   移除黑名单\n"
                        + "示例：\n"
                        + promptChar
                        + cmd.cmdName()
                        + "\n"
                        + promptChar
                        + cmd.cmdName()
                        + " 1.2.3.4\n"
                        + promptChar
                        + cmd.cmdName()
                        + " -1.2.3.4";
            case REVIEW ->
                "【"
                        + promptChar
                        + cmd.cmdName()
                        + " 审核申请】\n"
                        + "用法："
                        + promptChar
                        + cmd.cmdName()
                        + " l         查看待审列表\n"
                        + "      "
                        + promptChar
                        + cmd.cmdName()
                        + " y [玩家]  通过申请\n"
                        + "      "
                        + promptChar
                        + cmd.cmdName()
                        + " n [玩家]  拒绝申请\n"
                        + "示例：\n"
                        + promptChar
                        + cmd.cmdName()
                        + " l\n"
                        + promptChar
                        + cmd.cmdName()
                        + " y Steve";
            case PERMISSION ->
                "【"
                        + promptChar
                        + cmd.cmdName()
                        + " 权限管理】\n"
                        + "用法："
                        + promptChar
                        + cmd.cmdName()
                        + " u [玩家]  权限升级\n"
                        + "      "
                        + promptChar
                        + cmd.cmdName()
                        + " d [玩家]  权限降级\n"
                        + "示例：\n"
                        + promptChar
                        + cmd.cmdName()
                        + " u Steve\n"
                        + promptChar
                        + cmd.cmdName()
                        + " d Steve";
            case SHOW_PLAYERS ->
                "【" + promptChar + cmd.cmdName() + " 查看在线玩家】\n" + "用法：" + promptChar + cmd.cmdName() + "（无参数，直接执行）";
            case SHOW_WHITELIST ->
                "【"
                        + promptChar
                        + cmd.cmdName()
                        + " 查看白名单玩家】\n"
                        + "用法："
                        + promptChar
                        + cmd.cmdName()
                        + " [页码]（可选，如 "
                        + promptChar
                        + cmd.cmdName()
                        + " 2）";
            case SHOW_HELP ->
                "【" + promptChar + cmd.cmdName() + " 查看帮助】\n" + "用法：" + promptChar + cmd.cmdName() + "（无参数，直接显示本帮助）";
            case BACKUP ->
                "【" + promptChar + cmd.cmdName() + " 地图备份】\n" + "用法：" + promptChar + cmd.cmdName() + "（无参数，直接执行备份）";
            case OPTIMIZE_WORLD ->
                "【" + promptChar + cmd.cmdName() + " 优化地图大小】\n" + "用法：" + promptChar + cmd.cmdName() + "（无参数，直接执行优化）";
        };
    }
}
