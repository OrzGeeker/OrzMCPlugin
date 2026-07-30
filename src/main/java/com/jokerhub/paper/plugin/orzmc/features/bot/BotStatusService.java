package com.jokerhub.paper.plugin.orzmc.features.bot;

import com.jokerhub.paper.plugin.orzmc.core.ports.health.HealthStatus;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import net.kyori.adventure.text.Component;

public final class BotStatusService {
    private final OrzTextStyles styles;
    private final HealthStatus health;

    public BotStatusService(OrzTextStyles styles, HealthStatus health) {
        this.styles = styles;
        this.health = health;
    }

    public Component buildStatusMessage() {
        HealthStatus.Entry easybot = health.get("easybot");
        return styles.warn("EasyBot:")
                .append(Component.space())
                .append(easybot.enabled() ? styles.success("enabled") : styles.error("disabled"))
                .append(Component.space())
                .append(easybot.httpOk() ? styles.success("httpOk") : styles.error("httpNotOk"))
                .append(Component.space())
                .append(easybot.wsConnected() ? styles.success("wsOk") : styles.error("wsNotOk"))
                .append(Component.space())
                .append(
                        easybot.lastError() == null || easybot.lastError().isEmpty()
                                ? styles.success("")
                                : styles.error("lastError: " + easybot.lastError()));
    }
}
