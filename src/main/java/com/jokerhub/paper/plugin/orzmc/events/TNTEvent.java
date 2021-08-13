package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TNTEvent implements Listener {
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {

        Block block = event.getBlock();
        Location tntLocation = block.getLocation();
        OrzMC.logger().info("坐标:" + tntLocation + "处，TNT被点着！");
        event.setCancelled(true);

        switch (event.getReason()) {
            case FIRE: // 被火点着
            case PROJECTILE: // 被箭点着
            case ITEM:  // 被打火石点着
            {


            }
                break;
            case REDSTONE:  // 红石点着
                break;
            case EXPLOSION: // 被其它TNT点着
                break;
            default:
                break;
        }
    }
}
