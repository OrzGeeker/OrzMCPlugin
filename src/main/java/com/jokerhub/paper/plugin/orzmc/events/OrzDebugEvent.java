package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.bot.OrzNotifier;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.stream.Collectors;

public class OrzDebugEvent implements Listener {

    @EventHandler
    public void cmdDebugHandler(ServerCommandEvent event) {
        OrzMC.server().getScheduler().runTaskAsynchronously(OrzMC.plugin(), () -> {
            OrzNotifier.processMessage(event.getCommand(), true, result -> {
                String whitelistUserNames = OrzMC.server().getWhitelistedPlayers().stream().map((OfflinePlayer::getName)).collect(Collectors.joining(","));
                OrzMC.logger().info("whitelist: " + whitelistUserNames);
                OrzMC.logger().info("cmd debug: " + result);
            });
        });
    }
}
