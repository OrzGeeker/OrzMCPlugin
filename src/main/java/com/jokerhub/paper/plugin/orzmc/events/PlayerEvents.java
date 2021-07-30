package com.jokerhub.paper.plugin.orzmc.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEvents implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        System.out.println(player.displayName() + "Join!!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        System.out.println(player.displayName() + "Quit!!");
    }

    @EventHandler
    public void onPlayerKickLeave(PlayerKickEvent event) {
        Player player = event.getPlayer();
        System.out.println(player.displayName() + "KickLeave!!");
    }
}
