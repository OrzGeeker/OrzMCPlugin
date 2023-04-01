package com.jokerhub.paper.plugin.orzmc.qqbot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class QQBotEvent implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            InputStream is = exchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            // 请求的JSON参数解析
            String jsonString = sb.toString();
            JSONParser jsonParser = new JSONParser();
            JSONObject json = (JSONObject) jsonParser.parse(jsonString);

            String groupId = json.get("group_id").toString();
            String message = json.get("raw_message").toString().trim();
            boolean isAdmin = ((JSONObject)json.get("sender")).get("role").toString().equals("admin");
            String qqGroupId = OrzMC.config().getString("qq_group_id");
            if (groupId.equals(qqGroupId) && message.startsWith("/")) {
                ArrayList<String> cmd = new ArrayList<>(Arrays.asList(message.split("[, ]+")));
                String cmdName = cmd.remove(0);
                Set<String> userNameSet = new HashSet<>(cmd);
                // 普通命令

                if(cmdName.contains("/list")) {
                    notifyQQGroupOnlinePlayers();
                }
                else if(cmdName.contains("/wl")) {
                    showWhiteList();
                }
                else if(cmdName.contains("/?")) {
                    showQQCmdHelp();
                }
                // 管理员命令
                if(isAdmin) {
                    if(cmdName.contains("/wa")) {
                        addWhiteList(userNameSet);
                    }
                    else if(cmdName.contains("/wr")) {
                        removeWhiteList(userNameSet);
                    }
                }
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }
    private void showQQCmdHelp() {
        String cmdHelp = """
                👨‍💼 管理员命令：
                /wa\t添加玩家到服务器白名单中
                /wr\t从服务器白名单中移除玩家
                👨🏻‍💻 通用命令:\s
                /list\t查看当前在线玩家
                /wl\t查看当前在白名单中的玩家
                /?\t查看QQ群中可以使用的命令信息
                """;
        sendQQGroupMsg(cmdHelp);
    }
    private void showWhiteList() {
        ArrayList<OfflinePlayer> whiteListPlayers = allWhiteListPlayer();
        StringBuilder whiteListInfo = new StringBuilder(String.format("------当前白名单玩家(%d)------", whiteListPlayers.size()));
        for(OfflinePlayer player: whiteListPlayers) {
            String playerName = player.getName();
            String lastSeen = new SimpleDateFormat("MM/dd/ HH:mm").format(new Date(player.getLastSeen()));
            String isOnline = player.isOnline() ? "•" : "◦";
            whiteListInfo.append("\n")
                    .append(isOnline)
                    .append(" ")
                    .append(playerName)
                    .append(" ")
                    .append(String.format("%s",lastSeen));
        }
        sendQQGroupMsg(whiteListInfo.toString());
    }

    private void addWhiteList(Set<String> userNames) {
        for (String userName: userNames) {
            String addUserCmd = String.format("whitelist add %s", userName);
            OrzMC.server().dispatchCommand(OrzMC.server().getConsoleSender(), addUserCmd);
        }
        OrzMC.server().reloadWhitelist();
        Set<String> allWhiteListName = allWhiteListPlayerName();
        String message = "------白名单添加------\n";
        if(allWhiteListName.containsAll(userNames)) {
            message += String.join("\n", userNames.stream().map(name -> "✔︎ ︎" + name).collect(Collectors.toSet()));
        }
        userNames.removeAll(allWhiteListName);
        if(userNames.size() > 0) {
            message += String.join("\n", userNames.stream().map(name -> "✘ " + name).collect(Collectors.toSet()));
        }
        sendQQGroupMsg(message);
    }

    private void removeWhiteList(Set<String> userNames) {
        for (String userName: userNames) {
            String addUserCmd = String.format("whitelist remove %s", userName);
            OrzMC.server().dispatchCommand(OrzMC.server().getConsoleSender(), addUserCmd);
        }
        OrzMC.server().reloadWhitelist();
        Set<String> allWhiteListName = allWhiteListPlayerName();
        String message = "------白名单移除------\n";
        if(!allWhiteListName.containsAll(userNames)) {
            message += String.join("\n", userNames.stream().map(name -> "✔︎ " + name).collect(Collectors.toSet()));
        }
        userNames.retainAll(allWhiteListName);
        if(userNames.size() > 0){
            message += String.join("\n", userNames.stream().map(name -> "✘ " + name).collect(Collectors.toSet()));
        }
        sendQQGroupMsg(message);
    }

    private ArrayList<OfflinePlayer> allWhiteListPlayer() {
        ArrayList<OfflinePlayer> whiteListPlayers = new ArrayList<>(OrzMC.server().getWhitelistedPlayers());
        whiteListPlayers.sort((o1, o2) -> Long.compare(o2.getLastSeen(), o1.getLastSeen()));
        return whiteListPlayers;
    }

    private Set<String> allWhiteListPlayerName() {
        return allWhiteListPlayer().stream().map(OfflinePlayer::getName).collect(Collectors.toSet());
    }

    private void notifyQQGroupOnlinePlayers() {
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
            String name = playerQQDisplayName(p);
            msgBuilder.append("\n").append(name);
        }
        sendQQGroupMsg(msgBuilder.toString());

    }

    public static String playerQQDisplayName(Player player) {
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

    public static void sendQQGroupMsg(String msg) {
        try {
            String groupId = OrzMC.config().getString("qq_group_id");
            String url = "http://" + OrzMC.config().getString("qq_bot_api_host") + "/send_group_msg?group_id=" + groupId + "&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
            asyncHttpRequest(url);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public static void sendPrivateMsg(String msg) {
        try {
            String userId = OrzMC.config().getString("qq_admin_id");
            String url = "http://" + OrzMC.config().getString("qq_bot_api_host") + "/send_msg?user_id=" + userId + "&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
            asyncHttpRequest(url);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public static void asyncHttpRequest(String url) {
        new Thread(() -> {
            try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
                httpclient.start();
                HttpGet request = new HttpGet(url);
                Future<HttpResponse> future = httpclient.execute(request, null);
                HttpResponse response = future.get();
                OrzMC.logger().info("Response Code : " + response.getStatusLine());
            } catch (ExecutionException e) {
                OrzMC.logger().info("QQ机器人无法连接，工作异常");
            } catch (Exception e) {
                OrzMC.logger().info(e.toString());
            }
        }).start();
    }
}
