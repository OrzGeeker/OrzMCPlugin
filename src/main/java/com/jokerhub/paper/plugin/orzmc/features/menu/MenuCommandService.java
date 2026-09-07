package com.jokerhub.paper.plugin.orzmc.features.menu;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import org.bukkit.entity.Player;

public final class MenuCommandService {
    private final MenuService service;
    private final I18nService i18n;

    public MenuCommandService(OrzTextStyles styles, I18nService i18n) {
        this.i18n = i18n;
        this.service = new MenuService(styles, i18n);
    }

    public sealed interface Result permits Result.Success, Result.Failure {
        record Success() implements Result {}

        record Failure() implements Result {}
    }

    public Result handle(Player p) {
        service.openMenu(p);
        return new Result.Success();
    }
}
