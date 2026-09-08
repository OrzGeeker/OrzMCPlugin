package com.jokerhub.paper.plugin.orzmc.features.server;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.notify.Notifier;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;

public final class ServerLifecycleService {
    private final ServerFacade server;
    private final TypedConfigProvider configs;
    private final Notifier notifier;

    public ServerLifecycleService(ServerFacade server, TypedConfigProvider configs, Notifier notifier) {
        this.server = server;
        this.configs = configs;
        this.notifier = notifier;
    }

    public void notifyServerStop() {
        String minecraftVersion = server.server().getMinecraftVersion();
        MessageEnvelope env = configs.renderEvent("server_stop", java.util.Map.of("version", minecraftVersion));
        notifier.event("server_stop", env);
    }
}
