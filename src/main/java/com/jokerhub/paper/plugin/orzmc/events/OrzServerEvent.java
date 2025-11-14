package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.commands.OrzUserCmd;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerLoadEvent;

public class OrzServerEvent extends OrzBaseListener {
    public OrzServerEvent(OrzMC plugin) {
        super(plugin);
    }

    @EventHandler
    public void onException(ServerExceptionEvent event) {
        ServerException exception = event.getException();
        plugin.sendPrivateMessage(exception.toString());
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        String minecraftVersion = OrzMC.server().getMinecraftVersion();
        StringBuilder stringBuilder = new StringBuilder("Minecraft " + minecraftVersion).append("\n");
        stringBuilder.append("------").append("\n");
        switch (event.getType()) {
            case STARTUP -> stringBuilder.append("启动完成");
            case RELOAD -> stringBuilder.append("重启完成");
        }
        stringBuilder.append("\n\n");
        stringBuilder.append("发送 \"").append(OrzUserCmd.SHOW_HELP.getCmdString()).append("\" 查看支持的命令消息");
        plugin.sendPublicMessage(stringBuilder.toString());
    }
}
