package com.jokerhub.paper.plugin.orzmc;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Events implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getEntity().sendMessage("Wow! You died loser!!!");
        event.getEntity().setFlying(true);

    }

    @EventHandler
    public void onLeaveBed(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("You left a bed new change");
    }

    @EventHandler
    public void onShearSheep(PlayerShearEntityEvent event) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.GREEN + "Nice Try!");
    }

}
