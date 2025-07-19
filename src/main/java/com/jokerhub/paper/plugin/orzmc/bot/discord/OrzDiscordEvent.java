package com.jokerhub.paper.plugin.orzmc.bot.discord;

import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class OrzDiscordEvent extends ListenerAdapter {

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
}
