package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.features.rank.RankService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家在线时长监听：记录加入/退出时间差，累计到 RankStore。
 *
 * <p>每次退出（或踢出）时把本次在线分钟数累加，并检查是否达到自动晋升阈值。
 * 防抖：同 UUID 重复 Join 覆盖旧记录。</p>
 */
public final class OrzRankEvent extends OrzBaseListener {

    private final RankService service;
    private final Map<UUID, Instant> joinedAt = new ConcurrentHashMap<>();

    public OrzRankEvent(OrzMC plugin, RankService service) {
        super(plugin);
        this.service = service;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        joinedAt.put(player.getUniqueId(), Instant.now());
        // 上线即检查一次（若已达标立即晋升）
        service.checkPromotion(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        accumulateAndCheck(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(org.bukkit.event.player.PlayerKickEvent event) {
        accumulateAndCheck(event.getPlayer());
    }

    private void accumulateAndCheck(Player player) {
        Instant start = joinedAt.remove(player.getUniqueId());
        if (start == null) {
            return;
        }
        long seconds = java.time.Duration.between(start, Instant.now()).getSeconds();
        if (seconds >= 60) {
            service.recordPlaytime(player.getUniqueId(), java.time.Duration.ofSeconds(seconds));
        }
        service.checkPromotion(player.getUniqueId());
    }
}
