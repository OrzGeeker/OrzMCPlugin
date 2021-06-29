package com.jokerhub.paper.plugin.orzmc.commands;

import org.bukkit.ChatColor;
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

public class teleport implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(player.hasPermission("orzmc.teleport")) {
                ItemStack teleport_bow = new ItemStack(Material.BOW);
                teleport_bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 666);

                ItemMeta meta = teleport_bow.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Teleport Bow");
                meta.setUnbreakable(true);
                ArrayList<String> lore = new ArrayList<>();
                lore.add("This is a bow that can teleport you whereever the arrow decides to land.");
                meta.setLore(lore);
                teleport_bow.setItemMeta(meta);

                player.getInventory().addItem(teleport_bow);
            }
        } else {
            System.out.println("You are not a player, You can not do this");
        }
        return false;
    }
}
