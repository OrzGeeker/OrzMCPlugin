package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.TextComponent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PlayerEvents implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        TextComponent playDisplayName = (TextComponent) player.displayName();
        String playName = playDisplayName.content();
        OrzMC.logger().info(playName + "Join!!");

        String msg = playName + "上线了！";
        notifyQQGroupWithMsg(msg);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        TextComponent playDisplayName = (TextComponent) player.displayName();
        String playName = playDisplayName.content();
        OrzMC.logger().info(playName + "Quit!!");

        String msg = playName + "下线了！";
        notifyQQGroupWithMsg(msg);
    }

    @EventHandler
    public void onPlayerKickLeave(PlayerKickEvent event) {
        Player player = event.getPlayer();

        TextComponent playDisplayName = (TextComponent) player.displayName();
        String playName = playDisplayName.content();
        OrzMC.logger().info(playName + "KickLeave!!");

        String msg = playName + "被踢了！";
        notifyQQGroupWithMsg(msg);
    }

    private void notifyQQGroupWithMsg(String msg) {
        try {
            StringBuilder msgBuilder = new StringBuilder(msg);
            Object[] objs = OrzMC.server().getOnlinePlayers().toArray();
            String tip = String.format("------当前在线(%s/%d)------", objs.length, OrzMC.server().getMaxPlayers());
            msgBuilder.append(tip);
            for(Object obj: objs){
                if(obj instanceof Player) {
                    Player player = (Player) obj;
                    TextComponent playDisplayName = (TextComponent) player.displayName();
                    msgBuilder.append("\n").append(playDisplayName.content());
                }
            }
            msg = msgBuilder.toString();
            sendQQGroupMsg(msg);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    private void sendQQGroupMsg(String msg) throws Exception {
        String groupId = "1056934080";
        String url = "http://localhost:8200/send_group_msg?group_id=" + groupId + "&message=" + msg;

        HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
        httpClient.setRequestMethod("GET");
        httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = httpClient.getResponseCode();
        OrzMC.logger().info("Sending 'GET' request to URL : " + url);
        OrzMC.logger().info("Response Code : " + responseCode);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            OrzMC.logger().info(response.toString());
        }
    }
}
