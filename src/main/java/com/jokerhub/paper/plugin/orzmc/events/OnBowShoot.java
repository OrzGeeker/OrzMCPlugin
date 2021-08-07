package com.jokerhub.paper.plugin.orzmc.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.ItemMeta;

public class OnBowShoot implements Listener {

    @EventHandler
    public void onBowShoot(ProjectileHitEvent event) {

        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.isInWater()) {
                return;
            }

            if(arrow.getShooter() instanceof Player) {
                Player player = (Player) arrow.getShooter();
                ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
                if(meta == null) {
                    return;
                }
                Component mainHandItemName = meta.displayName();
                if (mainHandItemName instanceof TextComponent) {
                    TextComponent displayName = (TextComponent) mainHandItemName;
                    if (displayName.content().equals("传送弓")) {
                        player.teleport(arrow);
                        player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, 1.0F, 1.0F);
                        player.sendMessage("传到弓传送完成!");
                    }
                }
            }
        }
    }
}