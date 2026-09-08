package com.jokerhub.paper.plugin.orzmc.features.command.binding;

import com.jokerhub.paper.plugin.orzmc.features.command.CommandFeedbackService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

/**
 * 命令权限判定（管理命令拦截 / 服务内守卫共用）。
 *
 * <p>拒绝文案经 {@link I18nService} 按玩家语言渲染（{@code common.admin_required}）。</p>
 */
public final class CommandPermissionService {

    public record PermissionResult(boolean allowed, TextComponent message) {}

    private final CommandFeedbackService feedbackService;

    public CommandPermissionService(I18nService i18n) {
        this.feedbackService = new CommandFeedbackService(i18n);
    }

    public PermissionResult requireAdmin(Player player) {
        if (!(player.isOp() || player.hasPermission("orzmc.admin"))) {
            return new PermissionResult(false, feedbackService.adminRequiredTip(player));
        }
        return new PermissionResult(true, Component.text(""));
    }
}
