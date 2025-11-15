package com.jokerhub.paper.plugin.orzmc.utils.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;

import java.util.Map;

public class OrzBotManager {
    private final OrzMC plugin;
    private Map<String, OrzBaseBot> bots;

    public OrzBotManager(OrzMC plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        bots = Map.of("qq", new OrzQQBot(plugin), "discord", new OrzDiscordBot(plugin), "lark", new OrzLarkBot(plugin));
        bots.values().forEach(OrzBaseBot::setup);
    }

    public void sendMessage(String message, boolean isPrivate) {
        bots.values().forEach(bot -> {
            if (isPrivate) {
                bot.sendPrivateMessage(message);
            } else {
                bot.sendMessage(message);
            }
        });
    }

    public void tearDown() {
        bots.values().forEach(OrzBaseBot::teardown);
    }
}
