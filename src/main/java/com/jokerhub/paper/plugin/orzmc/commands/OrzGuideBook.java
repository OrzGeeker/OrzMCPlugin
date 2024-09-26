package com.jokerhub.paper.plugin.orzmc.commands;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class OrzGuideBook implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player player) {
            openNewPlayerGuideBook(player);
        }
        return false;
    }

    private static ItemStack guideBook() {
        ItemStack guideBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) guideBook.getItemMeta();
        bookMeta.setTitle("新手指南");
        bookMeta.setAuthor("腐竹");
        bookMeta.setGeneration(BookMeta.Generation.COPY_OF_COPY);

        Style linkStyle = Style.style()
                .color(TextColor.fromCSSHexString("#5555FF"))
                .decorate(TextDecoration.UNDERLINED)
                .build();
        TextComponent page2 = Component.text()
                .append(Component.text("欢迎新朋友来到我的世界！"))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("服务器中一些热爱创造的小伙伴在这里花费了大量心力建造出了各种漂亮的建筑，希望刚加入的朋友不要随意对其进行破坏，尊重他人的劳动成果。做一个有素质的MC玩家!"))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("相关链接").decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("1. "))
                .append(Component.text("服务器主页",linkStyle)
                        .clickEvent(ClickEvent.openUrl("https://minecraft.jokerhub.cn"))
                        .hoverEvent(HoverEvent.showText(Component.text("点击前往主页")))
                )
                .append(Component.newline())
                .append(Component.text("2. "))
                .append(Component.text("玩家手册",linkStyle)
                        .clickEvent(ClickEvent.openUrl("https://minecraft.jokerhub.cn/user/"))
                        .hoverEvent(HoverEvent.showText(Component.text("点击查看玩家手册")))
                )
                .append(Component.newline())
                .append(Component.text("3. "))
                .append(Component.text("加入玩家QQ群",linkStyle)
                        .clickEvent(ClickEvent.openUrl("https://jq.qq.com/?_wv=1027&k=DUEQuLE6"))
                        .hoverEvent(HoverEvent.showText(Component.text("点击加入QQ群")))
                )
                .build();
        bookMeta.addPages(page2);

        guideBook.setItemMeta(bookMeta);

        return guideBook;
    }
    private void openNewPlayerGuideBook(Player player) {
        player.openBook(guideBook());
    }

    public static void giveNewPlayerAGuideBook(Player player) {
        UUID playerUUID = player.getPlayerProfile().getId();
        if(playerUUID == null) return;

        OfflinePlayer offlinePlayer = OrzMC.server().getOfflinePlayer(playerUUID);
        if(!offlinePlayer.hasPlayedBefore()) {
            ItemStack guideBook = guideBook();
            player.getInventory().addItem(guideBook);
            player.sendMessage("获得新手指南");
        }
    }
}
