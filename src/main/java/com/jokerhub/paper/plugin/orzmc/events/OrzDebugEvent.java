package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.core.bot.BotInboundHandler;
import java.util.logging.Level;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class OrzDebugEvent extends OrzBaseListener {
    private final BotInboundHandler inboundHandler;

    public OrzDebugEvent(OrzMC plugin, BotInboundHandler inboundHandler) {
        super(plugin);
        this.inboundHandler = inboundHandler;
    }

    public static boolean debug = false;

    /**
     * 控制台（stdin）命令：/orzdebug &lt;bot命令&gt;
     *
     * <p>注意：Paper 26 中 RCON 命令触发的是 {@link RemoteServerCommandEvent}（子类），
     * 本监听器通过父类 {@link ServerCommandEvent} 注册仍可收到；但命令字符串可能带前导斜杠
     * （如 {@code /orzdebug $h}），需先剥掉再判断前缀。</p>
     */
    @EventHandler
    public void cmdDebugHandler(ServerCommandEvent event) {
        handle(event.getCommand());
    }

    /** RCON 命令显式监听（防御：若父类分发链路变化，此处兜底）。 */
    @EventHandler
    public void rconDebugHandler(RemoteServerCommandEvent event) {
        handle(event.getCommand());
    }

    private void handle(String rawCommand) {
        String command = rawCommand.startsWith("/") ? rawCommand.substring(1) : rawCommand;
        String debugCmdPrefix = "orzdebug";
        debug = command.startsWith(debugCmdPrefix);
        if (!debug) {
            return;
        }
        String cmd = command.substring(debugCmdPrefix.length()).trim();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                inboundHandler.handleMessage(
                        cmd, true, env -> plugin.getLogger().info("cmd debug: \n" + env.message()));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "debug 命令异步执行异常", e);
            }
        });
    }
}
