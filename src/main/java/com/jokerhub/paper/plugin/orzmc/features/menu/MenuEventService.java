package com.jokerhub.paper.plugin.orzmc.features.menu;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public final class MenuEventService {
    private final MenuService service;
    private final I18nService i18n;

    public MenuEventService(OrzTextStyles styles, I18nService i18n) {
        this.i18n = i18n;
        this.service = new MenuService(styles, i18n);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getView().getTopInventory().getType() != InventoryType.CHEST) return;
        if (event.getView().getTopInventory().getHolder() instanceof OrzMenuHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            service.onClick(p, clicked);
        }
    }
}
