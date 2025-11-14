package com.jokerhub.paper.plugin.orzmc.utils.bot;

import com.google.gson.Gson;
import com.jokerhub.paper.plugin.orzmc.OrzMC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class OrzLarkBot extends OrzBaseBot {
    public OrzLarkBot(OrzMC plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnable() {
        return botConfig.getBoolean("enable_lark_bot");
    }

    @Override
    public void setup() {

    }

    @Override
    public void teardown() {

    }

    public void sendMessage(String msg) {
        if (!this.isEnable()) {
            OrzMC.debugInfo("Lark Bot Disabled!");
            return;
        }
        try {
            String larkBotWebhookUrl = botConfig.getString("lark_bot_webhook");
            asyncHttpRequest(larkBotWebhookUrl, msg);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    @Override
    public void sendPrivateMessage(String message) {

    }

    private void asyncHttpRequest(String url, String msg) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("msg_type", "text");
        HashMap<String, String> content = new HashMap<>();
        content.put("text", msg);
        params.put("content", content);
        String postBodyJsonString = new Gson().toJson(params);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(postBodyJsonString)).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> OrzMC.debugInfo("Response : " + response.toString())).exceptionally(e -> {
                OrzMC.logger().severe("Lark机器人无法连接，工作异常: " + e.toString());
                return null;
            });
        } catch (Exception e) {
            OrzMC.logger().severe(e.toString());
        }
    }
}
