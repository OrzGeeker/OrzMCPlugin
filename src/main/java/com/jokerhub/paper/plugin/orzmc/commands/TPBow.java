package com.jokerhub.paper.plugin.orzmc.commands;

import com.destroystokyo.paper.utils.PaperPluginLogger;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TPBow implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;

            ItemStack teleport_bow = new ItemStack(Material.BOW, 1);
            ItemMeta meta = teleport_bow.getItemMeta();
            meta.addEnchant(Enchantment.ARROW_INFINITE,1,true);
            TextComponent name = Component.text("传送弓");
            meta.displayName(name);
            ArrayList<Component> loreList = new ArrayList<>();
            loreList.add(Component.text("可以把你传送到箭落地的位置"));
            meta.lore(loreList);
            teleport_bow.setItemMeta(meta);
            player.getInventory().addItem(teleport_bow);

            ItemStack arrow = new ItemStack(Material.ARROW, 1);
            player.getInventory().addItem(arrow);

        } else {
            OrzMC.logger().info("不是玩家，此命令无效！");
        }
        return false;
    }
}
