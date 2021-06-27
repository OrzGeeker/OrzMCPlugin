package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.events.demo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class OrzMC extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("OrzMC Plugin OnEnable!");
        getServer().getPluginManager().registerEvents(new demo(), this);
        getCommand("fly").setExecutor(new com.jokerhub.paper.plugin.orzmc.commands.demo());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("OrzMC Plugin OnDisable!");
    }

}
