package com.jokerhub.paper.plugin.orzmc.events;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.Future;

public class QQBotEvent implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            InputStream is = exchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
            // 请求的JSON参数解析
            String jsonString = sb.toString();
            JSONParser jsonParser = new JSONParser();
            JSONObject json = (JSONObject) jsonParser.parse(jsonString);

            String groupId = json.get("group_id").toString();
            String message = json.get("raw_message").toString();
            if(groupId.equals("1056934080") && message.contains("/list")) {
                notifyQQGroupOnlinePlayers();
            }

            exchange.sendResponseHeaders(200,0);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    private void notifyQQGroupOnlinePlayers() {
        ArrayList<Player> onlinePlayers = new ArrayList<>();
        Object[] objects = OrzMC.server().getOnlinePlayers().toArray();
        for(Object obj: objects) {
            if(obj instanceof Player) {
                Player p = (Player)obj;
                onlinePlayers.add(p);
            }
        }

        String tip = String.format("------当前在线(%d/%d)------", onlinePlayers.size(), OrzMC.server().getMaxPlayers());
        StringBuilder msgBuilder = new StringBuilder(tip);

        for(Player p: onlinePlayers) {
            String name = playerQQDisplayName(p);
            msgBuilder.append("\n").append(name);
        }
        try {
            sendQQGroupMsg(msgBuilder.toString());
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public static String playerQQDisplayName(Player player) {
        String ret=player.getPlayerProfile().getName();
        if(player.isOp()) {
            ret += "(op)";
        }
        return ret;
    }

    public static void sendQQGroupMsg(String msg) throws Exception {
        String groupId = "1056934080";
        String url = "http://localhost:8200/send_group_msg?group_id=" + groupId + "&message=" + URLEncoder.encode(msg,"utf-8");
        asyncHttpRequest(url);
    }

    public static void sendPrivateMsg(String msg) throws Exception {
        String userId = "824219521";
        String url = "http://localhost:8200/send_msg?user_id=" + userId + "&message=" + URLEncoder.encode(msg,"utf-8");
        asyncHttpRequest(url);
    }

    public  static void asyncHttpRequest(String url) throws Exception {
        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
            httpclient.start();
            HttpGet request = new HttpGet(url);
            Future<HttpResponse> future = httpclient.execute(request, null);
            HttpResponse response = future.get();
            OrzMC.logger().info("Response Code : " + response.getStatusLine());
        }
    }
}
