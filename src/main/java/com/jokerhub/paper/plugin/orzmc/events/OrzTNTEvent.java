package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.bot.OrzNotifier;
import com.jokerhub.paper.plugin.orzmc.bot.OrzQQBot;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

public class OrzTNTEvent implements Listener {
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {
        /* 处理TNT被点燃事件， 不在白名单坐标区域的TNT点燃后不会爆炸，TNT被点燃时，会全服通告 */
        /* 点燃方式：红石信号、打火石、火焰弹、火弓射击 */

        Block placedBlock = event.getBlock();
        if (hitInWhiteList(placedBlock)) return;
        else {
            event.setCancelled(true);
        }
        OrzMC.server().sendMessage(blockLocationInfo(placedBlock, "处有TNT被点燃！"));
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        /* 处理玩家放置方块事件，如果放置TNT，玩家会被全服通告，并发到QQ群里留底 */
        boolean shouldNotify = false;
        Block placedBlock = event.getBlockPlaced();
        Material placedBlockType = placedBlock.getType();
        switch (placedBlockType) {
            case TNT -> {
                if (hitInWhiteList(placedBlock)) return;
                shouldNotify = true;
            }
            case RESPAWN_ANCHOR -> shouldNotify = true;
            default -> {
            }
        }
        if (shouldNotify) {
            Player player = event.getPlayer();
            TextComponent msg = Component.text()
                    .append(playerInfo(player))
                    .append(Component.space())
                    .append(Component.text("在"))
                    .append(blockLocationInfo(placedBlock, "放置了 " + placedBlockType.name()))
                    .build();
            OrzMC.server().sendMessage(msg);
            String qqGroupMsg = OrzNotifier.playerDisplayName(player) + " 在" + locationString(placedBlock) + "放置了 " + placedBlockType.name();
            OrzQQBot.sendQQGroupMsg(qqGroupMsg);
        }
    }

    @EventHandler
    public void onBlockPreDispense(BlockPreDispenseEvent event) {
        /* 处理发射器发射物品事件，如果发射的是TNT，则取消发射，并全服通告 */
        /* 发射器直接发射TNT、发射器发射打火石点燃TNT、发射器发射火焰弹 */
        Block placedBlock = event.getBlock();
        ItemStack itemStack = event.getItemStack();
        if ((itemStack.getType() == Material.TNT || itemStack.getType() == Material.TNT_MINECART) && !hitInWhiteList(placedBlock)) {
            event.setCancelled(true);
            OrzMC.server().sendMessage(blockLocationInfo(event.getBlock(), "发射" + itemStack.getType().name() + "被禁止"));
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        String msg = locationString(block) + "处" + block.getType().name() + "爆炸";
        OrzQQBot.sendQQGroupMsg(msg);
        OrzMC.server().sendMessage(locationComponent(block).append(Component.text("处" + block.getType().name() + "爆炸")));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        String msg = locationString(event.getLocation()) + "处" + event.getEntityType().name() + "爆炸";
        OrzQQBot.sendQQGroupMsg(msg);
        OrzMC.server().sendMessage(locationComponent(event.getLocation()).append(Component.text("处" + event.getEntityType().name() + "爆炸")));
    }

    boolean hitInWhiteList(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        if (x >= 30746 && x <= 30808 && y >= 62 && z >= 10139 && z <= 10227) return true;
        OrzMC.logger().info(x + ", " + y + ", " + z + "处的" + block.getType().name() + "不在豁免区");
        return false;
    }

    TextComponent blockLocationInfo(Block block, String message) {
        return Component.text()
                .append(Component.text("坐标"))
                .append(Component.space())
                .append(locationComponent(block))
                .append(Component.space())
                .append(Component.text(message))
                .build();
    }

    TextComponent playerInfo(Player player) {
        String playerName = OrzNotifier.playerDisplayName(player);
        return Component.text()
                .append(Component.text(playerName).color(TextColor.fromHexString("#FF0000")))
                .build();
    }

    TextComponent locationComponent(Block block) {
        String blockLocation = locationString(block);
        TextComponent blockComponent = Component.text()
                .append(Component.text(blockLocation))
                .build()
                .clickEvent(ClickEvent.copyToClipboard(blockLocation))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标位置")))
                .color(TextColor.fromCSSHexString("#00FF00"));
        return Component.text().append(blockComponent).build();
    }

    String locationString(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        return " " + x + " " + y + " " + z + " ";
    }

    String locationString(Location location) {
        return " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + " ";
    }

    TextComponent locationComponent(Location location) {
        return locationComponent(location.getBlock());
    }
}
