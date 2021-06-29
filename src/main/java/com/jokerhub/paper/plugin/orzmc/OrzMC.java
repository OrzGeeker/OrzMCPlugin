package com.jokerhub.paper.plugin.orzmc;

import com.jokerhub.paper.plugin.orzmc.commands.ArmorStands;
import com.jokerhub.paper.plugin.orzmc.commands.fly;
import com.jokerhub.paper.plugin.orzmc.commands.teleport;
import com.jokerhub.paper.plugin.orzmc.events.demo;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrzMC extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("OrzMC Plugin OnEnable!");
        getServer().getPluginManager().registerEvents(new demo(), this);
        getCommand("fly").setExecutor(new fly());
        getCommand("tpbow").setExecutor(new teleport());
        getCommand("armstand").setExecutor(new ArmorStands());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("OrzMC Plugin OnDisable!");
    }

}
