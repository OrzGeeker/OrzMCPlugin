package com.jokerhub.paper.plugin.orzmc.features.botcommands;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nServiceHolder;
import com.jokerhub.paper.plugin.orzmc.infra.player.OnlineListFormatter;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.infra.templates.TemplateRenderer;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.entity.Player;

/**
 * $l/$w 列表类回复组装（i18n P4a 收口）。
 *
 * <p>四个命令回复模板（command_players/command_whitelist_header/command_whitelist_page/
 * command_whitelist_cleanup）正文已改为 {@code {message}} 直通壳：此处把语言包 bot.list.* 各段
 * （online_header/whitelist_header/cleanup_title/page_meta）按默认语言 R1 组装为完整文案并作为
 * {@code message} 变量传入，模板仅承担格式（templates.format）与服主自定义正文覆盖。
 *
 * <p>占位符变量均为「新 message + 旧分段变量」并集：全新安装（模板为 {@code {message}}）渲染
 * 组装文案；历史磁盘模板（仍是旧字面正文）与服主自定义正文照常渲染旧变量，无回归。</p>
 */
public final class BotCommandListFeedbackService {
    private final ServerFacade server;
    private final TypedConfigProvider configs;
    private final OnlineListFormatter listFormatter;

    public BotCommandListFeedbackService(ServerFacade server, TypedConfigProvider configs) {
        this(server, configs, new OnlineListFormatter());
    }

    public BotCommandListFeedbackService(
            ServerFacade server, TypedConfigProvider configs, OnlineListFormatter listFormatter) {
        this.server = server;
        this.configs = configs;
        this.listFormatter = listFormatter;
    }

    public record OnlineList(String list, String fallback, String header, String onlineCount, String maxCount) {}

    public record WhitelistHeader(String header, String fallback) {}

    public record CleanupNotice(String removedList, String fallback) {}

    public record WhitelistPage(String fallback, Map<String, String> vars) {}

    public OnlineList buildOnlineList(ArrayList<Player> onlinePlayers, int maxPlayers) {
        int count = onlinePlayers.size();
        String countText = String.valueOf(count);
        String maxText = String.valueOf(maxPlayers);
        String header = I18nServiceHolder.msg("bot.list.online_header", Map.of("count", countText, "max", maxText));
        String list = listFormatter.list(onlinePlayers);
        // 完整文案（语言包）作为 {message}；模板正文直通时即最终输出
        String composed = header + (list.isEmpty() ? "" : "\n" + list);
        return new OnlineList(list, composed, header, countText, maxText);
    }

    public Map<String, String> onlineVars(OnlineList online) {
        return Map.of(
                "message", online.fallback(),
                "online_count", online.onlineCount(),
                "max_count", online.maxCount(),
                "online_list", online.list());
    }

    public WhitelistHeader buildWhitelistHeader(int total) {
        Map<String, String> vars = whitelistHeaderVars(total);
        String headerFallback = vars.get("message");
        String headerTemplate = configs.resolveTemplate("command_whitelist_header", headerFallback);
        String header = TemplateRenderer.render(headerTemplate, vars);
        return new WhitelistHeader(header, header);
    }

    public Map<String, String> whitelistHeaderVars(int total) {
        String count = String.valueOf(total);
        return Map.of(
                "message", I18nServiceHolder.msg("bot.list.whitelist_header", Map.of("count", count)), "count", count);
    }

    public CleanupNotice buildCleanupNotice(java.util.Set<String> removed) {
        String removedList = String.join(
                "\n", removed.stream().map(name -> "✔︎ " + name).collect(java.util.stream.Collectors.toSet()));
        String composed = I18nServiceHolder.msg("bot.list.cleanup_title") + "\n" + removedList;
        return new CleanupNotice(removedList, composed);
    }

    public Map<String, String> cleanupVars(CleanupNotice notice) {
        return Map.of("message", notice.fallback(), "removed_list", notice.removedList());
    }

    public WhitelistPage buildWhitelistPage(String headerText, int pageIndex, int total, String body) {
        String page = String.valueOf(pageIndex);
        String totalText = String.valueOf(total);
        String composed = headerText + "\n"
                + I18nServiceHolder.msg("bot.list.page_meta", Map.of("page", page, "total", totalText))
                + "\n"
                + body;
        Map<String, String> vars = Map.of(
                "message", composed,
                "header", headerText,
                "page", page,
                "total", totalText,
                "body", body);
        return new WhitelistPage(composed, vars);
    }

    public ArrayList<Player> currentOnlinePlayers() {
        ArrayList<Player> onlinePlayers = new ArrayList<>();
        Object[] objects = server.server().getOnlinePlayers().toArray();
        for (Object obj : objects) {
            if (obj instanceof Player p) {
                onlinePlayers.add(p);
            }
        }
        return onlinePlayers;
    }
}
