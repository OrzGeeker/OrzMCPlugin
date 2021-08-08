package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.TPBow;
import com.jokerhub.paper.plugin.orzmc.events.OnBowShoot;
import com.jokerhub.paper.plugin.orzmc.events.PlayerEvents;
import com.jokerhub.paper.plugin.orzmc.events.QQBotEvent;
import com.jokerhub.paper.plugin.orzmc.events.TPEvennts;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public final class OrzMC extends JavaPlugin implements Listener {

    private HttpServer server;

    @Override
    public void onEnable() {
        super.onEnable();

        // Plugin startup logic
        getLogger().info("OrzMC 插件生效!");
        getServer().getPluginManager().registerEvents(new OnBowShoot(), this);
        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
        getServer().getPluginManager().registerEvents(new TPEvennts(), this);

        PluginCommand tpbowCmd = getCommand("tpbow");
        if(tpbowCmd != null) {
            tpbowCmd.setExecutor(new TPBow());
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
            server = HttpServer.create(new InetSocketAddress(8200), 0);
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
}
