package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class OrzDebugEvent implements Listener {

    public static boolean debug = false;

    @EventHandler
    public void cmdDebugHandler(ServerCommandEvent event) {
        String debugCmdPrefix = "debug";
        debug = event.getCommand().startsWith(debugCmdPrefix);
        if (!debug) {
            return;
        }
        String cmd = event.getCommand().substring(debugCmdPrefix.length()).trim();
        OrzMC.server().getScheduler().runTaskAsynchronously(OrzMC.plugin(), () -> OrzMessageParser.parse(cmd, true, result -> OrzMC.logger().info("cmd debug: \n" + result)));
    }
}