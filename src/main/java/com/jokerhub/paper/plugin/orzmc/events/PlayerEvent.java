package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.bot.Notifier;
import com.jokerhub.paper.plugin.orzmc.commands.GuideBook;
import com.jokerhub.paper.plugin.orzmc.bot.QQBot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

public class PlayerEvent implements Listener {

    enum PlayerState {
        JOIN,
        QUIT,
        KICK
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GuideBook.giveNewPlayerAGuideBook(event.getPlayer());
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

        String playerName = Notifier.playerDisplayName(player);
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
            String name = Notifier.playerDisplayName(p);
            msgBuilder.append("\n").append(name);
        }
        QQBot.sendQQGroupMsg(msgBuilder.toString());
        OrzMC.logger().info(msgBuilder.toString());
        if (onlinePlayerCount == 0) {
            QQBot.sendPrivateMsg("服务器当前无玩家，可进行服务器维护");
        }
    }
}
