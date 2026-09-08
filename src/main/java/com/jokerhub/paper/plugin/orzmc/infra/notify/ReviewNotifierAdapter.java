package com.jokerhub.paper.plugin.orzmc.infra.notify;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewNotifier;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 审核通知端口实现：适配现有 {@link Notifier} + {@link TypedConfigProvider} 模板渲染。
 *
 * <ul>
 *   <li>游戏内消息：玩家在线即发（离线忽略，由群通知兜底）</li>
 *   <li>群推送：走 templates.yml 模板（review_submitted / review_cancelled / review_approved / review_rejected）</li>
 * </ul>
 */
public final class ReviewNotifierAdapter implements ReviewNotifier {

    private final TypedConfigProvider configs;
    private final Notifier notifier;

    public ReviewNotifierAdapter(TypedConfigProvider configs, Notifier notifier) {
        this.configs = configs;
        this.notifier = notifier;
    }

    @Override
    public void gameMessage(UUID playerId, String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text(message));
        }
    }

    @Override
    public void groupEvent(String templateKey, Map<String, String> vars) {
        // i18n P4b-2：事件正文迁语言包 event.*（默认语言 R1）——renderEvent 按磁盘正文优先
        // （存量/服主定制）→ 缺失回落语言包；原内联 zh fallback switch 已删除。
        MessageEnvelope env = configs.renderEvent(templateKey, vars);
        notifier.event(templateKey, env);
    }
}
