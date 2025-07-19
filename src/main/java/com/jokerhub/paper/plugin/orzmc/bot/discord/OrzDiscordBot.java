package com.jokerhub.paper.plugin.orzmc.bot.discord;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.bot.base.OrzBaseBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
        if (!this.isEnable()) return;
        api.shutdown();
    }

    public void sendMessage(String message) {
        if (!this.isEnable()) {
            OrzMC.debugInfo("Discord Bot Disabled!");
            return;
        }
        TextChannel channel = null;
        String playerTextChannelId = OrzMC.config().getString("discord_player_text_channel_id");
        if (playerTextChannelId != null) {
            channel = api.getTextChannelById(playerTextChannelId);
        }
        if (channel != null) {
            String markdownMessage = markdownFormatMessage(message);
            channel.sendMessage(markdownMessage).queue();
        } else {
            OrzMC.logger().warning("your discord bot not in this text channel: " + playerTextChannelId);
        }
    }

    public static String markdownFormatMessage(String rawmessage) {
        return "```\n" + rawmessage + "\n```";
    }
}