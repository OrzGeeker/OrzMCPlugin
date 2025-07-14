package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OrzNotifier {

    public static void processMessage(String message, Boolean isAdmin, Consumer<String> callback) {

        if (!message.startsWith("/")) return;

        String info = null;

        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(message.split("[, ]+")));
        String cmdName = cmd.removeFirst();
        Set<String> userNameSet = new HashSet<>(cmd);

        // 普通命令
        if (cmdName.contains("/list")) {
            callback.accept(onlinePlayersInfo());
        } else if (cmdName.contains("/wl")) {
            callback.accept(whiteListInfo());
        } else if (cmdName.contains("/?")) {
            callback.accept(cmdHelpInfo());
        }
        // 管理员命令
        else if (cmdName.contains("/wa")) {
            addWhiteListInfo(cmdName, isAdmin, userNameSet, callback);
        }
        // 管理员命令
        else if (cmdName.contains("/wr")) {
            removeWhiteListInfo(cmdName, isAdmin, userNameSet, callback);
        }
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
            String name = OrzNotifier.playerDisplayName(p);
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

    public static String adminPermissionRequiredTip(String cmd) {
        return cmd + "命令需要群管理员权限";
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

    private static void addWhiteListInfo(String cmdName, boolean isAdmin, Set<String> userNames, Consumer<String> callback) {
        if (!isAdmin) {
            callback.accept(adminPermissionRequiredTip(cmdName));
            return;
        }
        // 主线程上执行白名单操作
        OrzMC.server().getScheduler().runTask(OrzMC.plugin(), () -> {
            for (String userName : userNames) {
                OfflinePlayer player = OrzMC.server().getOfflinePlayer(userName);
                player.setWhitelisted(true);
            }
            // 回调异步执行
            OrzMC.server().getScheduler().runTaskAsynchronously(OrzMC.plugin(), () -> {
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
                callback.accept(message);
            });
        });
    }

    private static void removeWhiteListInfo(String cmdName, boolean isAdmin, Set<String> userNames, Consumer<String> callback) {
        if (!isAdmin) {
            callback.accept(adminPermissionRequiredTip(cmdName));
            return;
        }
        // 主线程上执行白名单操作
        OrzMC.server().getScheduler().runTask(OrzMC.plugin(), () -> {
            for (String userName : userNames) {
                OfflinePlayer player = OrzMC.server().getOfflinePlayer(userName);
                player.setWhitelisted(false);
            }
            // 回调异步执行
            OrzMC.server().getScheduler().runTaskAsynchronously(OrzMC.plugin(), () -> {
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
                callback.accept(message);
            });
        });
    }

    private static ArrayList<OfflinePlayer> allWhiteListPlayer() {
        ArrayList<OfflinePlayer> whiteListPlayers = new ArrayList<>(OrzMC.server().getWhitelistedPlayers());
        whiteListPlayers.sort((o1, o2) -> Long.compare(o2.getLastSeen(), o1.getLastSeen()));
        return whiteListPlayers;
    }

    private static Set<String> allWhiteListPlayerName() {
        return allWhiteListPlayer().stream().map(OfflinePlayer::getName).collect(Collectors.toSet());
    }

    private static void performCommandFromConsoleSender(String cmd) {
        OrzMC.server().getScheduler().runTask(OrzMC.plugin(), () -> OrzMC.server().dispatchCommand(OrzMC.server().getConsoleSender(), cmd));
    }
}
