package com.jokerhub.paper.plugin.orzmc.features.teleport;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.Lang;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.MessageKeys;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

/**
 * 传送弓玩家消息组装：语言包 tag + 正文，按目标玩家语言渲染（i18n P2a）。
 *
 * <p>组装只负责「拼前缀 + 空格 + 正文」，颜色由调用方按消息语义上色（与迁移前一致）。</p>
 */
public final class TeleportBowTexts {

    private final I18nService i18n;
    private final OrzTextStyles styles;

    public TeleportBowTexts(I18nService i18n, OrzTextStyles styles) {
        this.i18n = i18n;
        this.styles = styles;
    }

    /** 组装 {tag} {content} 前缀行；正文被覆盖层屏蔽（空串）时返回空组件。 */
    public TextComponent logText(Lang lang, String key) {
        String content = i18n.msg(lang, key);
        if (content.isEmpty()) {
            return Component.empty();
        }
        return Component.text()
                .append(Component.text(i18n.msg(lang, MessageKeys.TELEPORT_BOW_TAG))
                        .color(styles.colorWarn()))
                .append(Component.space())
                .append(Component.text(content))
                .build();
    }
}
