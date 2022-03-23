package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class TNTEvent implements Listener {
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {
        /* 处理TNT被点燃事件， 不在白名单坐标区域的TNT点燃后不会爆炸，TNT被点燃时，会全服通告 */
        /* 点燃方式：红石信号、打火石、火焰弹、火弓射击 */

        Block placedBlock = event.getBlock();
        int x = placedBlock.getX();
        int y = placedBlock.getY();
        int z = placedBlock.getZ();

        if (hitInWhiteList(x, y, z)) return;
        else {
            event.setCancelled(true);
        }

        OrzMC.server().sendMessage(blockLocationInfo(placedBlock,"处有TNT被点燃！"));
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) throws Exception {
        /* 处理玩家放置方块事件，如果放置TNT，玩家会被全服通告，并发到QQ群里留底 */
        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getType() == Material.TNT || placedBlock.getType() == Material.TNT_MINECART) {

            int x = placedBlock.getX();
            int y = placedBlock.getY();
            int z = placedBlock.getZ();

            if (hitInWhiteList(x, y, z)) return;

            Player player = event.getPlayer();
            String playerName = QQBotEvent.playerQQDisplayName(player);
            TextComponent msg = Component.text()
                    .append(Component.text(playerName).color(TextColor.fromHexString("#FF0000")))
                    .append(Component.space())
                    .append(Component.text("在"))
                    .append(Component.space())
                    .append(Component.text()
                            .append(Component.text(x))
                            .append(Component.space())
                            .append(Component.text(y))
                            .append(Component.space())
                            .append(Component.text(z))
                            .build()
                            .clickEvent(ClickEvent.copyToClipboard(placedBlock.getX() + " " + placedBlock.getY() + " " + placedBlock.getZ()))
                            .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标位置")))
                            .color(TextColor.fromCSSHexString("#00FF00")))
                    .append(Component.space())
                    .append(Component.text("处放置了TNT"))
                    .build();
            OrzMC.server().sendMessage(msg);

            String qqGroupMsg = playerName + " 在 " + x + " " + y + " " + z + " 放置过" + placedBlock.getType().name();
            QQBotEvent.sendQQGroupMsg(qqGroupMsg);
        }
    }

    @EventHandler
    public void onBlockPreDispense(BlockPreDispenseEvent event) {
        /* 处理发射器发射物品事件，如果发射的是TNT，则取消发射，并全服通告 */
        /* 发射器直接发射TNT、发射器发射打火石点燃TNT、发射器发射火焰弹 */
        Block placedBlock = event.getBlock();
        int x = placedBlock.getX();
        int y = placedBlock.getY();
        int z = placedBlock.getZ();
        ItemStack itemStack = event.getItemStack();
        if ((itemStack.getType() == Material.TNT || itemStack.getType() == Material.TNT_MINECART) && !hitInWhiteList(x, y, z)) {
            event.setCancelled(true);
            OrzMC.server().sendMessage(blockLocationInfo(event.getBlock(), "发射TNT被禁止"));
        }
    }

    boolean hitInWhiteList(int x, int y, int z) {
        if (x >= 30746 && x <= 30808 && y >= 62 && z >= 10139 && z <= 10227) return true;
        OrzMC.server().getLogger().info("TNT没命中白名单：" + x + ", " + y + ", " + z);
        return false;
    }

    TextComponent blockLocationInfo(Block block, String message) {

        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        return Component.text()
                .append(Component.text("坐标"))
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
                .append(Component.text(message))
                .build();
    }
}
