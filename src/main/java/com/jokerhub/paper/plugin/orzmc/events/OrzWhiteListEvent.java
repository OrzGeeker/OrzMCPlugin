package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent;
import com.destroystokyo.paper.event.server.WhitelistToggleEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;

public class OrzWhiteListEvent extends OrzBaseListener {
    public OrzWhiteListEvent(OrzMC plugin) {
        super(plugin);
    }

    private boolean isEnableForceWhitelist() {
        return plugin.getConfig().getBoolean("force_whitelist");
    }

    @EventHandler
    public void onWhitelistVerify(ProfileWhitelistVerifyEvent event) {
        PlayerProfile player = event.getPlayerProfile();
        if (player.getName() == null) {
            return;
        }
        if (event.isWhitelisted()) {
            return;
        }
        TextComponent.Builder kickMsgBuilder = Component.text();
        String qqGroupId = OrzMC.config().getString("qq_group_id");
        if (qqGroupId != null && !qqGroupId.isEmpty()) {
            if (!kickMsgBuilder.build().equals(Component.empty())) {
                kickMsgBuilder.append(Component.newline()).append(Component.newline());
            }
            kickMsgBuilder.append(Component.text(player.getName()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)).append(Component.space()).append(Component.text("不在服务器白名单中，请先加入QQ群:")).append(Component.space()).append(Component.text(qqGroupId).color(NamedTextColor.YELLOW)).append(Component.space()).append(Component.text("，联系管理员添加白名单"));
        }
        String discordServerLink = OrzMC.config().getString("discord_server_link");
        if (discordServerLink != null && !discordServerLink.isEmpty()) {
            if (!kickMsgBuilder.build().equals(Component.empty())) {
                kickMsgBuilder.append(Component.newline()).append(Component.newline());
            }
            kickMsgBuilder.append(Component.text("you can also join the discord server: ")).append(Component.text(discordServerLink).color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl(discordServerLink)));
        }
        if (!kickMsgBuilder.build().equals(Component.empty())) {
            event.kickMessage(kickMsgBuilder.build());
        }

        // 通知玩家群
        String playChatGroupMsg = player.getName() + " 尝试加入服务器，被白名单拦截";
        plugin.sendPublicMessage(playChatGroupMsg);
    }

    @EventHandler
    public void onWhitelistToggled(WhitelistToggleEvent event) {
        if (isEnableForceWhitelist() && !event.isEnabled()) {
            plugin.sendPublicMessage("‼️服务器白名单异常关闭");
        }
    }
}

