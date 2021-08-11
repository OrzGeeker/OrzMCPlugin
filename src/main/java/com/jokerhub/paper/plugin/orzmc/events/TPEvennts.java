package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;

public class TPEvennts implements Listener {
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {

        if(event.getEntity() instanceof Tameable) {
            return;
        }

        if(event.getEntity() instanceof Enderman || event.getEntity() instanceof ArmorStand || event.getEntity() instanceof Shulker) {
            return;
        }

        // 禁用tp实体
        event.setCancelled(true);
        OrzMC.logger().info("实体传送被禁用:" + event.getEntity().getName());
    }
}
