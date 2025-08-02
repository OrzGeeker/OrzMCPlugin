package com.jokerhub.paper.plugin.orzmc.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.commands.OrzGuideBook;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class OrzPlayerEvent implements Listener {

    private static String toPrettyFormat(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonObject);
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        String ipAddress = event.getAddress().getHostAddress();
        String playerName = event.getPlayerProfile().getName();
        String resultDesc = event.getLoginResult().toString();
        if (!ipAddress.isEmpty()) {
            try (HttpClient client = HttpClient.newHttpClient()) {
                // use ip parse service: https://www.geojs.io/docs/v1/endpoints/geo/
                String url = "https://get.geojs.io/v1/ip/geo/" + ipAddress + ".json";
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> {
                    OrzMC.debugInfo("Response Code : " + response.toString());
                    if (response.statusCode() == 200) {
                        String result = response.body();
                        String addressInfo = toPrettyFormat(result);
                        String qqMsg = "--- " + resultDesc + " ---" + "\n" + playerName + "(" + ipAddress + ")" + "\n" + addressInfo;
                        OrzMC.sendPublicMessage(qqMsg);
                    }
                }).exceptionally(e -> {
                    OrzMC.logger().warning("IP地址解析服务异常: " + e.toString());
                    return null;
                });
            } catch (Exception e) {
                OrzMC.logger().severe(e.toString());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        OrzGuideBook.giveNewPlayerAGuideBook(event.getPlayer());
        notifyPlayerChatGroupWithMsg(event.getPlayer(), PlayerState.JOIN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        notifyPlayerChatGroupWithMsg(event.getPlayer(), PlayerState.QUIT);
    }

    @EventHandler
    public void onPlayerKickLeave(PlayerKickEvent event) {
        notifyPlayerChatGroupWithMsg(event.getPlayer(), PlayerState.KICK);
    }

    private void notifyPlayerChatGroupWithMsg(Player player, PlayerState state) {
        ArrayList<Player> onlinePlayers = new ArrayList<>();

        Object[] objects = OrzMC.server().getOnlinePlayers().toArray();
        for (Object obj : objects) {
            if (obj instanceof Player p) {
                onlinePlayers.add(p);
            }
        }

        int onlinePlayerCount = onlinePlayers.size();
        int maxPlayerCount = OrzMC.server().getMaxPlayers();

        String playerName = OrzMessageParser.playerDisplayName(player);
        StringBuilder msgBuilder = new StringBuilder(playerName).append(" ");

        boolean isMinusCurrentPlayer = false;
        switch (state) {
            case JOIN -> msgBuilder.append("上线");
            case QUIT -> {
                isMinusCurrentPlayer = true;
                msgBuilder.append("下线");
            }
            case KICK -> {
                isMinusCurrentPlayer = true;
                msgBuilder.append("被踢");
            }
            default -> {
            }
        }

        if (isMinusCurrentPlayer) {
            onlinePlayerCount -= 1;
        }

        msgBuilder.append("\n");
        String tip = String.format("------当前在线(%d/%d)------", onlinePlayerCount, maxPlayerCount);
        msgBuilder.append(tip);

        for (Player p : onlinePlayers) {
            if (p.getUniqueId() == player.getUniqueId() && isMinusCurrentPlayer) {
                continue;
            }
            String name = OrzMessageParser.playerDisplayName(p);
            msgBuilder.append("\n").append(name);
        }
        OrzMC.sendPublicMessage(msgBuilder.toString());
        OrzMC.logger().info(msgBuilder.toString());
        if (onlinePlayerCount == 0) {
            OrzMC.sendPrivateMessage("服务器当前无玩家，可进行服务器维护");
        }
    }

    enum PlayerState {
        JOIN, QUIT, KICK
    }
}
