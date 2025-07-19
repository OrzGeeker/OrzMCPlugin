package com.jokerhub.paper.plugin.orzmc.bot;

public abstract class OrzBaseBot {

    // 机器人是否可用
    public abstract boolean isEnable();

    // 初始化机器人
    public abstract void setup();

    // 清理资源
    public abstract void teardown();
}
