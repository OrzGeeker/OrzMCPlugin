package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ServerEvent implements Listener {
    @EventHandler
    public void onException(ServerExceptionEvent event) {
        ServerException exception = event.getException();
        QQBotEvent.sendPrivateMsg(exception.toString());
    }
}
