package com.jokerhub.paper.plugin.orzmc.features.rank;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RankService 测试：自动晋升判定（读服务器原生 stats 时长）、申请流程。
 *
 * <p>设计（2026-08-07）：
 * <ul>
 *   <li>default→member：累计在线时长（服务器 stats 数据源）达阈值自动晋升</li>
 *   <li>member→builder：/apply 申请，管理员审核（本服务只记录申请状态）</li>
 *   <li>builder→admin：手动（不自动）</li>
 * </ul>
 */
class RankServiceTest {

    private RankStore store;
    private RankPromoter promoter;
    private RankService service;

    @BeforeEach
    void setUp() {
        store = mock(RankStore.class);
        promoter = mock(RankPromoter.class);
        service = new RankService(store, promoter);
    }

    // ---- 自动晋升（default→member）----

    @Test
    void checkPromotion_belowThreshold_doesNotPromote() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(30L); // 0.5h < 10h
        when(promoter.isInGroup(id, "default")).thenReturn(true);

        service.checkPromotion(id);

        verify(promoter, never()).promoteToNext(id);
    }

    @Test
    void checkPromotion_atThreshold_promotesDefaultToMember() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(600L); // 10h
        when(promoter.isInGroup(id, "default")).thenReturn(true);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
    }

    @Test
    void checkPromotion_aboveThreshold_promotesDefaultToMember() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(720L); // 12h
        when(promoter.isInGroup(id, "default")).thenReturn(true);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
    }

    @Test
    void checkPromotion_alreadyMember_doesNotPromote() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(600L);
        when(promoter.isInGroup(id, "default")).thenReturn(false);

        service.checkPromotion(id);

        verify(promoter, never()).promoteToNext(id);
    }

    @Test
    void checkPromotion_offlinePlayer_usesServerStats() {
        // 时长来自 stats（离线可读），玩家不在线也能判断
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(600L);
        when(promoter.isInGroup(id, "default")).thenReturn(true);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
    }

    // ---- 申请流程（member→builder）----

    @Test
    void applyForBuilder_recordsPendingRequest() {
        UUID id = UUID.randomUUID();

        service.applyForBuilder(id);

        verify(store).setPendingApplication(id, true);
    }

    @Test
    void reviewApplication_approve_clearsPending() {
        UUID id = UUID.randomUUID();

        service.reviewApplication(id, true);

        verify(store).setPendingApplication(id, false);
        verify(promoter).promoteToBuilder(id);
    }

    @Test
    void reviewApplication_reject_clearsPending() {
        UUID id = UUID.randomUUID();

        service.reviewApplication(id, false);

        verify(store).setPendingApplication(id, false);
        verify(promoter, never()).promoteToBuilder(id);
    }

    // ---- 阈值配置 ----

    @Test
    void memberThresholdHours_configuredValue() {
        service = new RankService(store, promoter, 5); // 5h 阈值

        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(300L); // 5h
        when(promoter.isInGroup(id, "default")).thenReturn(true);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
    }
}
