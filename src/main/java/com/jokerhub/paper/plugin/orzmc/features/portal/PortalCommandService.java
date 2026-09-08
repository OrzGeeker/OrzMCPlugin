package com.jokerhub.paper.plugin.orzmc.features.portal;

import com.jokerhub.paper.plugin.orzmc.core.ports.portal.PortalInfo;
import com.jokerhub.paper.plugin.orzmc.core.ports.portal.PortalPort;
import com.jokerhub.paper.plugin.orzmc.features.command.CommandFeedbackService;
import com.jokerhub.paper.plugin.orzmc.features.command.binding.CommandPermissionService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.Lang;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.MessageKeys;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

public final class PortalCommandService {
    private final CommandFeedbackService feedbackService;
    private final CommandPermissionService permissionService;
    private final I18nService i18n;
    private final PortalPort portalService;
    private final OrzTextStyles styles;

    public PortalCommandService(PortalPort portalService, OrzTextStyles styles, I18nService i18n) {
        this.portalService = portalService;
        this.styles = styles;
        this.i18n = i18n;
        this.feedbackService = new CommandFeedbackService(i18n);
        this.permissionService = new CommandPermissionService(i18n);
    }

    public sealed interface Result permits Result.Success, Result.Failure {
        record Success(TextComponent message) implements Result {}

        record Failure(TextComponent message) implements Result {}
    }

    public Result handle(Player player, String[] args) {
        CommandPermissionService.PermissionResult pr = permissionService.requireAdmin(player);
        if (!pr.allowed()) {
            return new Result.Failure(pr.message());
        }
        Lang lang = i18n.langFor(player);
        if (args == null || args.length < 1) {
            return new Result.Failure(styles.info(i18n.msg(lang, MessageKeys.PORTAL_USAGE)));
        }
        if ("remove".equalsIgnoreCase(args[0]) || "rm".equalsIgnoreCase(args[0])) {
            return handleRemove(player, lang, args);
        }
        return handleCreate(player, lang, args);
    }

    private Result handleRemove(Player player, Lang lang, String[] args) {
        if (args.length < 2) {
            return new Result.Failure(styles.info(i18n.msg(lang, MessageKeys.PORTAL_USAGE_REMOVE)));
        }
        String host = args[1];
        int port = 25565;
        if (args.length >= 3) {
            try {
                port = Integer.parseInt(args[2]);
            } catch (Exception e) {
                return new Result.Failure(styles.warn(i18n.msg(lang, MessageKeys.PORTAL_PORT_REQUIRED)));
            }
        }
        String target = host + ":" + port;
        int removed = portalService.removeByTarget(target);
        if (removed <= 0) {
            return new Result.Success(
                    styles.warn(i18n.msg(lang, MessageKeys.PORTAL_NOT_FOUND, Map.of("target", target))));
        }
        return new Result.Success(styles.success(i18n.msg(
                lang, MessageKeys.PORTAL_REMOVED, Map.of("count", Integer.toString(removed), "target", target))));
    }

    private Result handleCreate(Player player, Lang lang, String[] args) {
        String host = args[0];
        int port = 25565;
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (Exception e) {
                return new Result.Failure(styles.warn(i18n.msg(lang, MessageKeys.PORTAL_PORT_REQUIRED)));
            }
        }
        PortalInfo info = portalService.createPortal(player, host, port);
        String msg = i18n.msg(
                lang,
                MessageKeys.PORTAL_CREATED,
                Map.of(
                        "host", host,
                        "port", Integer.toString(port),
                        "world", info.location().getWorld().getName(),
                        "x", Integer.toString(info.location().getBlockX()),
                        "y", Integer.toString(info.location().getBlockY()),
                        "z", Integer.toString(info.location().getBlockZ()),
                        "axis", info.axis().name()));
        return new Result.Success(styles.success(msg));
    }

    public Component requirePlayerTip() {
        return feedbackService.playerRequiredTip(null);
    }
}
