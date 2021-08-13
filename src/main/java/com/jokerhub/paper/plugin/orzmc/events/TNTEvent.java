package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TNTEvent implements Listener {
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) throws Exception {

        Block block = event.getBlock();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        event.setCancelled(true);
        TextComponent msg = Component.text("坐标: ")
                .append(Component.text( x + ", " + y + ", " + z).color(TextColor.fromCSSHexString("#00FF00")))
                .append(Component.space())
                .append(Component.text("处有TNT被点燃！"));
        OrzMC.server().sendMessage(msg);
        QQBotEvent.sendQQGroupMsg(msg.content());
    }
}
