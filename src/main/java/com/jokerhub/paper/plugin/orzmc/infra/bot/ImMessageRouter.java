package com.jokerhub.paper.plugin.orzmc.infra.bot;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope.TargetType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * IM 消息路由（出站目标解析，纯逻辑、driver 无关）。
 *
 * <p>由 {@link OrzEasyBot}（EasyBotDriver）与未来的 BuiltinDriver 共用：
 * <ul>
 *   <li>{@code PUBLIC} → 各启用会话的 {@code player_group}（空降级 {@code admin_group}）</li>
 *   <li>{@code PRIVATE} → 各启用会话的 {@code admin_dm}（空则跳过）</li>
 * </ul>
 * 目标输出为可直接投递的目标字符串（与来源会话标识同构）；遍历保持入参顺序。</p>
 */
public final class ImMessageRouter {

    private ImMessageRouter() {}

    /** 解析某目标类型对应的全部目标；未启用会话一律排除。 */
    public static List<String> resolveTargets(TargetType targetType, Collection<ImConversation> conversations) {
        if (targetType == null || conversations == null) {
            return List.of();
        }
        return switch (targetType) {
            case PUBLIC -> publicTargets(conversations);
            case PRIVATE -> privateTargets(conversations);
        };
    }

    /** PUBLIC 目标：各启用会话 playerGroup 优先、空降级 adminGroup。 */
    public static List<String> publicTargets(Collection<ImConversation> conversations) {
        List<String> targets = new ArrayList<>();
        for (ImConversation conv : conversations) {
            if (!conv.enabled()) {
                continue;
            }
            String target = conv.publicTarget();
            if (target != null && !target.isEmpty()) {
                targets.add(target);
            }
        }
        return targets;
    }

    /** PRIVATE 目标：各启用会话 adminDm（空跳过）。 */
    public static List<String> privateTargets(Collection<ImConversation> conversations) {
        List<String> targets = new ArrayList<>();
        for (ImConversation conv : conversations) {
            if (!conv.enabled()) {
                continue;
            }
            String target = conv.adminDm();
            if (target != null && !target.isEmpty()) {
                targets.add(target);
            }
        }
        return targets;
    }
}
