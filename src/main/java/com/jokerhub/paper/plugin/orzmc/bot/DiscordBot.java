package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {

    private static JDA api;

    public static void setup() {
        String serverInfo = "jokerhub.cn";
        String botToken = OrzMC.config().getString("discord_bot_token");
        api = JDABuilder.createLight(botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new DiscordEvent())
                .setActivity(Activity.playing(serverInfo))
                .build();
    }

    public static void shutdown() {
        api.shutdown();
    }

}
