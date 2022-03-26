package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.OrzMenuCommand;
import com.jokerhub.paper.plugin.orzmc.commands.GuideBook;
import com.jokerhub.paper.plugin.orzmc.commands.TPBow;
import com.jokerhub.paper.plugin.orzmc.events.*;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import com.sun.net.httpserver.HttpServer;
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
        getServer().getPluginManager().registerEvents(new TNTEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzMenuEvent(), this);
        getServer().getPluginManager().registerEvents(new ServerEvent(), this);

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
    }

    @Override
    public void onDisable() {
        super.onDisable();

        stopQQBotServer();
    }

    public void startQQBotServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(8201), 0);
            server.createContext("/qqbot", new QQBotEvent());
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
