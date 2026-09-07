package com.jokerhub.paper.plugin.orzmc.features.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.testutil.TestI18n;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

/**
 * CommandFeedbackService：sender 无（null）时回落服务器默认语言（zh-CN），
 * 断言用 bundled zh 原文。
 */
class CommandFeedbackServiceTest {

    private final I18nService i18n = TestI18n.newService();
    private final CommandFeedbackService service = new CommandFeedbackService(i18n);

    @Test
    void cooldownTip_defaultLang() {
        String text = PlainTextComponentSerializer.plainText().serialize(service.cooldownTip(null));
        assertTrue(text.contains("冷却"));
    }

    @Test
    void adminRequiredTip_defaultLang() {
        String text = PlainTextComponentSerializer.plainText().serialize(service.adminRequiredTip(null));
        assertTrue(text.contains("管理员权限"));
    }

    @Test
    void playerRequiredTip_defaultLang() {
        String text = PlainTextComponentSerializer.plainText().serialize(service.playerRequiredTip(null));
        assertTrue(text.contains("玩家"));
    }

    @Test
    void prisonDeniedTip_defaultLang() {
        String text = PlainTextComponentSerializer.plainText().serialize(service.prisonDeniedTip(null));
        assertTrue(text.contains("坐牢"));
    }

    @Test
    void usageTip() {
        String text = PlainTextComponentSerializer.plainText().serialize(service.usageTip("/cmd <arg>"));
        assertEquals("/cmd <arg>", text);
    }

    @Test
    void portNumberRequiredTip() {
        String text = PlainTextComponentSerializer.plainText().serialize(service.portNumberRequiredTip());
        assertTrue(text.contains("数字"));
    }
}
