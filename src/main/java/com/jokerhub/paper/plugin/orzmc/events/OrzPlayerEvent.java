package com.jokerhub.paper.plugin.orzmc.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.commands.OrzGuideBook;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import net.kyori.adventure.text.Component;
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
import java.util.List;

public class OrzPlayerEvent implements Listener {

    private static JsonObject parseToJsonObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static String toPrettyFormat(JsonObject jsonObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonObject);
    }

    private static List<String> allowCountryList() {
        return OrzMC.config().getStringList("allow_country_code");
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        List<String> allowCountList = allowCountryList();
        if (allowCountList.isEmpty() && event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            return;
        }
        String ipAddress = event.getAddress().getHostAddress();
        String playerName = event.getPlayerProfile().getName();
        if (!ipAddress.isEmpty()) {
            try (HttpClient client = HttpClient.newHttpClient()) {
                // use ip parse service: https://www.geojs.io/docs/v1/endpoints/geo/
                String url = "https://get.geojs.io/v1/ip/geo/" + ipAddress + ".json";
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> {
                    OrzMC.debugInfo("Response Code : " + response.toString());
                    if (response.statusCode() == 200) {
                        String result = response.body();
                        JsonObject jsonObject = parseToJsonObject(result);
                        String addressInfo = toPrettyFormat(jsonObject);
                        String countryCode = String.valueOf(jsonObject.get("country_code").getAsString());
                        if (!allowCountList.contains(countryCode) && !ipAddress.equals("127.0.0.1")) {
                            String msg = playerName + "(" + ipAddress + ")" + "\n" + countryCode + "\n" + "IP位置不在服务支持区域" + String.join(",", allowCountList);
                            OrzMC.sendPublicMessage(msg + "\n" + addressInfo);
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(msg));
                        }
                    }
                }).exceptionally(e -> {
                    String msg = "IP地址解析服务异常: " + e.toString();
                    OrzMC.logger().warning(msg);
                    OrzMC.sendPublicMessage(msg);
                    return null;
                });
            } catch (Exception e) {
                String msg = e.toString();
                OrzMC.logger().severe(msg);
                OrzMC.sendPublicMessage(msg);
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
