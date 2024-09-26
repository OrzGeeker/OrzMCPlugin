package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

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

    @SuppressWarnings("unchecked")
    private static void asyncHttpRequest(String url, String msg) {
        new Thread(() -> {
            try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
                httpclient.start();
                HttpPost postReq = new HttpPost(url);
                postReq.setHeader("Content-Type", "application/json");
                JSONObject textMessage = new JSONObject();
                textMessage.put("msg_type", "text");
                JSONObject textMessageContent = new JSONObject();
                textMessageContent.put("text", msg);
                textMessage.put("content", textMessageContent);
                StringEntity jsonEntity = new StringEntity(textMessage.toJSONString(), ContentType.APPLICATION_JSON);
                postReq.setEntity(jsonEntity);
                Future<HttpResponse> future = httpclient.execute(postReq, null);
                HttpResponse response = future.get();
                OrzMC.logger().info("Response : " + EntityUtils.toString(response.getEntity()));
            } catch (ExecutionException e) {
                e.printStackTrace();
                OrzMC.logger().info("Lark机器人无法连接，工作异常");
            } catch (Exception e) {
                e.printStackTrace();
                OrzMC.logger().info(e.toString());
            }
        }).start();
    }

    private static String larkBotWebhook() {
        return OrzMC.config().getString("lark_bot_webhook");
    }
}
