package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.features.portal.PortalEventService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class OrzPortalEvent extends OrzBaseListener {
    private final PortalEventService service;

    public OrzPortalEvent(OrzMC plugin, PortalEventService service) {
        super(plugin);
        this.service = service;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        service.handle(event);
    }

    /**
     * Folia 补偿路径：PlayerPortalEvent 在 Folia 26.2 不触发（下界传送门走 portalAsync 新路径），
     * 由 PlayerMoveEvent 区域检测触发跨服 transfer（服务内部判断仅 Folia 生效）。
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        service.handleMove(event);
    }
}
