package com.jokerhub.paper.plugin.orzmc.infra.player;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nServiceHolder;
import org.bukkit.entity.Player;

public final class PlayerDisplayNames {
    private PlayerDisplayNames() {}

    public static String format(Player player) {
        return format(player, null);
    }

    /** 玩家显示名：玩家名(op) 游戏模式 权限组（groupName 可为 null，省略权限组）。 */
    public static String format(Player player, String groupName) {
        String ret = player.getPlayerProfile().getName();
        if (player.isOp()) {
            ret += "(op)";
        }
        // P5-5：游戏模式词汇走语言包 playermode.*（默认语言 R1，bot/群事件 var 值）
        String gameMode =
                switch (player.getGameMode()) {
                    case CREATIVE -> I18nServiceHolder.msg("playermode.creative");
                    case SURVIVAL -> I18nServiceHolder.msg("playermode.survival");
                    case ADVENTURE -> I18nServiceHolder.msg("playermode.adventure");
                    case SPECTATOR -> I18nServiceHolder.msg("playermode.spectator");
                };
        ret += " " + gameMode;
        if (groupName != null && !groupName.isBlank()) {
            ret += " " + groupName;
        }
        return ret;
    }
}
