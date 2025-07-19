package com.jokerhub.paper.plugin.orzmc.bot.discord;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.bot.base.OrzBaseBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Base64;

public class OrzDiscordBot extends OrzBaseBot {

    private static JDA api;

    @Override
    public boolean isEnable() {
        return OrzMC.config().getBoolean("enable_discord_bot");
    }

    @Override
    public void setup() {
        if (!this.isEnable()) {
            OrzMC.debugInfo("Discord Bot Disabled!");
            return;
        }
        String minecraftVersion = OrzMC.server().getMinecraftVersion();
        String serverInfo = "Minecraft" + "(" + minecraftVersion + ")";
        String botTokenBase64Encoded = OrzMC.config().getString("discord_bot_token_base64_encoded");
        String botToken = new String(Base64.getDecoder().decode(botTokenBase64Encoded));
        api = JDABuilder.createLight(botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS).addEventListeners(new OrzDiscordEvent()).setActivity(Activity.playing(serverInfo)).build();
    }

    @Override
    public void teardown() {
        boolean enable = OrzMC.config().getBoolean("enable_discord_bot");
        if (!enable) return;
        api.shutdown();
    }
}
