package com.jokerhub.paper.plugin.orzmc.bot.qq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import com.jokerhub.paper.plugin.orzmc.bot.base.OrzBaseBot;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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
        OrzMC.larkBot.sendMessage(msg);
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
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> OrzMC.debugInfo("Response Code : " + response.toString())).exceptionally(e -> {
                OrzMC.logger().severe("QQ机器人无法连接，工作异常: " + e.toString());
                return null;
            });
        } catch (Exception e) {
            OrzMC.logger().severe(e.toString());
        }
    }

    public void setupWebSocketClient() {
        String wsServer = OrzMC.config().getString("qq_bot_ws_server");
        if (!this.isEnable() || wsServer == null || wsServer.isEmpty()) {
            return;
        }
        try {
            URI uri = new URI(wsServer);
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handShakeData) {
                    OrzMC.logger().info("打开长链接");
                }

                @Override
                public void onMessage(String message) {
                    OrzMC.debugInfo("接收到消息: " + message);
                    OrzMC.qqBot.processJsonStringPayload(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    OrzMC.logger().info("关闭长链接");
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
