package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.TPBow;
import com.jokerhub.paper.plugin.orzmc.events.OnBowShoot;
import com.jokerhub.paper.plugin.orzmc.events.PlayerEvents;
import com.jokerhub.paper.plugin.orzmc.events.TPEvennts;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrzMC extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("OrzMC 插件生效!");
        getServer().getPluginManager().registerEvents(new OnBowShoot(), this);
        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
        getServer().getPluginManager().registerEvents(new TPEvennts(), this);
        getCommand("tpbow").setExecutor(new TPBow());
    }
}
