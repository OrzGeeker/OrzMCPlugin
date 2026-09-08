package com.jokerhub.paper.plugin.orzmc.features.server;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.botcommands.OrzUserCmd;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceProgress;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceReason;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.BotConfig;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nServiceHolder;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.MessageKeys;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.server.ServerLoadEvent;

public final class ServerFeedbackService {
    private final ServerFacade server;
    private final TypedConfigProvider configs;
    private final OrzTextStyles styles;
    private final MaintenanceModeService maintenanceModeService;

    public ServerFeedbackService(
            ServerFacade server,
            TypedConfigProvider configs,
            OrzTextStyles styles,
            MaintenanceModeService maintenanceModeService) {
        this.server = server;
        this.configs = configs;
        this.styles = styles;
        this.maintenanceModeService = maintenanceModeService;
    }

    /** 组装 server_load 事件变量（P5-2：正文迁语言包 event.server_load，此处仅注入动态值）。 */
    public java.util.Map<String, String> buildServerLoadVars(ServerLoadEvent event) {
        String mode = server.server().getOnlineMode()
                ? I18nServiceHolder.msg(MessageKeys.SERVERLIFE_MODE_ONLINE)
                : I18nServiceHolder.msg(MessageKeys.SERVERLIFE_MODE_OFFLINE);
        String status =
                switch (event.getType()) {
                    case STARTUP -> I18nServiceHolder.msg(MessageKeys.SERVERLIFE_STATUS_STARTUP);
                    case RELOAD -> I18nServiceHolder.msg(MessageKeys.SERVERLIFE_STATUS_RELOAD);
                };
        String promptHelp = configs.bot().cmdPromptChar() + OrzUserCmd.SHOW_HELP.cmdName();
        return java.util.Map.of(
                "version", server.server().getMinecraftVersion(),
                "mode", mode,
                "status", status,
                "prompt_help", promptHelp);
    }

    public Component buildMaintenanceMotd() {
        BotConfig botConfig = configs.bot();
        MaintenanceReason reason = maintenanceModeService.reason();
        // 进度快照取一次：场景文案渲染与进度行拼接用同一快照，避免两次读不同快照拼出不一致 MOTD
        MaintenanceProgress progress = maintenanceModeService.progress();
        // 统一渲染入口：场景文案（maintenance.motd.* 语言包/磁盘正文）+ 进度行（MaintenanceTexts）
        String msg = MaintenanceModeService.renderMotdText(reason, configs.maintenanceTexts(), progress);
        String discordLink = botConfig.discordServerLink();
        String qqGroupId = botConfig.qqGroupId();
        TextComponent.Builder motdBuilder = Component.text();
        motdBuilder.append(styles.warn(I18nServiceHolder.msg(MessageKeys.SERVERLIFE_MOTD_TITLE))
                .decorate(TextDecoration.BOLD));
        motdBuilder.append(Component.newline());
        motdBuilder.append(styles.info(msg));
        if (qqGroupId != null && !qqGroupId.isEmpty()) {
            motdBuilder.append(Component.newline());
            motdBuilder
                    .append(styles.info(I18nServiceHolder.msg(MessageKeys.SERVERLIFE_QQ_LABEL)))
                    .append(styles.warn(qqGroupId));
        }
        if (discordLink != null && !discordLink.isEmpty()) {
            motdBuilder.append(Component.newline());
            motdBuilder
                    .append(styles.info(I18nServiceHolder.msg(MessageKeys.SERVERLIFE_DISCORD_LABEL)))
                    .append(Component.text(discordLink)
                            .decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(
                                    Component.text(I18nServiceHolder.msg(MessageKeys.SERVERLIFE_DISCORD_HOVER))))
                            .clickEvent(ClickEvent.openUrl(discordLink)));
        }
        return motdBuilder.build();
    }
}
