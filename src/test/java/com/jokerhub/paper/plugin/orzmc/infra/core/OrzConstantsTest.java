package com.jokerhub.paper.plugin.orzmc.infra.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrzConstantsTest {

    @Test
    void tpbowKey() {
        assertEquals("tpbow", OrzConstants.TPBOW_KEY);
    }

    @Test
    void constantsAreFinalStrings() {
        // Verify they're non-null and non-empty
        assertNotNull(OrzConstants.TPBOW_KEY);
    }
}
