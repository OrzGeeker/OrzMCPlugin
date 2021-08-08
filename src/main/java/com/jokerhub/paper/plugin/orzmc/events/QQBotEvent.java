package com.jokerhub.paper.plugin.orzmc.events;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

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
            OrzMC.logger().info(json.toString());

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
            String name = ((TextComponent)p.displayName()).content();
            msgBuilder.append("\n").append(name);
        }
        try {
            sendQQGroupMsg(msgBuilder.toString());
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public static void sendQQGroupMsg(String msg) throws Exception {
        String groupId = "1056934080";
        String url = "http://localhost:8200/send_group_msg?group_id=" + groupId + "&message=" + URLEncoder.encode(msg,"utf-8");

        HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
        httpClient.setRequestMethod("GET");
        httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = httpClient.getResponseCode();
        OrzMC.logger().info("Sending 'GET' request to URL : " + url);
        OrzMC.logger().info("Response Code : " + responseCode);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            OrzMC.logger().info(response.toString());
        }
    }
}
