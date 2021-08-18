package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

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

        TextComponent msg = Component.text()
                .append(Component.text("坐标:"))
                .append(Component.space())
                .append(Component.text()
                        .append(Component.text(x))
                        .append(Component.space())
                        .append(Component.text(y))
                        .append(Component.space())
                        .append(Component.text(z))
                        .build()
                        .clickEvent(ClickEvent.copyToClipboard(x + " " + y + " " + z))
                        .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标位置")))
                        .color(TextColor.fromCSSHexString("#00FF00")))
                .append(Component.space())
                .append(Component.text("处有TNT被点燃！"))
                .build();
        OrzMC.server().sendMessage(msg);

        OrzMC.logger().info("test: " + msg.content());

        String qqGroupMsg = "坐标: " + x + " " + y + " " + z + "处有TNT被点燃！";
        QQBotEvent.sendQQGroupMsg(qqGroupMsg);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if(placedBlock.getType() == Material.TNT || placedBlock.getType() == Material.TNT_MINECART) {
            Player player = event.getPlayer();
            String playerName = player.getPlayerProfile().getName();
            if(playerName == null) return;
            TextComponent msg = Component.text()
                    .append(Component.text(playerName).color(TextColor.fromHexString("#FF0000")))
                    .append(Component.space())
                    .append(Component.text("在"))
                    .append(Component.space())
                    .append(Component.text()
                            .append(Component.text(placedBlock.getX()))
                            .append(Component.space())
                            .append(Component.text(placedBlock.getY()))
                            .append(Component.space())
                            .append(Component.text(placedBlock.getZ()))
                            .build()
                            .clickEvent(ClickEvent.copyToClipboard(placedBlock.getX() + " " + placedBlock.getY() + " " + placedBlock.getZ()))
                            .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标位置")))
                            .color(TextColor.fromCSSHexString("#00FF00")))
                    .append(Component.space())
                    .append(Component.text("处放置了TNT"))
                    .build();
            OrzMC.server().sendMessage(msg);
        }
    }
}
