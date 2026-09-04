package com.jokerhub.paper.plugin.orzmc.assembly;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import com.jokerhub.paper.plugin.orzmc.features.bot.ImAdminService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * IM 管理子命令（挂在 /config im 下，方案 §4.3 D10-D12）：setup / status / bind / test。
 *
 * <p>权限由 /config 管理根的 admin 拦截链统一兜底（D10：bind 仅控制台/游戏内 op）；
 * 服务层 {@link ImAdminService} 另有权限守卫防串用（可单测权限拒绝）。</p>
 */
final class ImCommandRegistrar {

    private ImCommandRegistrar() {}

    /** 构建 {@code im} 子树节点，由 /config 注册器挂载。 */
    static LiteralCommandNode<CommandSourceStack> build(ImAdminService svc) {
        LiteralArgumentBuilder<CommandSourceStack> im = literal("im");
        im.then(literal("setup").executes(ctx -> {
            svc.setup(sender(ctx));
            return 1;
        }));
        im.then(literal("status").executes(ctx -> {
            svc.status(sender(ctx));
            return 1;
        }));
        im.then(bindSubtree(svc));
        im.then(testSubtree(svc));
        im.executes(ctx -> {
            svc.setup(sender(ctx));
            ctx.getSource()
                    .getSender()
                    .sendMessage(Component.text(
                            "用法: /config im setup|status | bind <平台> <group|user> <会话id> <admin_group|player_group|admin_dm>"
                                    + " | test <平台> <group|user> <会话id> <文本>"));
            return 1;
        });
        return im.build();
    }

    /** bind 子树：platform chat_type chat_id role（role 层执行）。 */
    private static ArgumentBuilder<CommandSourceStack, ?> bindSubtree(ImAdminService svc) {
        RequiredArgumentBuilder<CommandSourceStack, String> role = argument("role", StringArgumentType.word());
        role.executes(ctx -> {
            svc.bind(
                    sender(ctx),
                    ctx.getArgument("platform", String.class),
                    ctx.getArgument("chat_type", String.class),
                    ctx.getArgument("chat_id", String.class),
                    ctx.getArgument("role", String.class));
            return 1;
        });
        RequiredArgumentBuilder<CommandSourceStack, String> chatId = argument("chat_id", StringArgumentType.word());
        chatId.then(role);
        RequiredArgumentBuilder<CommandSourceStack, String> chatType = argument("chat_type", StringArgumentType.word());
        chatType.then(chatId);
        RequiredArgumentBuilder<CommandSourceStack, String> platform = argument("platform", StringArgumentType.word());
        platform.then(chatType);
        return literal("bind").then(platform);
    }

    /** test 子树：platform chat_type chat_id text（text 层执行）。 */
    private static ArgumentBuilder<CommandSourceStack, ?> testSubtree(ImAdminService svc) {
        RequiredArgumentBuilder<CommandSourceStack, String> text = argument("text", StringArgumentType.greedyString());
        text.executes(ctx -> {
            svc.test(
                    sender(ctx),
                    ctx.getArgument("platform", String.class),
                    ctx.getArgument("chat_type", String.class),
                    ctx.getArgument("chat_id", String.class),
                    ctx.getArgument("text", String.class));
            return 1;
        });
        RequiredArgumentBuilder<CommandSourceStack, String> chatId = argument("chat_id", StringArgumentType.word());
        chatId.then(text);
        RequiredArgumentBuilder<CommandSourceStack, String> chatType = argument("chat_type", StringArgumentType.word());
        chatType.then(chatId);
        RequiredArgumentBuilder<CommandSourceStack, String> platform = argument("platform", StringArgumentType.word());
        platform.then(chatType);
        return literal("test").then(platform);
    }

    private static CommandSender sender(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }
}
