package com.jokerhub.paper.plugin.orzmc.features.botcommands;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BotCommandFeedbackServiceTest {

    private BotCommandFeedbackService feedback;

    @BeforeEach
    void setUp() {
        feedback = new BotCommandFeedbackService();
    }

    @Test
    void helpInfo_containsAdminCommands() {
        String help = feedback.helpInfo("$");
        assertTrue(help.contains("$a"));
        assertTrue(help.contains("$r"));
        assertTrue(help.contains("$b"));
        assertTrue(help.contains("$o"));
    }

    @Test
    void helpInfo_containsUserCommands() {
        String help = feedback.helpInfo("$");
        assertTrue(help.contains("$l"));
        assertTrue(help.contains("$w"));
        assertTrue(help.contains("$h"));
    }

    @Test
    void helpInfo_usesCustomPromptChar() {
        String help = feedback.helpInfo("!");
        assertTrue(help.contains("!a"));
        assertTrue(help.contains("!l"));
    }

    @Test
    void helpInfo_sectionsUseConsistentColon() {
        String help = feedback.helpInfo("$");
        // 两个分节标题的冒号必须一致（全角），避免半角/全角混用
        assertTrue(help.contains("【管理员指令】"));
        assertTrue(help.contains("【通用指令】"));
        assertFalse(help.contains("通用指令:"));
    }

    @Test
    void adminRequiredTip_forAdminCmd() {
        String tip = feedback.adminRequiredTip(OrzUserCmd.BACKUP, "$");
        assertTrue(tip.contains("需要管理员权限"));
    }

    @Test
    void adminRequiredTip_forNonAdminCmd() {
        String tip = feedback.adminRequiredTip(OrzUserCmd.SHOW_PLAYERS, "$");
        assertEquals("", tip);
    }

    @Test
    void usageTip_forWhitelistCommands() {
        String tip = feedback.usageTip(OrzUserCmd.ADD_PLAYER_TO_WHITELIST, "$");
        assertTrue(tip.contains("$a"));
        assertTrue(tip.contains("[玩家]"));
    }

    @Test
    void usageTip_forConsoleCommand() {
        String tip = feedback.usageTip(OrzUserCmd.EXECUTE_CONSOLE_COMMAND, "$");
        assertTrue(tip.contains("$e"));
        assertTrue(tip.contains("[控制台命令]"));
    }

    @Test
    void usageTip_forEveryCommand_returnsNonEmptyUsage() {
        // 所有命令都必须有用法提示（$cmd ? 一律展示帮助，绝不降级执行）
        for (OrzUserCmd cmd : OrzUserCmd.values()) {
            String tip = feedback.usageTip(cmd, "$");
            assertNotNull(tip, cmd + " 的 usageTip 不应为 null");
            assertFalse(tip.isBlank(), cmd + " 的 usageTip 不应为空");
            assertTrue(tip.contains("用法："), cmd + " 的 usageTip 应包含「用法：」");
            assertTrue(tip.contains("【$" + cmd.cmdName()), cmd + " 的 usageTip 应包含标题行");
        }
    }

    @Test
    void usageTip_examplesSectionPresent() {
        // 带参数的命令应包含示例块；无参数命令至少说明无参数
        String whitelistTip = feedback.usageTip(OrzUserCmd.ADD_PLAYER_TO_WHITELIST, "$");
        assertTrue(whitelistTip.contains("示例："));
        assertTrue(whitelistTip.contains("$a Steve"));

        String blacklistTip = feedback.usageTip(OrzUserCmd.BLACKLIST, "$");
        assertTrue(blacklistTip.contains("示例："));
        assertTrue(blacklistTip.contains("$d -1.2.3.4"));

        String backupTip = feedback.usageTip(OrzUserCmd.BACKUP, "$");
        assertTrue(backupTip.contains("无参数"));
    }

    @Test
    void usageTip_usesCustomPromptChar() {
        String tip = feedback.usageTip(OrzUserCmd.PERMISSION, "!");
        assertTrue(tip.contains("!p u [玩家]"));
    }
}
