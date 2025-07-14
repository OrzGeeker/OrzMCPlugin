package com.jokerhub.paper.plugin.orzmc.bot;

import com.google.gson.Gson;
import com.jokerhub.paper.plugin.orzmc.OrzMC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class OrzLarkBot {

    public static boolean enable() {
        return OrzMC.config().getBoolean("enable_lark_bot");
    }

    public static void sendMessage(String msg) {
        if (!enable()) {
            OrzMC.logger().info("Lark Bot Disabled!");
            return;
        }
        try {
            String url = larkBotWebhook();
            asyncHttpRequest(url, msg);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    private static void asyncHttpRequest(String url, String msg) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("msg_type", "text");
        HashMap<String, String> content = new HashMap<>();
        content.put("text", msg);
        params.put("content", content);
        String postBodyJsonString = new Gson().toJson(params);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(postBodyJsonString))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAcceptAsync(response -> {
                        if (OrzMC.enableDebug()) {
                            OrzMC.logger().info("Response : " + response.toString());
                        }
                    })
                    .exceptionally(e -> {
                        OrzMC.logger().severe("Lark机器人无法连接，工作异常: " + e.toString());
                        return null;
                    });
        } catch (Exception e) {
            OrzMC.logger().severe(e.toString());
        }
    }

    private static String larkBotWebhook() {
        return OrzMC.config().getString("lark_bot_webhook");
    }
}
