package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import org.bukkit.event.Listener;

public class OrzBaseListener implements Listener {
    final OrzMC plugin;

    public OrzBaseListener(OrzMC plugin) {
        this.plugin = plugin;
    }
}
