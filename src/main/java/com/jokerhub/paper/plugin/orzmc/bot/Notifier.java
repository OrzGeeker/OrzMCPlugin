package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Notifier {

    public static String processMessage(String message, Boolean isAdmin) {

        if (!message.startsWith("/")) return null;

        String info = null;

        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(message.split("[, ]+")));
        String cmdName = cmd.removeFirst();
        Set<String> userNameSet = new HashSet<>(cmd);

        // 普通命令
        if (cmdName.contains("/list")) {
            info = onlinePlayersInfo();
        } else if (cmdName.contains("/wl")) {
            info = whiteListInfo();
        } else if (cmdName.contains("/?")) {
            info = cmdHelpInfo();
        }
        // 管理员命令
        else if (cmdName.contains("/wa") && isAdmin) {
            info = addWhiteListInfo(userNameSet);
        }
        // 管理员命令
        else if (cmdName.contains("/wr") && isAdmin) {
            info = removeWhiteListInfo(userNameSet);
        }

        return info;
    }

    public static String playerDisplayName(Player player) {
        String ret = player.getPlayerProfile().getName();

        if (player.isOp()) {
            ret += "(op)";
        }

        String gameMode = "";
        switch (player.getGameMode()) {
            case CREATIVE -> gameMode = "创造";
            case SURVIVAL -> gameMode = "生存";
            case ADVENTURE -> gameMode = "冒险";
            case SPECTATOR -> gameMode = "观察";
            default -> {
            }
        }
        ret += " " + gameMode + "模式";
        return ret;
    }

    private static String onlinePlayersInfo() {
        ArrayList<Player> onlinePlayers = new ArrayList<>();
        Object[] objects = OrzMC.server().getOnlinePlayers().toArray();
        for (Object obj : objects) {
            if (obj instanceof Player p) {
                onlinePlayers.add(p);
            }
        }

        String tip = String.format("------当前在线(%d/%d)------", onlinePlayers.size(), OrzMC.server().getMaxPlayers());
        StringBuilder msgBuilder = new StringBuilder(tip);

        for (Player p : onlinePlayers) {
            String name = Notifier.playerDisplayName(p);
            msgBuilder.append("\n").append(name);
        }

        return msgBuilder.toString();
    }

    public static String cmdHelpInfo() {
        return """
                👨‍💼 管理员命令：
                /wa\t添加玩家到服务器白名单中
                /wr\t从服务器白名单中移除玩家
                👨🏻‍💻 通用命令:\s
                /list\t查看当前在线玩家
                /wl\t查看当前在白名单中的玩家
                /?\t查看QQ群中可以使用的命令信息
                """;
    }

    private static String whiteListInfo() {
        ArrayList<OfflinePlayer> whiteListPlayers = allWhiteListPlayer();
        StringBuilder whiteListInfo = new StringBuilder(String.format("------当前白名单玩家(%d)------", whiteListPlayers.size()));
        for (OfflinePlayer player : whiteListPlayers) {
            String playerName = player.getName();
            String lastSeen = new SimpleDateFormat("MM/dd/ HH:mm").format(new Date(player.getLastSeen()));
            String isOnline = player.isOnline() ? "•" : "◦";
            whiteListInfo.append("\n").append(isOnline).append(" ").append(playerName).append(" ").append(String.format("%s", lastSeen));
        }
        return whiteListInfo.toString();
    }

    private static String addWhiteListInfo(Set<String> userNames) {
        for (String userName : userNames) {
            String addUserCmd = String.format("whitelist add %s", userName);
            OrzMC.server().dispatchCommand(OrzMC.server().getConsoleSender(), addUserCmd);
        }
        OrzMC.server().reloadWhitelist();
        Set<String> allWhiteListName = allWhiteListPlayerName();
        String message = "------白名单添加------\n";
        if (allWhiteListName.containsAll(userNames)) {
            message += String.join("\n", userNames.stream().map(name -> "✔︎ ︎" + name).collect(Collectors.toSet()));
        }
        userNames.removeAll(allWhiteListName);
        if (!userNames.isEmpty()) {
            message += String.join("\n", userNames.stream().map(name -> "✘ " + name).collect(Collectors.toSet()));
        }

        return message;
    }

    private static String removeWhiteListInfo(Set<String> userNames) {
        for (String userName : userNames) {
            String addUserCmd = String.format("whitelist remove %s", userName);
            OrzMC.server().dispatchCommand(OrzMC.server().getConsoleSender(), addUserCmd);
        }
        OrzMC.server().reloadWhitelist();
        Set<String> allWhiteListName = allWhiteListPlayerName();
        String message = "------白名单移除------\n";
        if (!allWhiteListName.containsAll(userNames)) {
            message += String.join("\n", userNames.stream().map(name -> "✔︎ " + name).collect(Collectors.toSet()));
        }
        userNames.retainAll(allWhiteListName);
        if (!userNames.isEmpty()) {
            message += String.join("\n", userNames.stream().map(name -> "✘ " + name).collect(Collectors.toSet()));
        }
        return message;
    }

    private static ArrayList<OfflinePlayer> allWhiteListPlayer() {
        ArrayList<OfflinePlayer> whiteListPlayers = new ArrayList<>(OrzMC.server().getWhitelistedPlayers());
        whiteListPlayers.sort((o1, o2) -> Long.compare(o2.getLastSeen(), o1.getLastSeen()));
        return whiteListPlayers;
    }

    private static Set<String> allWhiteListPlayerName() {
        return allWhiteListPlayer().stream().map(OfflinePlayer::getName).collect(Collectors.toSet());
    }
}
