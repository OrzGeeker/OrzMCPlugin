package com.jokerhub.paper.plugin.orzmc.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.util.io.BukkitObjectInputStream;

public class demo implements Listener {
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getEntity().sendMessage("Wow! You died loser!!!");
        event.getEntity().setFlying(true);

    }

    @EventHandler
    public void onLeaveBed(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("You left a bed new");
    }

    @EventHandler
    public void onShearSheep(PlayerShearEntityEvent event) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.GREEN + "Nice Try!");
    }

    @EventHandler
    public void onBowShoot(ProjectileHitEvent event) {

        if(event.getEntity() instanceof Arrow) {
            Player player = (Player) event.getEntity().getShooter();
            Location location = event.getEntity().getLocation();

            player.teleport(location);
            player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, 1.0F, 1.0F);
            player.sendMessage("Swoosh!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity entity = Bukkit.getWorld("world").spawnEntity(new Location(Bukkit.getWorld("world"), 33, 70, -156), EntityType.SKELETON);
        entity.setGravity(false);
        entity.setGlowing(true);
        entity.setCustomName("Daddy Skeleton");
        entity.setCustomNameVisible(true);
    }
}
