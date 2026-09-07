package com.jokerhub.paper.plugin.orzmc.features.teleport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.Lang;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import com.jokerhub.paper.plugin.orzmc.testutil.TestI18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class TeleportBowTextsTest {

    private final I18nService i18n = TestI18n.newService();

    private TeleportBowTexts texts() {
        OrzTextStyles styles = mock(OrzTextStyles.class);
        when(styles.colorWarn()).thenReturn(NamedTextColor.GOLD);
        return new TeleportBowTexts(i18n, styles);
    }

    @Test
    void logText_includesTagAndContentInTargetLang() {
        Component result = texts().logText(Lang.ZH_CN, "teleport.bow.done");

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertTrue(plain.contains("[传送弓]"));
        assertTrue(plain.contains("传送完成!"));
    }

    @Test
    void logText_englishTarget_usesEnglishCatalog() {
        Component result = texts().logText(Lang.of("en-US"), "teleport.bow.done");

        String plain = PlainTextComponentSerializer.plainText().serialize(result);
        assertTrue(plain.contains("[Teleport Bow]"));
        assertTrue(plain.contains("Teleport complete!"));
    }
}
