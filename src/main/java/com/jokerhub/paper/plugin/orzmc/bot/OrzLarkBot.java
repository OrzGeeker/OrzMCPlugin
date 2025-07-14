package com.jokerhub.paper.plugin.orzmc.bot;

import com.google.gson.Gson;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
        new Thread(() -> {
            try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
                httpclient.start();
                HttpPost postReq = getHttpPost(url, msg);
                Future<HttpResponse> future = httpclient.execute(postReq, null);
                HttpResponse response = future.get();
                OrzMC.logger().info("Response : " + EntityUtils.toString(response.getEntity()));
            } catch (ExecutionException e) {
                OrzMC.logger().warning("Lark机器人无法连接，工作异常" + ":" + e);
            } catch (Exception e) {
                OrzMC.logger().warning(e.toString());
            }
        }).start();
    }

    private static @NotNull HttpPost getHttpPost(String url, String msg) {
        HttpPost postReq = new HttpPost(url);
        postReq.setHeader("Content-Type", "application/json");
        HashMap<String, Object> params = new HashMap<>();
        params.put("msg_type", "text");
        HashMap<String, String> content = new HashMap<>();
        content.put("text", msg);
        params.put("content", content);
        String paramJsonString = new Gson().toJson(params);
        StringEntity jsonEntity = new StringEntity(paramJsonString, ContentType.APPLICATION_JSON);
        postReq.setEntity(jsonEntity);
        return postReq;
    }

    private static String larkBotWebhook() {
        return OrzMC.config().getString("lark_bot_webhook");
    }
}
