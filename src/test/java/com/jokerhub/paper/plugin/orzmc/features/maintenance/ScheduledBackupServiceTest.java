package com.jokerhub.paper.plugin.orzmc.features.maintenance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.config.configs.MaintenanceConfig;
import com.jokerhub.paper.plugin.orzmc.infra.notify.Notifier;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ScheduledBackupServiceTest {

    private ServerFacade server;
    private TypedConfigProvider configs;
    private BukkitTask task;

    @BeforeEach
    void setUp() {
        server = mock(ServerFacade.class);
        configs = mock(TypedConfigProvider.class);
        task = mock(BukkitTask.class);
        when(server.runTaskTimer(any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
    }

    private static MaintenanceConfig config(long intervalHours) {
        return new MaintenanceConfig(false, 300L, 5, "服务器维护中，稍后再试", intervalHours);
    }

    @Test
    void setup_zeroHours_doesNotSchedule() {
        when(configs.maintenance()).thenReturn(config(0L));
        ScheduledBackupService service =
                new ScheduledBackupService(server, configs, mock(WorldMaintenanceService.class));

        service.setup();

        verify(server, never()).runTaskTimer(any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void setup_positiveHours_schedulesRepeatingTimer() {
        when(configs.maintenance()).thenReturn(config(2L));
        ScheduledBackupService service =
                new ScheduledBackupService(server, configs, mock(WorldMaintenanceService.class));

        service.setup();

        // 2 小时 = 2 * 72000 ticks，delay 与 period 相同（首次 2 小时后触发，之后每 2 小时一次）
        verify(server).runTaskTimer(any(Runnable.class), eq(144000L), eq(144000L));
    }

    @Test
    void tick_triggersBackupWithMaintenanceConfig() {
        when(configs.maintenance()).thenReturn(config(1L));
        WorldMaintenanceService maintenance = mock(WorldMaintenanceService.class);
        ScheduledBackupService service = new ScheduledBackupService(server, configs, maintenance);
        service.setup();

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(server).runTaskTimer(captor.capture(), anyLong(), anyLong());
        captor.getValue().run();

        verify(maintenance).backup(eq(300L), eq(5), any());
    }

    @Test
    void repeatedTick_backupRunsExclusive_doesNotStack() {
        // 真实 WorldMaintenanceService：runExclusive 的 AtomicBoolean 互斥，
        // 前一次备份进行中时再次触发直接跳过（不叠加第二次踢人/save-off）。
        when(configs.maintenance()).thenReturn(config(1L));
        OrzTextStyles styles = mock(OrzTextStyles.class);
        WorldMaintenanceService maintenance =
                new WorldMaintenanceService(server, configs, styles, mock(Notifier.class));
        ScheduledBackupService service = new ScheduledBackupService(server, configs, maintenance);

        service.tick();
        service.tick();

        verify(server, times(1)).runSync(any(Runnable.class));
        assertTrue(maintenance.isRunning());
    }

    @Test
    void tearDown_cancelsScheduledTask() {
        when(configs.maintenance()).thenReturn(config(1L));
        ScheduledBackupService service =
                new ScheduledBackupService(server, configs, mock(WorldMaintenanceService.class));
        service.setup();

        service.tearDown();

        verify(task).cancel();
    }
}
