package com.jokerhub.paper.plugin.orzmc.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class fly implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player) {
            Player player = (Player)sender;
            if(command.getName().equals("fly")) {
                player.setAllowFlight(true);
                player.sendMessage(ChatColor.AQUA + "you can fly");
            }
        }
        return false;
    }
}
