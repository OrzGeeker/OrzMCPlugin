package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.TpBow;
import com.jokerhub.paper.plugin.orzmc.events.EventHandlers;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrzMC extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("OrzMC 插件生效!");
        getServer().getPluginManager().registerEvents(new EventHandlers(), this);
        getCommand("tpbow").setExecutor(new TpBow());
    }
}
