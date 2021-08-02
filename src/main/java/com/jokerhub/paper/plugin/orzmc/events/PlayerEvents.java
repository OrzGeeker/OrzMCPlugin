package com.jokerhub.paper.plugin.orzmc.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.URL;

public class PlayerEvents implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        System.out.println(player.displayName() + "Join!!");

        String msg = player.displayName() + "上线了！";
        sendQQGroupMsg(msg);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        System.out.println(player.displayName() + "Quit!!");

        String msg = player.displayName() + "下线了！";
        sendQQGroupMsg(msg);
    }

    @EventHandler
    public void onPlayerKickLeave(PlayerKickEvent event) {
        Player player = event.getPlayer();
        System.out.println(player.displayName() + "KickLeave!!");

        String msg = player.displayName() + "被踢了！";
        sendQQGroupMsg(msg);
    }

    void sendQQGroupMsg(String msg) {
        // TODO: 调用OPQ消息API发送消息到QQ群
    }
}
