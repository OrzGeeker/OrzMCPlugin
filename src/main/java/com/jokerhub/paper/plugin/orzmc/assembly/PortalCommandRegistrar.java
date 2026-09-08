package com.jokerhub.paper.plugin.orzmc.assembly;

import static com.jokerhub.paper.plugin.orzmc.assembly.BrigadierSupport.commandInterceptors;
import static com.jokerhub.paper.plugin.orzmc.assembly.BrigadierSupport.guardedExec;
import static com.jokerhub.paper.plugin.orzmc.assembly.BrigadierSupport.requirement;
import static com.jokerhub.paper.plugin.orzmc.assembly.BrigadierSupport.withPrisonDeny;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import com.jokerhub.paper.plugin.orzmc.features.command.binding.CommandInterceptor;
import com.jokerhub.paper.plugin.orzmc.features.portal.PortalCommandService;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.CommandPolicies;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.MessageKeys;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Portal 命令注册器：/portal [remove] <host> [port]（自 FeatureCommandRegistrar 拆出）。 */
final class PortalCommandRegistrar implements CommandGroup {

    private final PortalCommandService svc;
    private final OrzTextStyles styles;
    private final Supplier<CommandPolicies> cpSupplier;
    private final Predicate<Player> prisonCheck;
    private final I18nService i18n;

    PortalCommandRegistrar(
            PortalCommandService svc,
            OrzTextStyles styles,
            Supplier<CommandPolicies> cpSupplier,
            Predicate<Player> prisonCheck,
            I18nService i18n) {
        this.svc = svc;
        this.styles = styles;
        this.cpSupplier = cpSupplier;
        this.prisonCheck = prisonCheck;
        this.i18n = i18n;
    }

    /** Portal: /portal [remove] <host> [port] */
    @Override
    public void register(Commands commands) {
        List<CommandInterceptor> interceptors =
                withPrisonDeny(commandInterceptors("portal", cpSupplier, false, i18n), prisonCheck, i18n);
        Predicate<CommandSourceStack> req = requirement(interceptors);

        // /portal remove <host> [port]
        Command<CommandSourceStack> removeExec = guardedExec("portal", interceptors, ctx -> {
            String target = ctx.getArgument("target", String.class);
            return handlePortal(svc, ctx.getSource(), "remove " + target, styles);
        });

        // /portal <host> [port]
        Command<CommandSourceStack> createExec = guardedExec("portal", interceptors, ctx -> {
            String target = ctx.getArgument("target", String.class);
            return handlePortal(svc, ctx.getSource(), target, styles);
        });

        // /portal (no args → show usage)
        Command<CommandSourceStack> usageExec = guardedExec("portal", interceptors, ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            sender.sendMessage(styles.info(
                    i18n.msg(sender instanceof Player p ? i18n.langFor(p) : i18n.langFor(), MessageKeys.PORTAL_USAGE)));
            return 1;
        });

        commands.register(
                literal("portal")
                        .requires(req)
                        .then(literal("remove")
                                .then(argument("target", StringArgumentType.greedyString())
                                        .executes(removeExec)))
                        .then(argument("target", StringArgumentType.greedyString())
                                .executes(createExec))
                        .executes(usageExec)
                        .build(),
                i18n.msg(i18n.langFor(), MessageKeys.PORTAL_COMMAND_DESC),
                List.of());
    }

    private static int handlePortal(
            PortalCommandService svc, CommandSourceStack source, String argsStr, OrzTextStyles styles) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player p)) {
            sender.sendMessage(svc.requirePlayerTip());
            return 1;
        }
        String[] args = argsStr.split(" ");
        PortalCommandService.Result result = svc.handle(p, args);
        if (result instanceof PortalCommandService.Result.Success s) {
            p.sendMessage(s.message());
        } else if (result instanceof PortalCommandService.Result.Failure f) {
            p.sendMessage(f.message());
        }
        return 1;
    }
}
