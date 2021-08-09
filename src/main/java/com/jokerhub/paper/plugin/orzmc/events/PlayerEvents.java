package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.TextComponent;
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
            String tip = String.format("------当前在线(%d/%d)------", onlinePlayerCount, maxPlayerCount);
            msgBuilder.append(tip);

            for(Player p: onlinePlayers) {
                if(p.getUniqueId() == player.getUniqueId() && isMinusCurrentPlayer) {
                    continue;
                }
                String name = ((TextComponent)p.displayName()).content();
                msgBuilder.append("\n").append(name);
                if(p.isOp()) {
                    msgBuilder.append("(op)");
                }
            }
            QQBotEvent.sendQQGroupMsg(msgBuilder.toString());
            if(onlinePlayerCount == 0) {
                QQBotEvent.sendPrivateMsg("服务器当前无玩家，可进行服务器维护");
            }
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }
}
