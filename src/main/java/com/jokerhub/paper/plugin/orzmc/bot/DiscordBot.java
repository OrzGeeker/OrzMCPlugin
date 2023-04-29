package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Base64;

public class DiscordBot {

    private static JDA api;

    public static void setup() {
        boolean enable  = OrzMC.config().getBoolean("enable_discord_bot");
        if (!enable) return;

        String serverInfo = "jokerhub.cn";
        String base64BotToken = OrzMC.config().getString("base64_discord_bot_token");
        String botToken = new String(Base64.getDecoder().decode(base64BotToken));
        api = JDABuilder.createLight(botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new DiscordEvent())
                .setActivity(Activity.playing(serverInfo))
                .build();
    }

    public static void shutdown() {

        boolean enable  = OrzMC.config().getBoolean("enable_discord_bot");
        if (!enable) return;

        api.shutdown();
    }

}
