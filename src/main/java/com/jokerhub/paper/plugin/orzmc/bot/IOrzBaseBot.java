package com.jokerhub.paper.plugin.orzmc.bot;

public interface IOrzBaseBot {

    // 机器人是否可用
    boolean isEnable();

    // 初始化机器人
    void setup();

    // 清理资源
    void teardown();
}
