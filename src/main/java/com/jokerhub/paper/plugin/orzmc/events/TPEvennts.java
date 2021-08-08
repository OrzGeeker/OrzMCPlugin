package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.conversations.Conversation;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.awt.*;

public class TPEvennts implements Listener {
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {

        if(event.getEntity() instanceof Tameable) {
            return;
        }

        if(event.getEntity() instanceof Enderman) {
            return;
        }

        // 禁用tp实体
        event.setCancelled(true);
        OrzMC.logger().info("实体传送被禁用:" + event.getEntity().getName());
    }

//    @EventHandler
//    public void onPlayerTeleport(PlayerTeleportEvent event) {
//        Player player = event.getPlayer();
//        switch (event.getCause()) {
//            case COMMAND:
//                event.setCancelled(true);
//                OrzMC.logger().info("传送玩家命令已经被禁用了");
//                break;
//        }
//    }
}
