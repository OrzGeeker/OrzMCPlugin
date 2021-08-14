package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.commands.TPBow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.ItemMeta;

public class BowShootEvent implements Listener {

    @EventHandler
    public void onBowShoot(ProjectileHitEvent event) {

        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if(arrow.getShooter() instanceof Player) {
                Player player = (Player) arrow.getShooter();
                ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
                if(meta == null) {
                    return;
                }
                Component mainHandItemName = meta.displayName();
                if (mainHandItemName instanceof TextComponent) {
                    TextComponent displayName = (TextComponent) mainHandItemName;
                    if (displayName.content().equals(TPBow.name)) {
                        if (arrow.isInWater()) {
                            player.sendMessage(TPBow.logText("箭射进了水里!"));
                            return;
                        }
                        if(arrow.isInLava()) {
                            player.sendMessage(TPBow.logText("箭射进了岩浆里!"));
                            return;
                        }
                        player.teleport(arrow);
                        player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, 1.0F, 1.0F);
                        player.sendMessage(TPBow.logText("传送完成!"));
                    }
                }
            }
        }
    }
}