package com.jokerhub.paper.plugin.orzmc.features.server;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.botcommands.OrzUserCmd;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceProgress;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceReason;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.BotConfig;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.MaintenanceConfig;
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

    public String buildServerLoadMessage(ServerLoadEvent event) {
        String onlineMode = server.server().getOnlineMode() ? "正版服" : "离线服";
        String minecraftVersion = server.server().getMinecraftVersion();
        String[] parts = {"Minecraft", minecraftVersion, onlineMode};
        StringBuilder stringBuilder = new StringBuilder(String.join(" ", parts)).append("\n");
        stringBuilder.append("---------------------------------").append("\n");
        switch (event.getType()) {
            case STARTUP -> stringBuilder.append("启动完成");
            case RELOAD -> stringBuilder.append("重启完成");
        }
        stringBuilder.append("\n\n");
        String prompt = configs.bot().cmdPromptChar();
        stringBuilder
                .append("发送 \"")
                .append(prompt)
                .append(OrzUserCmd.SHOW_HELP.cmdName())
                .append("\" 查看支持的命令消息");
        return stringBuilder.toString();
    }

    public Component buildMaintenanceMotd() {
        MaintenanceConfig maintenance = configs.maintenance();
        BotConfig botConfig = configs.bot();
        MaintenanceReason reason = maintenanceModeService.reason();
        // 进度快照取一次：场景文案渲染与进度行拼接用同一快照，避免两次读不同快照拼出不一致 MOTD
        MaintenanceProgress progress = maintenanceModeService.progress();
        String sceneBase = sceneTextBase(maintenance, reason);
        String msg = MaintenanceModeService.renderTemplate(sceneBase, progress);
        String discordLink = botConfig.discordServerLink();
        String qqGroupId = botConfig.qqGroupId();
        TextComponent.Builder motdBuilder = Component.text();
        motdBuilder.append(styles.warn("⚠ 维护中").decorate(TextDecoration.BOLD));
        motdBuilder.append(Component.newline());
        motdBuilder.append(styles.info(msg));
        // 场景模板已含进度占位符（{stage}/{percent}/{eta}）时，占位符已被替换进 msg，
        // 不追加独立进度行（避免两行进度重复，与登录拦截侧防重逻辑一致）；
        // 仅「纯场景文案 + 有进度」才追加独立进度行。
        if (progress != null && reason != MaintenanceReason.MANUAL && !hasProgressPlaceholders(sceneBase)) {
            motdBuilder.append(Component.newline());
            motdBuilder.append(styles.info(progress.progressMessage()));
        }
        if (qqGroupId != null && !qqGroupId.isEmpty()) {
            motdBuilder.append(Component.newline());
            motdBuilder.append(styles.info("QQ群: ")).append(styles.warn(qqGroupId));
        }
        if (discordLink != null && !discordLink.isEmpty()) {
            motdBuilder.append(Component.newline());
            motdBuilder
                    .append(styles.info("Discord: "))
                    .append(Component.text(discordLink)
                            .decorate(TextDecoration.UNDERLINED)
                            .hoverEvent(HoverEvent.showText(Component.text("点击加入 Discord")))
                            .clickEvent(ClickEvent.openUrl(discordLink)));
        }
        return motdBuilder.build();
    }

    /** 按维护原因选场景文案模板（backup/optimize/manual 三种可配置）。 */
    private static String sceneTextBase(MaintenanceConfig maintenance, MaintenanceReason reason) {
        return switch (reason) {
            case BACKUP -> maintenance.backupMaintenanceMotd();
            case OPTIMIZE -> maintenance.optimizeMaintenanceMotd();
            case MANUAL -> maintenance.manualMaintenanceMotd();
            case null -> maintenance.backupMaintenanceMotd();
        };
    }

    /** 文案模板是否声明了进度占位符：含则占位符已渲染进场景文案，无需再追加独立进度行。 */
    private static boolean hasProgressPlaceholders(String template) {
        return template != null
                && (template.contains("{stage}") || template.contains("{percent}") || template.contains("{eta}"));
    }
}
