package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TNTEvent implements Listener {
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) throws Exception {
        event.setCancelled(true);

        Entity entity = event.getPrimerEntity();
        if(entity == null) { return; }

        Location loc = entity.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        TextComponent msg = Component.text("坐标: ")
                .append(Component.text( x + " " + y + " " + z).color(TextColor.fromCSSHexString("#00FF00")))
                .append(Component.space())
                .append(Component.text("处有TNT被点燃！"));
        OrzMC.server().sendMessage(msg);

        String qqGroupMsg = "坐标: " + x + " " + y + " " + z + "处有TNT被点燃！";
        QQBotEvent.sendQQGroupMsg(qqGroupMsg);
    }
}
