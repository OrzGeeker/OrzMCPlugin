package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.bot.OrzDiscordBot;
import com.jokerhub.paper.plugin.orzmc.bot.OrzQQBot;
import com.jokerhub.paper.plugin.orzmc.commands.OrzGuideBook;
import com.jokerhub.paper.plugin.orzmc.commands.OrzMenuCommand;
import com.jokerhub.paper.plugin.orzmc.commands.OrzTPBow;
import com.jokerhub.paper.plugin.orzmc.events.*;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.logging.Logger;

public final class OrzMC extends JavaPlugin implements Listener {

    private WebSocketClient webSocketClient;

    @Override
    public void onEnable() {
        super.onEnable();

        // 从jar中读config.yml文件内容到内存中
        getConfig().options().copyDefaults();
        // 存储config.yml到插件数据目录下
        saveDefaultConfig();

        // Plugin startup logic
        getLogger().info("OrzMC 插件生效!");
        getServer().getPluginManager().registerEvents(new OrzBowShootEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzPlayerEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzTPEvennt(), this);

        if (config().getBoolean("explosion_report")) {
            getServer().getPluginManager().registerEvents(new OrzTNTEvent(), this);
        }

        getServer().getPluginManager().registerEvents(new OrzMenuEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzServerEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzWhiteListEvent(), this);

        PluginCommand tpbowCmd = getCommand("tpbow");
        if (tpbowCmd != null) {
            tpbowCmd.setExecutor(new OrzTPBow());
        }

        PluginCommand guideCmd = getCommand("guide");
        if (guideCmd != null) {
            guideCmd.setExecutor(new OrzGuideBook());
        }

        PluginCommand menuCmd = getCommand("menu");
        if (menuCmd != null) {
            menuCmd.setExecutor(new OrzMenuCommand());
        }

        // 开启强制使用白名单机制
        boolean forceWhitelist = config().getBoolean("force_whitelist");
        getServer().setWhitelist(forceWhitelist);
        getServer().setWhitelistEnforced(forceWhitelist);
        getServer().reloadWhitelist();
        getServer().setDefaultGameMode(GameMode.SURVIVAL);
        if (forceWhitelist) {
            getLogger().info("服务端使用强制白名单机制");
        }

        OrzDiscordBot.setup();
        setupWebSocketClient();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        OrzDiscordBot.shutdown();
        shutdownWebSocketClient();
        OrzServerEvent.notifyServerStop();
    }

    public void setupWebSocketClient() {
        String wsServer = config().getString("qq_bot_ws_server");
        if (OrzQQBot.disable() || wsServer == null || wsServer.isEmpty()) {
            return;
        }
        try {
            URI uri = new URI(wsServer);
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handShakeData) {
                    logger().info("打开长链接");
                }

                @Override
                public void onMessage(String message) {
                    logger().info("接收到消息: " + message);
                    OrzQQBot.processJsonStringPayload(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger().info("关闭长链接");
                }

                @Override
                public void onError(Exception ex) {
                    logger().severe(ex.toString());
                }
            };

            webSocketClient.connect();
            // 在这里可以发送消息，例如：webSocketClient.send("Hello, WebSockets!");

        } catch (Exception e) {
            logger().info(e.toString());
        }
    }

    public void shutdownWebSocketClient() {
        if (webSocketClient == null) {
            return;
        }
        webSocketClient.close();
    }

    public static JavaPlugin plugin() {
        return JavaPlugin.getPlugin(OrzMC.class);
    }

    public static Server server() {
        return OrzMC.plugin().getServer();
    }

    public static Logger logger() {
        return OrzMC.plugin().getLogger();
    }

    public static FileConfiguration config() {
        return OrzMC.plugin().getConfig();
    }
}
