package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;

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
//                System.out.println("传送玩家命令已经被禁用了");
//                player.sendMessage("传送玩家命令已经被禁用了");
//                break;
//        }
//    }
}
