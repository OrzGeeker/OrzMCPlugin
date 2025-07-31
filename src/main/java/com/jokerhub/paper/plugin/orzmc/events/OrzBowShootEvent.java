package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.commands.OrzTPBow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class OrzBowShootEvent implements Listener {

    @EventHandler
    public void onBowShoot(@NotNull ProjectileHitEvent event) {

        if (event.getEntity() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player player) {
                ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
                if (meta == null) {
                    return;
                }
                Component mainHandItemName = meta.displayName();
                if (mainHandItemName instanceof TextComponent displayName) {
                    if (displayName.content().equals(OrzTPBow.name)) {
                        if (arrow.isInWater()) {
                            player.sendMessage(OrzTPBow.logText("箭射进了水里!"));
                            return;
                        }
                        if (arrow.isInLava()) {
                            player.sendMessage(OrzTPBow.logText("箭射进了岩浆里!"));
                            return;
                        }
                        player.teleport(arrow);
                        player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, 1.0F, 1.0F);
                        player.sendMessage(OrzTPBow.logText("传送完成!"));
                    }
                }
            }
        }
    }
}