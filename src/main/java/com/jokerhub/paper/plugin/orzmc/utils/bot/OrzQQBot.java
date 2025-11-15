package com.jokerhub.paper.plugin.orzmc.utils.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import com.jokerhub.paper.plugin.orzmc.utils.RobustWebSocketClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OrzQQBot extends OrzBaseBot {
    private RobustWebSocketClient webSocketClient;

    public OrzQQBot(OrzMC plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnable() {
        return botConfig.getBoolean("enable_qq_bot");
    }

    @Override
    public void setup() {
        this.setupWebSocketClient();
    }

    @Override
    public void teardown() {
        this.shutdownWebSocketClient();
    }

    @Override
    public void sendMessage(String message) {
        if (!this.isEnable()) {
            return;
        }
        try {
            String groupId = botConfig.getString("qq_group_id");
            String url = botConfig.getString("qq_bot_api_server") + "/send_group_msg?group_id=" + groupId + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            asyncHttpRequest(url);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    @Override
    public void sendPrivateMessage(String message) {
        if (!this.isEnable()) {
            return;
        }
        try {
            String userId = botConfig.getString("qq_admin_id");
            String url = botConfig.getString("qq_bot_api_server") + "/send_msg?user_id=" + userId + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            asyncHttpRequest(url);
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public void processJsonStringPayload(String jsonString) {
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
            String senderRole = json.get("sender").getAsJsonObject().get("role").getAsString();
            boolean isOwner = senderRole.equals("owner");
            boolean isAdmin = senderRole.equals("admin");
            String qqGroupId = botConfig.getString("qq_group_id");
            if (groupId.equals(qqGroupId)) {
                OrzMessageParser.parse(message, isAdmin || isOwner, info -> {
                    if (info != null) {
                        sendMessage(info);
                    }
                });
            }
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    private void asyncHttpRequest(String url) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            requestBuilder.uri(URI.create(url));
            this.httpServerHeaderMap().forEach(requestBuilder::setHeader);
            HttpRequest request = requestBuilder.build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> OrzMC.debugInfo("Response Code : " + response.toString())).exceptionally(e -> {
                OrzMC.logger().severe("QQ机器人无法连接，工作异常: " + e.toString());
                return null;
            });
        } catch (Exception e) {
            OrzMC.logger().severe(e.toString());
        }
    }

    private Map<String, String> httpServerHeaderMap() {
        Map<String, String> httpHeaders = new HashMap<>();
        String httpServerBearerToken = botConfig.getString("qq_bot_api_server_token");
        if (httpServerBearerToken != null && !httpServerBearerToken.isEmpty()) {
            httpHeaders.put("Authorization", "Bearer " + httpServerBearerToken);
        }
        return httpHeaders;
    }

    private Map<String, String> websocketServerHeaderMap() {
        Map<String, String> httpHeaders = new HashMap<>();
        String websocketServerBearerToken = botConfig.getString("qq_bot_ws_server_token");
        if (websocketServerBearerToken != null && !websocketServerBearerToken.isEmpty()) {
            httpHeaders.put("Authorization", "Bearer " + websocketServerBearerToken);
        }
        return httpHeaders;
    }

    public void setupWebSocketClient() {
        String wsServer = botConfig.getString("qq_bot_ws_server");
        if (!this.isEnable() || wsServer == null || wsServer.isEmpty()) {
            return;
        }
        try {
            webSocketClient = new RobustWebSocketClient(wsServer, 10, 5000, this.websocketServerHeaderMap()) {
                @Override
                public void handleMessage(String message) {
                    processJsonStringPayload(message);
                }
            };

            webSocketClient.connect();
            // 在这里可以发送消息，例如：webSocketClient.send("Hello, WebSockets!");

        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public void shutdownWebSocketClient() {
        if (webSocketClient == null) {
            return;
        }
        webSocketClient.disconnect();
    }
}
