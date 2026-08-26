package com.jokerhub.paper.plugin.orzmc.features.player;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.WorldMaintenanceService;
import com.jokerhub.paper.plugin.orzmc.features.security.AccessRuleService;
import com.jokerhub.paper.plugin.orzmc.features.security.GeoIpAccessService;
import com.jokerhub.paper.plugin.orzmc.features.security.PlayerNameRule;
import com.jokerhub.paper.plugin.orzmc.infra.config.TemplateKeys;
import com.jokerhub.paper.plugin.orzmc.infra.notify.Notifier;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import java.util.Map;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * 登录访问控制统一入口。
 *
 * <p>把维护模式、IP 黑名单、GeoIP 的 prelogin 编排收敛到同一处，事件监听器只负责转发。
 * 顺序固定为：维护模式 → 本地 IP 黑名单 → 玩家名规则 → GeoIP 国家/地区白名单。</p>
 */
public final class LoginAccessControlService {

    private final WorldMaintenanceService maintenanceService;
    private final AccessRuleService accessRuleService;
    private final GeoIpAccessService geoIpAccessService;
    private final PlayerEventService playerEventService;
    private final Notifier notifier;
    private final TypedConfigProvider configs;
    private final OrzTextStyles styles;
    private final ServerFacade server;

    public LoginAccessControlService(
            WorldMaintenanceService maintenanceService,
            AccessRuleService accessRuleService,
            GeoIpAccessService geoIpAccessService,
            PlayerEventService playerEventService,
            Notifier notifier,
            TypedConfigProvider configs,
            OrzTextStyles styles,
            ServerFacade server) {
        this.maintenanceService = maintenanceService;
        this.accessRuleService = accessRuleService;
        this.geoIpAccessService = geoIpAccessService;
        this.playerEventService = playerEventService;
        this.notifier = notifier;
        this.configs = configs;
        this.styles = styles;
        this.server = server;
    }

    public void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        if (maintenanceService.isRunning()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, styles.warn("服务器地图备份中，请稍后再尝试登录。"));
            return;
        }
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            return;
        }
        // getAddress() 在 prelogin 中通常恒有值，但做防御：为 null 视为无地址，
        // 跳过 IP/GeoIP 检查，玩家名规则仍照常生效。
        java.net.InetAddress address = event.getAddress();
        String ipAddress = address == null ? "" : address.getHostAddress();
        String matchedPattern = accessRuleService.matchedIpPattern(ipAddress);
        if (matchedPattern != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, styles.error("你的IP已被禁止访问"));
            notifyBanHit(playerName(event), ipAddress, matchedPattern);
            return;
        }
        String playerName = playerName(event);
        PlayerNameRule matchedNameRule = accessRuleService.matchedPlayerNameRule(playerName);
        if (matchedNameRule != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, styles.error("你的玩家名不符合服务器访问规则"));
            notifyPlayerNameBlocked(playerName, matchedNameRule);
            return;
        }
        if (ipAddress.isEmpty()) {
            return;
        }
        // 阻塞等待本次查询结果：只在异步处理器线程上等待，不阻塞主线程；
        // 超时/异常由 handleGeoIpPreLogin 内部按 fail-open 放行并告警。
        playerEventService.handleGeoIpPreLogin(
                event,
                playerName,
                ipAddress,
                geoIpAccessService.decide(ipAddress),
                GeoIpAccessService.DECISION_TIMEOUT_MS);
    }

    /** 封禁命中（安全加固 P2-4）：PRIVATE 私信管理员 + 服务端日志。 */
    private void notifyBanHit(String player, String ip, String pattern) {
        String fallback = "⚠ IP 黑名单拦截\n玩家: " + player + "\nIP: " + ip + "\n命中规则: " + pattern;
        MessageEnvelope env = configs.renderTemplate(
                TemplateKeys.IP_BLACKLIST_BLOCK, Map.of("player", player, "ip", ip, "pattern", pattern), fallback);
        notifier.event(TemplateKeys.IP_BLACKLIST_BLOCK, env);
        server.logger().warning("黑名单拦截: " + player + " (" + ip + ") 命中规则 " + pattern);
    }

    private void notifyPlayerNameBlocked(String player, PlayerNameRule rule) {
        String fallback = "⚠ 玩家名规则拦截\n玩家: " + player + "\n命中规则: " + rule.display();
        MessageEnvelope env = configs.renderTemplate(
                TemplateKeys.PLAYER_NAME_BLOCK, Map.of("player", player, "rule", rule.display()), fallback);
        notifier.event(TemplateKeys.PLAYER_NAME_BLOCK, env);
        server.logger().warning("玩家名规则拦截: " + player + " 命中规则 " + rule.display());
    }

    private static String playerName(AsyncPlayerPreLoginEvent event) {
        PlayerProfile profile = event.getPlayerProfile();
        return profile != null && profile.getName() != null ? profile.getName() : "未知玩家";
    }
}
