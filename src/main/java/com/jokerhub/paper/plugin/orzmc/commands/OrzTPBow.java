package com.jokerhub.paper.plugin.orzmc.commands;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class OrzTPBow implements CommandExecutor {

    public static final String name = "传送弓";

    public static Component logText(String content) {
        if(!content.isEmpty()) {
            return Component.text().append(Component.text("[" + OrzTPBow.name + "]")
                            .color(TextColor.fromCSSHexString("#00FF00")))
                    .append(Component.space())
                    .append(Component.text(content))
                    .build();
        }
        return Component.empty();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if(sender instanceof Player player) {

            ItemStack teleport_bow = new ItemStack(Material.BOW);
            ItemMeta meta = teleport_bow.getItemMeta();
            meta.addEnchant(Enchantment.INFINITY,1,true);
            TextComponent name = Component.text(OrzTPBow.name);
            meta.displayName(name);
            ArrayList<Component> loreList = new ArrayList<>();
            loreList.add(Component.text("可以把你传送到箭落地的位置"));
            meta.lore(loreList);
            teleport_bow.setItemMeta(meta);
            player.getInventory().addItem(teleport_bow);

            ItemStack arrow = new ItemStack(Material.ARROW);
            player.getInventory().addItem(arrow);
            player.sendMessage("你获得了" + OrzTPBow.name);

        } else {
            OrzMC.logger().info("不是玩家，此命令无效！");
        }
        return false;
    }
}
