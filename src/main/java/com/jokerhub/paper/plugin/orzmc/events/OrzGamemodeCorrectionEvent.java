package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.features.rank.GamemodeCorrectionService;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 游戏模式矫正登录兜底：玩家上线后延迟约 1 秒矫正一次。
 *
 * <p>覆盖「离线期间被改组」的场景——LP 数据加载完成后（约 1 秒）检查该玩家当前
 * 游戏模式是否仍有权限，无权限则切回生存。延迟执行经 {@code ServerFacade.runLater}
 * 落在同步调度线程（Folia global region / Paper 主线程），符合 Bukkit API 线程约束。</p>
 */
public final class OrzGamemodeCorrectionEvent extends OrzBaseListener {

    /** 延迟 tick：约 1 秒（等 LP 数据加载完成）。 */
    private static final long JOIN_DELAY_TICKS = 20L;

    private final GamemodeCorrectionService correctionService;

    public OrzGamemodeCorrectionEvent(OrzMC plugin, GamemodeCorrectionService correctionService) {
        super(plugin);
        this.correctionService = correctionService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        serverFacade()
                .runLater(
                        () -> {
                            try {
                                correctionService.correctIfNeeded(playerId);
                            } catch (RuntimeException e) {
                                plugin.getLogger().log(Level.WARNING, "登录后游戏模式矫正异常: " + playerId, e);
                            }
                        },
                        JOIN_DELAY_TICKS);
    }
}
