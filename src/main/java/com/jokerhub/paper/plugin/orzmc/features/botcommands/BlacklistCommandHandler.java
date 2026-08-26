package com.jokerhub.paper.plugin.orzmc.features.botcommands;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.security.AccessRuleService;
import com.jokerhub.paper.plugin.orzmc.features.security.PlayerNameRule;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * $d 访问规则命令处理器（从 BotCommandService 抽离）。
 *
 * <p>{@code accessRuleService} 通过 {@link Supplier} 注入——组合根经
 * {@link BotCommandService#injectDependencies} 一次性注入，处理器调用时读取最新值；未注入时提示服务不可用。</p>
 */
final class BlacklistCommandHandler extends BotCommandContext {

    private final Supplier<AccessRuleService> accessRuleService;

    BlacklistCommandHandler(
            ServerFacade server, TypedConfigProvider configs, Supplier<AccessRuleService> accessRuleService) {
        super(server, configs);
        this.accessRuleService = accessRuleService;
    }

    void handleBlacklist(OrzUserCmd cmd, boolean isAdmin, Consumer<MessageEnvelope> callback, String rawArgs) {
        if (!guardAdminCommand(cmd, isAdmin, callback)) return;
        AccessRuleService svc = accessRuleService.get();
        if (svc == null) {
            emit(callback, "command_blacklist_error", Map.of("message", "黑名单服务不可用"), "黑名单服务不可用");
            return;
        }
        if (rawArgs.isEmpty()) {
            listAccessRules(callback, svc);
            return;
        }
        if ("player".equals(rawArgs) || "player list".equals(rawArgs)) {
            listPlayerRules(callback, svc);
            return;
        }
        if (rawArgs.startsWith("-player")) {
            String rest = rawArgs.substring("-player".length());
            if (!rest.isEmpty() && !rest.startsWith(" ")) {
                emit(
                        callback,
                        "command_blacklist_error",
                        Map.of("message", "用法: $d -player <type> <value>"),
                        "用法: $d -player <type> <value>");
                return;
            }
            handlePlayerRule(callback, svc, true, rest.trim());
            return;
        }
        if (rawArgs.startsWith("player")) {
            String rest = rawArgs.substring("player".length());
            if (!rest.isEmpty() && !rest.startsWith(" ")) {
                emit(
                        callback,
                        "command_blacklist_error",
                        Map.of("message", "用法: $d player <type> <value>"),
                        "用法: $d player <type> <value>");
                return;
            }
            handlePlayerRule(callback, svc, false, rest.trim());
            return;
        }
        if (rawArgs.startsWith("-")) {
            svc.removeIpPattern(rawArgs.substring(1));
            emit(
                    callback,
                    "command_blacklist_remove",
                    Map.of("message", "已移除: " + rawArgs.substring(1)),
                    "已移除: " + rawArgs.substring(1));
        } else {
            svc.addIpPattern(rawArgs);
            emit(callback, "command_blacklist_add", Map.of("message", "已添加: " + rawArgs), "已添加: " + rawArgs);
        }
    }

    private void listAccessRules(Consumer<MessageEnvelope> callback, AccessRuleService svc) {
        List<String> lines = new ArrayList<>();
        List<String> ips = svc.getIpPatterns();
        List<PlayerNameRule> rules = svc.getPlayerNameRules();
        if (ips.isEmpty() && rules.isEmpty()) {
            emit(callback, "command_blacklist_list", Map.of("patterns", "访问规则为空"), "访问规则为空");
            return;
        }
        if (!ips.isEmpty()) {
            lines.add("IP黑名单:");
            ips.forEach(line -> lines.add("  " + line));
        }
        if (!rules.isEmpty()) {
            lines.add("玩家名规则:");
            rules.forEach(rule -> lines.add("  " + rule.display()));
        }
        String content = String.join("\n", lines);
        emit(callback, "command_blacklist_list", Map.of("patterns", content), content);
    }

    private void listPlayerRules(Consumer<MessageEnvelope> callback, AccessRuleService svc) {
        List<PlayerNameRule> rules = svc.getPlayerNameRules();
        if (rules.isEmpty()) {
            emit(callback, "command_blacklist_list", Map.of("patterns", "玩家名规则为空"), "玩家名规则为空");
            return;
        }
        String content = rules.stream()
                .map(PlayerNameRule::display)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        emit(callback, "command_blacklist_list", Map.of("patterns", content), content);
    }

    private void handlePlayerRule(
            Consumer<MessageEnvelope> callback, AccessRuleService svc, boolean remove, String raw) {
        String[] parts = raw.split("\\s+", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            emit(
                    callback,
                    "command_blacklist_error",
                    Map.of("message", "用法: $d " + (remove ? "-" : "") + "player <type> <value>"),
                    "用法: $d " + (remove ? "-" : "") + "player <type> <value>");
            return;
        }
        PlayerNameRule.MatchType type = PlayerNameRule.MatchType.from(parts[0]);
        if (type == null) {
            emit(
                    callback,
                    "command_blacklist_error",
                    Map.of("message", "无效匹配类型: " + parts[0] + "（支持 exact/prefix/suffix/contains/glob/regex）"),
                    "无效匹配类型: " + parts[0] + "（支持 exact/prefix/suffix/contains/glob/regex）");
            return;
        }
        PlayerNameRule rule = PlayerNameRule.of(type, parts[1]);
        if (!rule.isValid()) {
            emit(
                    callback,
                    "command_blacklist_error",
                    Map.of("message", "无效的正则表达式: " + parts[1]),
                    "无效的正则表达式: " + parts[1]);
            return;
        }
        String message = (remove ? "已移除玩家名规则: " : "已添加玩家名规则: ") + rule.display();
        if (remove) {
            svc.removePlayerNameRule(type, parts[1]);
            emit(callback, "command_blacklist_remove", Map.of("message", message), message);
        } else {
            svc.addPlayerNameRule(type, parts[1]);
            emit(callback, "command_blacklist_add", Map.of("message", message), message);
        }
    }
}
