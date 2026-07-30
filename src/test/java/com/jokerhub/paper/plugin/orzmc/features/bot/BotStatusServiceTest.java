package com.jokerhub.paper.plugin.orzmc.features.bot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.ports.health.HealthStatus;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import com.jokerhub.paper.plugin.orzmc.testutil.ServiceTestBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class BotStatusServiceTest extends ServiceTestBase {

    @Test
    void buildStatusMessage_containsBotNames() {
        OrzTextStyles styles = mock(OrzTextStyles.class);
        when(styles.warn(anyString())).then(i -> Component.text((String) i.getArgument(0)));
        when(styles.success(anyString())).then(i -> Component.text((String) i.getArgument(0)));
        when(styles.error(anyString())).then(i -> Component.text((String) i.getArgument(0)));

        HealthStatus health = mock(HealthStatus.class);
        when(health.get("easybot")).thenReturn(new HealthStatus.Entry(false, false, false, false, null, 0));

        BotStatusService service = new BotStatusService(styles, health);
        Component msg = service.buildStatusMessage();
        String plain = PlainTextComponentSerializer.plainText().serialize(msg);

        assertTrue(plain.contains("EasyBot"), plain);
        assertFalse(plain.contains("QQBot"), plain);
        assertFalse(plain.contains("DiscordBot"), plain);
    }
}
