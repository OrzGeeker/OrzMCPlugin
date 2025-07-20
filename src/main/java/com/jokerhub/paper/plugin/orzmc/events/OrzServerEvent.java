package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.exception.ServerException;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.commands.OrzUserCmd;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class OrzServerEvent implements Listener {
    public static void notifyServerStop() {
        String minecraftVersion = OrzMC.server().getMinecraftVersion();
        String stringBuilder = "Minecraft " + minecraftVersion + "\n" +
                "------" + "\n" +
                "服务停止" +
                "\n\n" +
                "停止状态无法响应命令消息";
        OrzMC.sendPublicMessage(stringBuilder);
    }

    @EventHandler
    public void onException(ServerExceptionEvent event) {
        ServerException exception = event.getException();
        OrzMC.sendPrivateMessage(exception.toString());
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
        OrzMC.sendPublicMessage(stringBuilder.toString());
    }
}
