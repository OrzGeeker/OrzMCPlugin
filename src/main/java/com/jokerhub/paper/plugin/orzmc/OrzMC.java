package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.OrzGuideBook;
import com.jokerhub.paper.plugin.orzmc.commands.OrzMenuCommand;
import com.jokerhub.paper.plugin.orzmc.commands.OrzTPBow;
import com.jokerhub.paper.plugin.orzmc.events.*;
import com.jokerhub.paper.plugin.orzmc.utils.bot.OrzBaseBot;
import com.jokerhub.paper.plugin.orzmc.utils.bot.OrzDiscordBot;
import com.jokerhub.paper.plugin.orzmc.utils.bot.OrzLarkBot;
import com.jokerhub.paper.plugin.orzmc.utils.bot.OrzQQBot;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

public final class OrzMC extends JavaPlugin implements Listener {

    private Listener[] eventListeners;
    private Map<String, CommandExecutor> commandHandlers;
    private Map<String, OrzBaseBot> bots;

    Listener[] getEventListeners() {
        if (eventListeners == null) {
            eventListeners = new Listener[]{
                    new OrzBowShootEvent(),
                    new OrzPlayerEvent(this),
                    new OrzTPEvent(),
                    new OrzTNTEvent(this),
                    new OrzMenuEvent(),
                    new OrzServerEvent(this),
                    new OrzWhiteListEvent(this),
                    new OrzDebugEvent()
            };
        }
        return eventListeners;
    }

    Map<String, CommandExecutor> getCommandHandlers() {
        if (commandHandlers == null) {
            commandHandlers = Map.of(
                    "tpbow", new OrzTPBow(),
                    "guide", new OrzGuideBook(),
                    "menu", new OrzMenuCommand()
            );
        }
        return commandHandlers;
    }

    Map<String, OrzBaseBot> getBots() {
        if (bots == null) {
            bots = Map.of(
                    "qq", new OrzQQBot(this),
                    "discord", new OrzDiscordBot(this),
                    "lark", new OrzLarkBot(this)
            );
        }
        return bots;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        getLogger().info("OrzMC 插件生效!");

        // 如果插件的数据目录下没有配置文件，则复制config.yml内容到插件的数据目录
        saveDefaultConfig();

        // 注册所有事件处理器
        Arrays.stream(getEventListeners()).forEach(eventListener -> getServer().getPluginManager().registerEvents(eventListener, this));

        // 设置所有命令处理器
        getCommandHandlers().forEach((key, value) -> {
            PluginCommand cmd = getCommand(key);
            if (cmd != null) {
                cmd.setExecutor(value);
            }
        });

        // 配置通知机器人
        getBots().values().forEach(OrzBaseBot::setup);

        // 设置是否开启强制白名单
        setupServerForceWhitelist();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        notifyServerStop();
        bots.values().forEach(OrzBaseBot::teardown);
    }

    // 公共静态成员
    public static JavaPlugin plugin() {
        return JavaPlugin.getPlugin(OrzMC.class);
    }

    public static Server server() {
        return plugin().getServer();
    }

    public static Logger logger() {
        return OrzMC.plugin().getLogger();
    }

    public static FileConfiguration config() {
        return OrzMC.plugin().getConfig();
    }

    // 公共方法
    public void sendPublicMessage(String message) {
        OrzMC.debugInfo(message);
        bots.values().forEach(bot -> bot.sendMessage(message));
    }

    public void sendPrivateMessage(String message) {
        debugInfo(message);
        bots.values().forEach(bot -> bot.sendPrivateMessage(message));
    }

    public static void debugInfo(String msg) {
        if (!OrzDebugEvent.debug) {
            return;
        }
        OrzMC.logger().info(msg);
    }

    private void notifyServerStop() {
        String minecraftVersion = getServer().getMinecraftVersion();
        String stringBuilder = "Minecraft " + minecraftVersion + "\n" + "------" + "\n" + "服务停止" + "\n\n" + "停止状态无法响应命令消息";
        sendPublicMessage(stringBuilder);
    }

    private void setupServerForceWhitelist() {
        boolean forceWhitelist = getConfig().getBoolean("force_whitelist");
        getServer().setWhitelist(forceWhitelist);
        getServer().setWhitelistEnforced(forceWhitelist);
        getServer().reloadWhitelist();
        getServer().setDefaultGameMode(GameMode.SURVIVAL);
        if (forceWhitelist) {
            getLogger().info("服务端使用强制白名单机制");
        }
    }
}