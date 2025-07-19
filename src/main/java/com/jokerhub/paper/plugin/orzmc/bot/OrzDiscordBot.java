package com.jokerhub.paper.plugin.orzmc.bot;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Base64;

public class OrzDiscordBot extends OrzBaseBot {

    private final ArrayList<String> toBeSendMessageWhenApiReady = new ArrayList<String>();
    private JDA api;
    private boolean isApiReady;

    public static String markdownFormatMessage(String rawmessage) {
        return "```\n" + rawmessage + "\n```";
    }

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
        api = JDABuilder.createLight(botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS).addEventListeners(new ListenerAdapter() {
            @Override
            public void onReady(@NotNull ReadyEvent event) {
                super.onReady(event);

                isApiReady = true;
                toBeSendMessageWhenApiReady.forEach(message -> {
                    sendMessage(message);
                });
                toBeSendMessageWhenApiReady.clear();
            }

            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                super.onMessageReceived(event);

                if (event.getAuthor().isBot()) return;

                Message message = event.getMessage();
                String content = message.getContentRaw();
                Boolean isAdmin = true;
                OrzMessageParser.parse(content, isAdmin, info -> {
                    if (info != null) {
                        MessageChannel channel = event.getChannel();
                        channel.sendMessage(OrzDiscordBot.markdownFormatMessage(info)).queue();
                    }
                });
            }
        }).setActivity(Activity.playing(serverInfo)).build();
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
        if (!isApiReady) {
            toBeSendMessageWhenApiReady.add(message);
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
}