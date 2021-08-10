package com.jokerhub.paper.plugin.orzmc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuideBook implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(commandSender instanceof Player) {
            Player player = (Player) commandSender;
            giveNewPlayerGuideBook(player);
        }
        return false;
    }

    private void giveNewPlayerGuideBook(Player player) {
        player.sendMessage("你获得了新手指南");
    }
}
