package com.jokerhub.paper.plugin.orzmc.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import com.jokerhub.paper.plugin.orzmc.commands.OrzGuideBook;
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

    public String getAddressOfIPv4(String ipv4Address) {
        String ret = "";
        if (!ipv4Address.isEmpty()) {
            try (HttpClient httpclient = HttpClient.newHttpClient()) {
                String url = "https://www.90th.cn/api/ip?key=1c9ac0159c94d8d0&ip=" + ipv4Address;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();
                HttpResponse<String> response = httpclient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String result = response.body();
                    ret = toPrettyFormat(result);
                }
            } catch (Exception e) {
                ret = e.toString();
            }
        }
        return ret;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        String playerName = event.getPlayerProfile().getName();
        String ipAddress = event.getAddress().getHostAddress();
        String resultDesc = event.getLoginResult().toString();
        String addressInfo = getAddressOfIPv4(ipAddress);
        String qqMsg =
                "--- " + resultDesc + " ---" + "\n"
                        + playerName + "(" + ipAddress + ")" + "\n"
                        + addressInfo;
        OrzMC.qqBot.sendQQGroupMsg(qqMsg);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        OrzGuideBook.giveNewPlayerAGuideBook(event.getPlayer());
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
        OrzMC.qqBot.sendQQGroupMsg(msgBuilder.toString());
        OrzMC.logger().info(msgBuilder.toString());
        if (onlinePlayerCount == 0) {
            OrzMC.qqBot.sendPrivateMsg("服务器当前无玩家，可进行服务器维护");
        }
    }

    enum PlayerState {
        JOIN,
        QUIT,
        KICK
    }
}
