package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.features.prison.PrisonService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 坐牢玩家登录监听：重进仍保持 prison 组，强制传回牢房（防逃跑/防绕管）。
 *
 * <p>坐牢 = LP 组独立（不在四级 track），因此 {@code OrzRankEvent} 的自动晋升检查
 * 对 prison 玩家直接跳过（RankService.checkPromotion 已拦截）；此处只做传送兜底。</p>
 */
public final class OrzPrisonEvent extends OrzBaseListener {

    private final PrisonService service;

    public OrzPrisonEvent(OrzMC plugin, PrisonService service) {
        super(plugin);
        this.service = service;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        // isPrisoner 在线玩家读 LP 在线缓存（零 future 等待，同步安全）；坐牢则传回牢房
        if (service.isPrisoner(player.getUniqueId())) {
            service.teleportToCell(player.getUniqueId());
        }
    }
}
