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
import java.net.URLEncoder;
import java.util.ArrayList;

public class PlayerEvents implements Listener {

    enum PlayerState {
        JOIN,
        QUIT,
        KICK
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        notifyQQGroupWithMsg(event.getPlayer(), PlayerState.JOIN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        notifyQQGroupWithMsg(event.getPlayer(), PlayerState.QUIT);
    }

    @EventHandler
    public void onPlayerKickLeave(PlayerKickEvent event) {
        notifyQQGroupWithMsg(event.getPlayer(), PlayerState.KICK);
    }

    private void notifyQQGroupWithMsg(Player player, PlayerState state) {
        try {
            ArrayList<Player> onlinePlayers = new ArrayList<>();

            Object[] objects = OrzMC.server().getOnlinePlayers().toArray();
            for(Object obj: objects) {
                if(obj instanceof Player) {
                    Player p = (Player)obj;
                    onlinePlayers.add(p);
                }
            }

            int onlinePlayerCount = onlinePlayers.size();
            int maxPlayerCount = OrzMC.server().getMaxPlayers();

            String playerName = ((TextComponent) player.displayName()).content();
            StringBuilder msgBuilder = new StringBuilder(playerName);

            boolean isMinusCurrentPlayer = false;
            switch (state) {
                case JOIN:
                {
                    msgBuilder.append("上线了!");
                }
                    break;
                case QUIT:
                {
                    isMinusCurrentPlayer = true;
                    msgBuilder.append("下线了!");
                }
                    break;
                case KICK:
                {
                    isMinusCurrentPlayer = true;
                    msgBuilder.append("被踢了!");
                }
                    break;
            }

            if(isMinusCurrentPlayer) {
                onlinePlayerCount -= 1;
            }

            msgBuilder.append("\n");
            String tip = String.format("------当前在线(%s/%d)------", onlinePlayerCount, maxPlayerCount);
            msgBuilder.append(tip);

            for(Player p: onlinePlayers) {
                if(p.getUniqueId() == player.getUniqueId() && isMinusCurrentPlayer) {
                    continue;
                }
                String name = ((TextComponent)p.displayName()).content();
                msgBuilder.append("\n").append(name);
            }
            sendQQGroupMsg(msgBuilder.toString());
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    private void sendQQGroupMsg(String msg) throws Exception {
        String groupId = "1056934080";
        String url = "http://localhost:8200/send_group_msg?group_id=" + groupId + "&message=" + URLEncoder.encode(msg,"utf-8");

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
