package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class QQBot implements HttpHandler {

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
            if (groupId.equals(qqGroupId)) {
                String info = Notifier.processMessage(message, isAdmin);
                if (info != null) {
                    sendQQGroupMsg(info);
                }
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
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
        if (!enable()) {
            return;
        }
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

    public static boolean enable() {
        return OrzMC.config().getBoolean("enable_qq_bot");
    }
}
