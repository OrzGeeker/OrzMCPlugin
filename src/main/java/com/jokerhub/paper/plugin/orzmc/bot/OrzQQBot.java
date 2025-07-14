package com.jokerhub.paper.plugin.orzmc.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class OrzQQBot {

    public static boolean disable() {
        return !OrzMC.config().getBoolean("enable_qq_bot");
    }

    public static void processJsonStringPayload(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            if (json.get("group_id") == null || json.get("raw_message") == null) {
                return;
            }
            String groupId = json.get("group_id").getAsString();
            String message = json.get("raw_message").getAsString().trim();
            boolean isAdmin = json.get("sender").getAsJsonObject().get("role").getAsString().equals("admin");
            String qqGroupId = OrzMC.config().getString("qq_group_id");
            if (groupId.equals(qqGroupId)) {
                OrzNotifier.processMessage(message, isAdmin, info -> {
                    if (info != null) {
                        sendQQGroupMsg(info);
                    }
                });
            }
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public static void sendQQGroupMsg(String msg) {
        OrzLarkBot.sendMessage(msg);
        if (disable()) {
            return;
        }
        try {
            String groupId = OrzMC.config().getString("qq_group_id");
            String url = OrzMC.config().getString("qq_bot_api_server") + "/send_group_msg?group_id=" + groupId + "&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
            asyncHttpRequest(url);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public static void sendPrivateMsg(String msg) {
        if (disable()) {
            return;
        }
        try {
            String userId = OrzMC.config().getString("qq_admin_id");
            String url = OrzMC.config().getString("qq_bot_api_server") + "/send_msg?user_id=" + userId + "&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
            asyncHttpRequest(url);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    private static void asyncHttpRequest(String url) {
        new Thread(() -> {
            try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
                httpclient.start();
                HttpGet request = new HttpGet(url);
                Future<HttpResponse> future = httpclient.execute(request, null);
                HttpResponse response = future.get();
                if (OrzMC.enableDebug()) {
                    OrzMC.logger().info("Response Code : " + response.getStatusLine());
                }
            } catch (ExecutionException e) {
                OrzMC.logger().info("QQ机器人无法连接，工作异常");
            } catch (Exception e) {
                OrzMC.logger().info(e.toString());
            }
        }).start();
    }
}
