package com.jokerhub.paper.plugin.orzmc.features.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceProgress;
import com.jokerhub.paper.plugin.orzmc.features.maintenance.MaintenanceModeService.MaintenanceReason;
import org.junit.jupiter.api.Test;

class MaintenanceModeServiceTest {

    @Test
    void initialState_isInactiveWithNoReason() {
        MaintenanceModeService svc = new MaintenanceModeService();
        assertFalse(svc.isActive());
        assertNull(svc.reason());
        assertEquals(0L, svc.startedAt());
        assertNull(svc.progress());
    }

    @Test
    void enter_setsReasonStartedAtAndClearsProgress() {
        MaintenanceModeService svc = new MaintenanceModeService();
        svc.updateProgress("区块", 10, 5); // 上一次残留进度
        svc.enter(MaintenanceReason.BACKUP);

        assertTrue(svc.isActive());
        assertEquals(MaintenanceReason.BACKUP, svc.reason());
        assertTrue(svc.startedAt() > 0);
        assertNull(svc.progress(), "enter 应清空上一次进度");
    }

    @Test
    void updateProgress_buildsImmutableSnapshotWithMessage() {
        MaintenanceModeService svc = new MaintenanceModeService();
        svc.enter(MaintenanceReason.OPTIMIZE);
        svc.updateProgress("区域", 45, 35);

        MaintenanceProgress p = svc.progress();
        assertNotNull(p);
        assertEquals("区域", p.stage());
        assertEquals(45, p.percent());
        assertEquals(35, p.etaSeconds());
        assertEquals("进度：区域 45% 预计剩余 35秒", p.progressMessage());
    }

    @Test
    void updateProgress_negativeEtaClampedToZero() {
        MaintenanceModeService svc = new MaintenanceModeService();
        svc.enter(MaintenanceReason.BACKUP);
        svc.updateProgress("文件", 99, -3);

        MaintenanceProgress p = svc.progress();
        assertEquals(0, p.etaSeconds());
        assertTrue(p.progressMessage().contains("0秒"));
    }

    @Test
    void exit_clearsAllState() {
        MaintenanceModeService svc = new MaintenanceModeService();
        svc.enter(MaintenanceReason.MANUAL);
        svc.updateProgress("完成", 100, 0);

        svc.exit();

        assertFalse(svc.isActive());
        assertNull(svc.reason());
        assertNull(svc.progress());
    }

    @Test
    void renderTemplate_replacesAllPlaceholders() {
        MaintenanceProgress p = MaintenanceProgress.of("区域", 30, 60);
        assertEquals("进度 区域 30 60", MaintenanceModeService.renderTemplate("进度 {stage} {percent} {eta}", p));
    }

    @Test
    void renderTemplate_withoutProgress_returnsTemplateUnchanged() {
        assertEquals("保留 {stage} {eta}", MaintenanceModeService.renderTemplate("保留 {stage} {eta}", null));
    }

    @Test
    void renderTemplate_nullTemplate_returnsEmpty() {
        assertEquals("", MaintenanceModeService.renderTemplate(null, MaintenanceProgress.of("区域", 1, 1)));
    }

    @Test
    void renderTemplate_nullStageKeepsStagePlaceholder() {
        MaintenanceProgress p = new MaintenanceProgress(null, 42, 7, "msg");
        assertEquals("stage={stage} 42 7", MaintenanceModeService.renderTemplate("stage={stage} {percent} {eta}", p));
    }
}
