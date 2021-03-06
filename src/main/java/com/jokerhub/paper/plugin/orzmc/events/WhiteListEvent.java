package com.jokerhub.paper.plugin.orzmc.events;

import com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.qqbot.QQBotEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WhiteListEvent implements Listener {
    @EventHandler
    public void onWhitelistVerify(ProfileWhitelistVerifyEvent event) {
        PlayerProfile player = event.getPlayerProfile();
        if(player.getName() == null) {
            return;
        }
        if(event.isWhitelisted()) {
            return;
        }
        String qqGroupId = OrzMC.config().getString("qq_group_id");
        TextComponent kickMsg = Component.text(player.getName())
                .append(Component.text(" 不在白名单中，请先加入QQ群: " + qqGroupId + " 联系管理员进行添加"));
        event.kickMessage(kickMsg);

        // 通知QQ群
        String qqGroupMsg = player.getName() + " 尝试加入服务器，被白名单拦截";
        QQBotEvent.sendQQGroupMsg(qqGroupMsg);
    }
}

