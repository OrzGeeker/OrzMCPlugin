package com.jokerhub.paper.plugin.orzmc.features.botcommands;

import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.WorldMaintenanceService;
import com.jokerhub.paper.plugin.orzmc.features.security.BlacklistService;
import com.jokerhub.paper.plugin.orzmc.features.security.CommandAuditService;
import com.jokerhub.paper.plugin.orzmc.features.security.CommandGuardService;
import com.jokerhub.paper.plugin.orzmc.features.whitelist.WhitelistService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.BotConfig;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.MaintenanceConfig;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.WhitelistConfig;
import com.jokerhub.paper.plugin.orzmc.infra.logging.LogCaptureService;
import com.jokerhub.paper.plugin.orzmc.infra.paging.Paginator;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public final class BotCommandService extends BotCommandContext implements BotInboundHandler {
    private BotCommandListFeedbackService listFeedbackService;
    private final Map<OrzUserCmd, CmdHandler> handlers;
    private WorldMaintenanceService maintenanceService;
    private BlacklistService blacklistService;
    private com.jokerhub.paper.plugin.orzmc.features.review.ReviewService reviewService;
    private com.jokerhub.paper.plugin.orzmc.features.rank.RankService rankService;
    private LogCaptureService logCaptureService;
    /** 危险命令判定核心（安全加固 P0-5）：$e 控制台执行前过 guard。未注入时放行（向后兼容测试）。 */
    private CommandGuardService commandGuardService;
    /** 命令审计日志（安全加固 P0-4）：$e 路径记录 bot 来源审计。 */
    private CommandAuditService commandAuditService;
    /** $v 群审核命令处理器（Supplier 注入 reviewService/rankService，调用时读取最新值）。 */
    private final ReviewCommandHandler reviewCommandHandler;
    /** $p 群权限升降级命令处理器（Supplier 注入 rankService）。 */
    private final PermissionCommandHandler permissionCommandHandler;
    /** $e 控制台命令执行处理器（Supplier 注入 guard/audit/logCapture）。 */
    private final ConsoleCommandHandler consoleCommandHandler;

    @FunctionalInterface
    private interface CmdHandler {
        /** 5 参入口：cmd/admin/发送者身份/回调/原始参数。 */
        void handle(
                OrzUserCmd cmd, boolean isAdmin, String senderName, Consumer<MessageEnvelope> callback, String rawArgs);

        /** 4 参便捷入口（senderName=null），兼容测试与无身份调用。 */
        default void handle(OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
            handle(cmd, isAdmin, null, callback, rawArgs);
        }
    }

    public BotCommandService(ServerFacade server, TypedConfigProvider configs) {
        super(server, configs);
        this.listFeedbackService = new BotCommandListFeedbackService(server, configs);
        this.reviewCommandHandler = new ReviewCommandHandler(server, configs, () -> reviewService, () -> rankService);
        this.permissionCommandHandler = new PermissionCommandHandler(server, configs, () -> rankService);
        this.consoleCommandHandler = new ConsoleCommandHandler(
                server, configs, () -> commandGuardService, () -> commandAuditService, () -> logCaptureService);
        this.handlers = Map.ofEntries(
                Map.entry(OrzUserCmd.SHOW_PLAYERS, (c, a, s, cb, r) -> handleShowPlayers(c, a, cb, r)),
                Map.entry(OrzUserCmd.SHOW_WHITELIST, (c, a, s, cb, r) -> handleShowWhitelist(c, a, cb, r)),
                Map.entry(OrzUserCmd.SHOW_HELP, (c, a, s, cb, r) -> handleShowHelp(c, a, cb, r)),
                Map.entry(OrzUserCmd.ADD_PLAYER_TO_WHITELIST, (c, a, s, cb, r) -> handleAddWhitelist(c, a, cb, r)),
                Map.entry(
                        OrzUserCmd.REMOVE_PLAYER_FROM_WHITELIST,
                        (c, a, s, cb, r) -> handleRemoveWhitelist(c, a, cb, r)),
                Map.entry(OrzUserCmd.BACKUP, (c, a, s, cb, r) -> handleBackup(c, a, cb, r)),
                Map.entry(OrzUserCmd.OPTIMIZE_WORLD, (c, a, s, cb, r) -> handleOptimize(c, a, cb, r)),
                Map.entry(OrzUserCmd.BLACKLIST, (c, a, s, cb, r) -> handleBlacklist(c, a, cb, r)),
                Map.entry(OrzUserCmd.REVIEW, (c, a, s, cb, r) -> reviewCommandHandler.handle(c, a, s, cb, r)),
                Map.entry(OrzUserCmd.PERMISSION, (c, a, s, cb, r) -> permissionCommandHandler.handle(c, a, cb, r)),
                Map.entry(
                        OrzUserCmd.EXECUTE_CONSOLE_COMMAND,
                        (c, a, s, cb, r) -> consoleCommandHandler.handle(c, a, s, cb, r)));
    }

    public void setMaintenanceService(WorldMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    public void setBlacklistService(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    public void setReviewService(com.jokerhub.paper.plugin.orzmc.features.review.ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public void setRankService(com.jokerhub.paper.plugin.orzmc.features.rank.RankService rankService) {
        this.rankService = rankService;
        // 重建列表反馈服务以注入 rankService（在线列表显示权限组）
        com.jokerhub.paper.plugin.orzmc.infra.player.OnlineListFormatter formatter =
                new com.jokerhub.paper.plugin.orzmc.infra.player.OnlineListFormatter();
        formatter.setRankService(rankService);
        this.listFeedbackService = new BotCommandListFeedbackService(server, configs, formatter);
    }

    /** 注入日志窗口收集服务（$e 命令输出兜底）。未注入时 $e 退化为仅返回执行状态。 */
    public void setLogCaptureService(LogCaptureService logCaptureService) {
        this.logCaptureService = logCaptureService;
    }

    /** 注入危险命令 guard 与审计（安全加固 P0-5）。未注入时 $e 按原路径直接执行（测试向后兼容）。 */
    public void setCommandGuard(CommandGuardService commandGuardService, CommandAuditService commandAuditService) {
        this.commandGuardService = commandGuardService;
        this.commandAuditService = commandAuditService;
    }

    @Override
    public void handleMessage(String message, boolean isAdmin, Consumer<MessageEnvelope> callback) {
        parse(message, isAdmin, null, callback);
    }

    @Override
    public void handleMessage(String message, boolean isAdmin, String senderName, Consumer<MessageEnvelope> callback) {
        parse(message, isAdmin, senderName, callback);
    }

    public void parse(String message, Boolean isAdmin, Consumer<MessageEnvelope> callback) {
        parse(message, isAdmin, null, callback);
    }

    public void parse(String message, Boolean isAdmin, String senderName, Consumer<MessageEnvelope> callback) {
        BotConfig botConfig = botConfig();
        String promptChar = botConfig.cmdPromptChar();
        if (!message.startsWith(promptChar)) return;

        for (OrzUserCmd userCmd : OrzUserCmd.values()) {
            String cmdPrefix = promptChar + userCmd.cmdName();
            if (matchesCommandPrefix(message, cmdPrefix)) {
                // 全角空格（U+3000）归一化为半角再 trim——Java String.trim() 不处理 U+3000，
                // 否则 "$b　?"（全角空格分隔）会绕过 ? 拦截直接触发备份/优化等重量级命令
                String rawArgs =
                        extractArgs(message, cmdPrefix).replace('\u3000', ' ').trim();

                // $cmd ?：在此指令分发前统一拦截。
                // 前缀匹配（$b ?x / $b ?? / $b ? 2 均视为帮助请求），防误触重量级命令；
                // $e 特判精确匹配——控制台命令本身可能以 ? 开头（如 "$e ?list"）
                boolean helpQuery = userCmd == OrzUserCmd.EXECUTE_CONSOLE_COMMAND
                        ? rawArgs.equals("?") || rawArgs.equals("？")
                        : rawArgs.startsWith("?") || rawArgs.startsWith("？");
                if (helpQuery) {
                    String tip = feedbackService.usageTip(userCmd, promptChar);
                    if (!tip.isBlank()) {
                        emitUsage(callback, tip);
                        return;
                    }
                    // 防御：无 usageTip 定义时发总帮助，绝不降级为执行命令
                    // （避免 $b ? / $o ? 等误触发备份/优化等重量级操作）
                    emitHelp(callback);
                    return;
                }

                CmdHandler handler = handlers.get(userCmd);
                if (handler != null) {
                    handler.handle(userCmd, isAdmin, senderName, callback, rawArgs);
                } else {
                    emitHelp(callback);
                }
                return;
            }
        }

        // 无匹配指令
        emitHelp(callback);
    }

    private void emitHelp(Consumer<MessageEnvelope> callback) {
        String help = feedbackService.helpInfo(botConfig().cmdPromptChar());
        emit(callback, "command_help", Map.of("help", help), help);
    }

    private boolean matchesCommandPrefix(String message, String fullCmd) {
        return message.equals(fullCmd)
                || (message.startsWith(fullCmd)
                        && message.length() > fullCmd.length()
                        && Character.isWhitespace(message.charAt(fullCmd.length())));
    }

    private String extractArgs(String rawMessage, String prefix) {
        if (rawMessage.length() <= prefix.length()) return "";
        return rawMessage.substring(prefix.length()).trim();
    }

    // ---- Command handlers (all follow CmdHandler interface) ----

    private void handleShowPlayers(
            OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        server.runAsync(() -> {
            try {
                ArrayList<Player> onlinePlayers = listFeedbackService.currentOnlinePlayers();
                BotCommandListFeedbackService.OnlineList online = listFeedbackService.buildOnlineList(
                        onlinePlayers, server.server().getMaxPlayers());
                emit(callback, "command_players", listFeedbackService.onlineVars(online), online.fallback());
            } catch (Exception e) {
                server.logger().log(Level.SEVERE, "onlinePlayersInfo 异步任务异常", e);
            }
        });
    }

    private void handleShowWhitelist(
            OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        server.runAsync(() -> {
            try {
                WhitelistConfig whitelistConfig = configs.whitelist();
                WhitelistService svc = WhitelistService.defaultImpl(server.plugin());
                int delayTicks = Math.max(0, whitelistConfig.paginationDelayTicks());
                Integer page = parsePageArg(rawArgs);
                if (isAdmin) {
                    renderWhitelistWithCleanup(callback, page, delayTicks, svc, whitelistConfig);
                } else {
                    renderWhitelistPages(callback, page, delayTicks, svc);
                }
            } catch (Exception e) {
                server.logger().log(Level.SEVERE, "whiteListInfo 异步任务异常", e);
            }
        });
    }

    private void handleShowHelp(OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        String help = feedbackService.helpInfo(botConfig().cmdPromptChar());
        emit(callback, "command_help", Map.of("help", help), help);
    }

    private void handleAddWhitelist(
            OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        Set<String> userNames = parseArgs(rawArgs);
        if (!guardWhitelistCommand(cmd, isAdmin, userNames, callback)) return;
        server.runSync(() -> {
            WhitelistService svc = WhitelistService.defaultImpl(server.plugin());
            String message = svc.addPlayers(server.server(), userNames);
            emit(callback, "command_whitelist_add_result", Map.of("message", message), message);
        });
    }

    private void handleRemoveWhitelist(
            OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        Set<String> userNames = parseArgs(rawArgs);
        if (!guardWhitelistCommand(cmd, isAdmin, userNames, callback)) return;
        server.runSync(() -> {
            WhitelistService svc = WhitelistService.defaultImpl(server.plugin());
            String message = svc.removePlayers(server.server(), userNames);
            emit(callback, "command_whitelist_remove_result", Map.of("message", message), message);
        });
    }

    private void handleBackup(OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        if (!guardAdminCommand(cmd, isAdmin, callback)) return;
        MaintenanceConfig maintenance = configs.maintenance();
        long tickTimeThreshold = maintenance.optimizeTickTimeThreshold();
        int retain = maintenance.backupRetentionCount();
        if (maintenanceService != null) {
            maintenanceService.backup(
                    tickTimeThreshold, retain, msg -> emit(callback, "command_backup", Map.of("message", msg), msg));
        }
    }

    private void handleOptimize(OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        if (!guardAdminCommand(cmd, isAdmin, callback)) return;
        if (!guardOptimizeEnabled(callback)) return;
        MaintenanceConfig maintenance = configs.maintenance();
        long tickTimeThreshold = maintenance.optimizeTickTimeThreshold();
        if (maintenanceService != null) {
            maintenanceService.optimize(
                    tickTimeThreshold, msg -> emit(callback, "command_optimize", Map.of("message", msg), msg));
        }
    }

    // ---- Blacklist command ----

    private void handleBlacklist(OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        if (!guardAdminCommand(cmd, isAdmin, callback)) return;
        if (blacklistService == null) {
            emit(callback, "command_blacklist_error", Map.of("message", "黑名单服务不可用"), "黑名单服务不可用");
            return;
        }
        if (rawArgs.isEmpty()) {
            List<String> patterns = blacklistService.getPatterns();
            if (patterns.isEmpty()) {
                emit(callback, "command_blacklist_list", Map.of("patterns", "黑名单为空"), "黑名单为空");
            } else {
                emit(
                        callback,
                        "command_blacklist_list",
                        Map.of("patterns", String.join("\n", patterns)),
                        String.join("\n", patterns));
            }
            return;
        }
        if (rawArgs.startsWith("-")) {
            blacklistService.remove(rawArgs.substring(1));
            emit(
                    callback,
                    "command_blacklist_remove",
                    Map.of("message", "已移除: " + rawArgs.substring(1)),
                    "已移除: " + rawArgs.substring(1));
        } else {
            blacklistService.add(rawArgs);
            emit(callback, "command_blacklist_add", Map.of("message", "已添加: " + rawArgs), "已添加: " + rawArgs);
        }
    }

    // ---- Helper ----

    // ---- Whitelist rendering ----

    private void renderWhitelistWithCleanup(
            Consumer<MessageEnvelope> callback,
            Integer page,
            int delayTicks,
            WhitelistService svc,
            WhitelistConfig whitelistConfig) {
        server.runSync(() -> {
            Set<String> removed =
                    svc.cleanupInactivePlayers(server.server(), Math.max(1, whitelistConfig.cleanupInactiveDays()));
            server.runAsync(() -> {
                try {
                    ArrayList<String> updatedLines = new ArrayList<>(svc.buildWhitelistLines(server.server()));
                    BotCommandListFeedbackService.WhitelistHeader headerInfo =
                            listFeedbackService.buildWhitelistHeader(updatedLines.size());
                    if (!removed.isEmpty()) {
                        BotCommandListFeedbackService.CleanupNotice notice =
                                listFeedbackService.buildCleanupNotice(removed);
                        emit(
                                callback,
                                "command_whitelist_cleanup",
                                listFeedbackService.cleanupVars(notice),
                                notice.fallback());
                    }
                    emitWhitelistPages(callback, headerInfo.header(), updatedLines, delayTicks, page);
                } catch (Exception e) {
                    server.logger().log(Level.SEVERE, "renderWhitelistWithCleanup 异步任务异常", e);
                }
            });
        });
    }

    private void renderWhitelistPages(
            Consumer<MessageEnvelope> callback, Integer page, int delayTicks, WhitelistService svc) {
        ArrayList<String> lines = new ArrayList<>(svc.buildWhitelistLines(server.server()));
        BotCommandListFeedbackService.WhitelistHeader headerInfo =
                listFeedbackService.buildWhitelistHeader(lines.size());
        emitWhitelistPages(callback, headerInfo.header(), lines, delayTicks, page);
    }

    private void emitWhitelistPages(
            Consumer<MessageEnvelope> callback, String header, ArrayList<String> lines, int delayTicks, Integer page) {
        Paginator.paginatePages(
                server,
                (pageIndex, total, headerText, body) -> {
                    BotCommandListFeedbackService.WhitelistPage pageInfo =
                            listFeedbackService.buildWhitelistPage(headerText, pageIndex, total, body);
                    emit(callback, "command_whitelist_page", pageInfo.vars(), pageInfo.fallback());
                },
                header,
                lines,
                delayTicks,
                page);
    }

    // ---- Guards ----

    // ---- Emitters ----

}
