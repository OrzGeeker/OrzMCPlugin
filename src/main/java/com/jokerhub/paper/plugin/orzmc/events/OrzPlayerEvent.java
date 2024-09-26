package com.jokerhub.paper.plugin.orzmc.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.bot.OrzNotifier;
import com.jokerhub.paper.plugin.orzmc.bot.OrzQQBot;
import com.jokerhub.paper.plugin.orzmc.commands.OrzGuideBook;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                String url = "https://www.90th.cn/api/ip?key=1c9ac0159c94d8d0&ip=" + ipv4Address;
                HttpGet request = new HttpGet(url);
                CloseableHttpResponse response = httpclient.execute(request);
                StatusLine status = response.getStatusLine();
                if (status.getStatusCode() == HttpStatus.SC_OK) {
                    String result = EntityUtils.toString(response.getEntity());
                    ret = toPrettyFormat(result);
                }
            } catch (Exception e) {
                ret = e.toString();
            }
        }
        return ret;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String ipAddress = event.getAddress().getHostAddress();
        String resultDesc = event.getResult().toString();
        String addressInfo = getAddressOfIPv4(ipAddress);
        String qqMsg =
                "--- " + resultDesc + " ---" + "\n"
                + playerName + "(" + ipAddress + ")" + "\n"
                + addressInfo;

        OrzQQBot.sendQQGroupMsg(qqMsg);
    }
    enum PlayerState {
        JOIN,
        QUIT,
        KICK
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

        String playerName = OrzNotifier.playerDisplayName(player);
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
            String name = OrzNotifier.playerDisplayName(p);
            msgBuilder.append("\n").append(name);
        }
        OrzQQBot.sendQQGroupMsg(msgBuilder.toString());
        OrzMC.logger().info(msgBuilder.toString());
        if (onlinePlayerCount == 0) {
            OrzQQBot.sendPrivateMsg("服务器当前无玩家，可进行服务器维护");
        }
    }
}
