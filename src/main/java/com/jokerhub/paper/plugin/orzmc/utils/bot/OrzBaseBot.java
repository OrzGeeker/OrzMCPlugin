package com.jokerhub.paper.plugin.orzmc.utils.bot;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class OrzBaseBot {
    // 机器人是否可用
    public abstract boolean isEnable();

    // 初始化机器人
    public abstract void setup();

    // 清理资源
    public abstract void teardown();

    // 发送消息
    public abstract void sendMessage(String message);

    // 发送私信
    public abstract void sendPrivateMessage(String message);

    protected final JavaPlugin plugin;

    protected OrzBaseBot(JavaPlugin plugin) {
        this.plugin = plugin;
    }
}
