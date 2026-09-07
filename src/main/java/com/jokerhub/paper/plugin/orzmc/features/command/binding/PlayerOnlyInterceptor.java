package com.jokerhub.paper.plugin.orzmc.features.command.binding;

import com.jokerhub.paper.plugin.orzmc.features.command.CommandFeedbackService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayerOnlyInterceptor implements CommandInterceptor {

    private final CommandFeedbackService feedbackService;

    public PlayerOnlyInterceptor(I18nService i18n) {
        this.feedbackService = new CommandFeedbackService(i18n);
    }

    @Override
    public Component preHandle(CommandSender sender, String commandName) {
        if (sender instanceof Player) return null;
        return feedbackService.playerRequiredTip(sender);
    }
}
