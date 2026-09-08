package com.jokerhub.paper.plugin.orzmc.features.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.notify.Notifier;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.testutil.ServiceTestBase;
import org.bukkit.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ServerLifecycleServiceTest extends ServiceTestBase {

    @Mock
    private ServerFacade server;

    @Mock
    private TypedConfigProvider configs;

    @Mock
    private Notifier notifier;

    private ServerLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new ServerLifecycleService(server, configs, notifier);
    }

    @Test
    void notifyServerStop_sendsEvent() {
        Server bukkitServer = mock(Server.class);
        when(server.server()).thenReturn(bukkitServer);
        when(bukkitServer.getMinecraftVersion()).thenReturn("1.21.4");
        when(configs.renderEvent(eq("server_stop"), anyMap()))
                .thenReturn(MessageEnvelope.publicMessage("Minecraft 1.21.4"));

        service.notifyServerStop();

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<java.util.Map<String, String>> vars =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(configs).renderEvent(eq("server_stop"), vars.capture());
        // P5-2：server_stop 正文迁语言包 event.server_stop，服务端仅注入 {version}
        assertTrue(vars.getValue().containsKey("version"), "vars: " + vars.getValue());
        assertEquals("1.21.4", vars.getValue().get("version"));
        verify(notifier).event(eq("server_stop"), any(MessageEnvelope.class));
    }
}
