package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.bot.DiscordBot;
import com.jokerhub.paper.plugin.orzmc.bot.QQBot;
import com.jokerhub.paper.plugin.orzmc.commands.GuideBook;
import com.jokerhub.paper.plugin.orzmc.commands.OrzMenuCommand;
import com.jokerhub.paper.plugin.orzmc.commands.TPBow;
import com.jokerhub.paper.plugin.orzmc.events.*;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Logger;

public final class OrzMC extends JavaPlugin implements Listener {

    private HttpServer server;
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
        getServer().getPluginManager().registerEvents(new BowShootEvent(), this);
        getServer().getPluginManager().registerEvents(new PlayerEvent(), this);
        getServer().getPluginManager().registerEvents(new TPEvennt(), this);

        if(config().getBoolean("explosion_report")) {
            getServer().getPluginManager().registerEvents(new TNTEvent(), this);
        }

        getServer().getPluginManager().registerEvents(new OrzMenuEvent(), this);
        getServer().getPluginManager().registerEvents(new ServerEvent(), this);
        getServer().getPluginManager().registerEvents(new WhiteListEvent(), this);

        PluginCommand tpbowCmd = getCommand("tpbow");
        if(tpbowCmd != null) {
            tpbowCmd.setExecutor(new TPBow());
        }

        PluginCommand guideCmd = getCommand("guide");
        if(guideCmd != null) {
            guideCmd.setExecutor(new GuideBook());
        }

        PluginCommand menuCmd = getCommand("menu");
        if(menuCmd != null) {
            menuCmd.setExecutor(new OrzMenuCommand());
        }

        startQQBotServer();

        DiscordBot.setup();

        // 开启强制使用白名单机制
        boolean forceWhitelist = config().getBoolean("force_whitelist");
        getServer().setWhitelist(forceWhitelist);
        getServer().setWhitelistEnforced(forceWhitelist);
        getServer().reloadWhitelist();
        getServer().setDefaultGameMode(GameMode.SURVIVAL);
        if (forceWhitelist) {
            getLogger().info("服务端使用强制白名单机制");
        }

        // 建立websocket链接
        setupWebSocketClient();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        stopQQBotServer();

        DiscordBot.shutdown();
    }

    public void startQQBotServer() {
        if (!QQBot.enable()) {
            logger().info("QQBot disabled!");
            return;
        }
        try {
            InetAddress ipv4Address = InetAddress.getByName("0.0.0.0");
            server = HttpServer.create(new InetSocketAddress(ipv4Address,39740), 0);
            server.createContext("/qqbot", new QQBot());
            server.setExecutor(Bukkit.getScheduler().getMainThreadExecutor(OrzMC.plugin()));
            server.start();
            logger().info("QQBot Server started!");
        } catch(Exception e) {
            logger().info(e.toString());
            stopQQBotServer();
        }
    }

    public void stopQQBotServer() {
        if(server != null) {
            server.stop(5);
            logger().info("QQBot Server stopping!");
        }
    }

    public void setupWebSocketClient() {
        String wsServer = config().getString("qq_bot_ws_server");
        if (!QQBot.enable() || wsServer == null || wsServer.isEmpty()) {
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
                    QQBot.processJsonStringPayload(message);
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

    public static JavaPlugin plugin() {
        return JavaPlugin.getPlugin(OrzMC.class);
    }

    public static Server server() {
        return OrzMC.plugin().getServer();
    }

    public static Logger logger() {
        return OrzMC.plugin().getLogger();
    }

    public static FileConfiguration config() { return OrzMC.plugin().getConfig(); }
}
