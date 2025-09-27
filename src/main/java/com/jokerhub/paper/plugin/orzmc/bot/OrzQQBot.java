package com.jokerhub.paper.plugin.orzmc.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OrzQQBot extends OrzBaseBot {
    private WebSocketClient webSocketClient;

    @Override
    public boolean isEnable() {
        return OrzMC.config().getBoolean("enable_qq_bot");
    }

    @Override
    public void setup() {
        this.setupWebSocketClient();
    }

    @Override
    public void teardown() {
        this.shutdownWebSocketClient();
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
            String qqGroupId = OrzMC.config().getString("qq_group_id");
            if (groupId.equals(qqGroupId)) {
                OrzMessageParser.parse(message, isAdmin || isOwner, info -> {
                    if (info != null) {
                        sendQQGroupMsg(info);
                    }
                });
            }
        } catch (Exception e) {
            OrzMC.logger().info(e.toString());
        }
    }

    public void sendQQGroupMsg(String msg) {
        if (!this.isEnable()) {
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

    public void sendPrivateMsg(String msg) {
        if (!this.isEnable()) {
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
        String httpServerBearerToken = OrzMC.config().getString("qq_bot_api_server_token");
        if (httpServerBearerToken != null && !httpServerBearerToken.isEmpty()) {
            httpHeaders.put("Authorization", "Bearer " + httpServerBearerToken);
        }
        return httpHeaders;
    }

    private Map<String, String> websocketServerHeaderMap() {
        Map<String, String> httpHeaders = new HashMap<>();
        String websocketServerBearerToken = OrzMC.config().getString("qq_bot_ws_server_token");
        if (websocketServerBearerToken != null && !websocketServerBearerToken.isEmpty()) {
            httpHeaders.put("Authorization", "Bearer " + websocketServerBearerToken);
        }
        return httpHeaders;
    }

    public void setupWebSocketClient() {
        String wsServer = OrzMC.config().getString("qq_bot_ws_server");
        if (!this.isEnable() || wsServer == null || wsServer.isEmpty()) {
            return;
        }
        try {
            URI uri = new URI(wsServer);
            webSocketClient = new WebSocketClient(uri, this.websocketServerHeaderMap()) {
                @Override
                public void onOpen(ServerHandshake handShakeData) {
                    OrzMC.logger().info("打开长链接");
                }

                @Override
                public void onMessage(String message) {
                    OrzMC.debugInfo("接收到消息: " + message);
                    processJsonStringPayload(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    OrzMC.logger().info("关闭长链接: code: " + code + ", reason: " + reason + ", remote: " + remote);
                }

                @Override
                public void onError(Exception ex) {
                    OrzMC.logger().severe(ex.toString());
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
        webSocketClient.close();
    }
}
