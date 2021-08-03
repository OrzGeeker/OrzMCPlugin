package com.jokerhub.paper.plugin.orzmc.events;


import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.awt.print.Paper;

public class TPEvennts implements Listener {
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        // 禁用tp实体
        event.setCancelled(true);
        System.out.println("实体传送被禁用");
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
