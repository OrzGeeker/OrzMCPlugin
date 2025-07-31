package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.bot.OrzDiscordBot;
import com.jokerhub.paper.plugin.orzmc.bot.OrzLarkBot;
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

import java.util.logging.Logger;

public final class OrzMC extends JavaPlugin implements Listener {

    // 机器人配置
    public static final OrzDiscordBot discordBot = new OrzDiscordBot();
    public static final OrzQQBot qqBot = new OrzQQBot();
    private static final OrzLarkBot larkBot = new OrzLarkBot();

    // 公共静态成员
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

    // 公共方法
    public static void sendPublicMessage(String message) {
        discordBot.sendMessage(message);
        larkBot.sendMessage(message);
        qqBot.sendQQGroupMsg(message);
    }

    public static void sendPrivateMessage(String message) {
        qqBot.sendPrivateMsg(message);
    }

    public static void debugInfo(String msg) {
        if (!OrzDebugEvent.debug) {
            return;
        }
        OrzMC.logger().info(msg);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // 如果插件的数据目录下没有配置文件，则复制config.yml内容到插件的数据目录
        saveDefaultConfig();

        // Plugin startup logic
        getLogger().info("OrzMC 插件生效!");
        getServer().getPluginManager().registerEvents(new OrzBowShootEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzPlayerEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzTPEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzTNTEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzMenuEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzServerEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzWhiteListEvent(), this);
        getServer().getPluginManager().registerEvents(new OrzDebugEvent(), this);

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

        setupBots();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        teardownBots();
        OrzServerEvent.notifyServerStop();
    }

    private void setupBots() {
        discordBot.setup();
        larkBot.setup();
        qqBot.setup();
    }

    private void teardownBots() {
        discordBot.teardown();
        larkBot.teardown();
        qqBot.teardown();
    }
    // ---
}
