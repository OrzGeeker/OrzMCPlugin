package com.jokerhub.paper.plugin.orzmc.assembly;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.MaintenanceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenanceModuleTest {

    @Mock
    private PlatformModule platform;

    @Mock
    private BotModule botModule;

    private MaintenanceModule module;

    @BeforeEach
    void setUp() {
        // ScheduledBackupService 在构造时捕获 configs 引用，setup() 才读取 maintenance()，
        // 因此这里提前给默认（interval=0 关闭）并 lenient，避免其余测试报 UnnecessaryStubbing。
        TypedConfigProvider configs = mock(TypedConfigProvider.class);
        lenient().when(configs.maintenance()).thenReturn(new MaintenanceConfig(false, 300L, 5, "服务器维护中，稍后再试", 0L));
        lenient().when(platform.configs()).thenReturn(configs);
        module = new MaintenanceModule(platform, botModule);
    }

    @Test
    void constructor_createsWorldMaintenanceService() {
        assertNotNull(module.worldMaintenanceService());
    }

    @Test
    void setup_doesNotThrow() {
        assertDoesNotThrow(() -> module.setup());
    }

    @Test
    void tearDown_doesNotThrow() {
        assertDoesNotThrow(() -> module.tearDown());
    }
}
