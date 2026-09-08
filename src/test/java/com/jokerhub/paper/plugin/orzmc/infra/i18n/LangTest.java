package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LangTest {

    @Test
    void of_normalizesUnderscoreAndCase() {
        assertEquals("zh-CN", Lang.of("zh_CN").code());
        assertEquals("en-US", Lang.of("EN-us").code());
        assertEquals("zh-CN", Lang.of(" zh_cn ").code());
    }

    @Test
    void of_blankOrNullReturnsNull() {
        assertNull(Lang.of(null));
        assertNull(Lang.of("  "));
    }

    @Test
    void fromLocale_usesLanguageAndCountry() {
        assertEquals("zh-CN", Lang.fromLocale(Locale.SIMPLIFIED_CHINESE).code());
        assertEquals("en-US", Lang.fromLocale(Locale.US).code());
        assertEquals("en", Lang.fromLocale(Locale.ENGLISH).code());
        assertNull(Lang.fromLocale(null));
    }

    @Test
    void zhCn_constantIsCanonical() {
        assertEquals("zh-CN", Lang.ZH_CN.code());
    }

    @Test
    void equalsHashCode_byCanonicalCode() {
        assertTrue(Lang.of("zh_CN").equals(new Lang("zh-CN")));
        assertFalse(Lang.of("zh-CN").equals(Lang.of("en-US")));
    }

    @Test
    void normalize_preservesScriptStyleCodes() {
        assertEquals("zh-HANS-CN", Lang.normalize("zh-hans-cn"));
    }
}
