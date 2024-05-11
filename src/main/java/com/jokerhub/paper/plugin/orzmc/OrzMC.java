package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.OrzMenuCommand;
import com.jokerhub.paper.plugin.orzmc.commands.GuideBook;
import com.jokerhub.paper.plugin.orzmc.commands.TPBow;
import com.jokerhub.paper.plugin.orzmc.bot.DiscordBot;
import com.jokerhub.paper.plugin.orzmc.events.*;
import com.jokerhub.paper.plugin.orzmc.bot.QQBot;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public final class OrzMC extends JavaPlugin implements Listener {

    private HttpServer server;

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
        getLogger().info("服务端使用强制白名单机制");
    }

    @Override
    public void onDisable() {
        super.onDisable();

        stopQQBotServer();

        DiscordBot.shutdown();
    }

    public boolean enableQQBot() {
        return OrzMC.config().getBoolean("enable_qq_bot");
    }

    public void startQQBotServer() {
        if (!enableQQBot()) {
            logger().info("QQBot disabled!");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(8201), 0);
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
