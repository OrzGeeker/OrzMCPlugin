package com.jokerhub.paper.plugin.orzmc.commands;

import net.kyori.adventure.text.TextComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;


public class GuideBook implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            openNewPlayerGuideBook(player);
        }
        return false;
    }

    private void openNewPlayerGuideBook(Player player) {

        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) guideBook.getItemMeta();
        bookMeta.setTitle("新手指南");
        bookMeta.setAuthor("腐竹");

        BaseComponent[] page = new ComponentBuilder("欢迎新朋友来到我的世界！")
                .append("\n\n")
                .append("服务器中一些热爱创造的小伙伴在这里花费了大量心力建造出了各种漂亮的建筑，希望刚加入的朋友不要随意对其进行破坏，尊重他人的劳动成果。做一个有素质的MC玩家!")
                .append("\n\n")
                .append("相关链接").color(ChatColor.BOLD)
                .append("\n")
                .append("1. ").append("服务器主页").color(ChatColor.BLUE).underlined(true)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.jokerhub.cn"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击前往主页").create()))
                .append("\n").reset()
                .append("2. ").append("玩家手册").color(ChatColor.BLUE).underlined(true)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.jokerhub.cn/user/"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击查看玩家手册").create()))
                .append("\n").reset()
                .append("3. ").append("加入玩家QQ群").color(ChatColor.BLUE).underlined(true)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://jq.qq.com/?_wv=1027&k=DUEQuLE6"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击加入QQ群").create()))
                .create();
        bookMeta.spigot().addPage(page);

        guideBook.setItemMeta(bookMeta);
        player.openBook(guideBook);
        player.sendMessage("你获得了" + ((TextComponent) bookMeta.title()).content());
    }
}
