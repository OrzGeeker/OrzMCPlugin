package com.jokerhub.paper.plugin.orzmc.features.rank;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.features.review.ReviewRequest;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RankService 测试：自动晋升判定（读服务器原生 stats 时长）、当前权限组查询。
 *
 * <p>设计（2026-08-07）：
 * <ul>
 *   <li>default→member：累计在线时长（服务器 stats 数据源）达阈值自动晋升</li>
 *   <li>member→builder：走通用审核框架（ReviewService），本服务不直接受理申请</li>
 *   <li>当前组推断：builder（有 APPROVED 审核记录）&gt; member（promoted 标记）&gt; default</li>
 * </ul>
 */
class RankServiceTest {

    private RankStore store;
    private ReviewStore reviewStore;
    private RankPromoter promoter;
    private RankService service;

    @BeforeEach
    void setUp() {
        store = mock(RankStore.class);
        reviewStore = mock(ReviewStore.class);
        promoter = mock(RankPromoter.class);
        service = new RankService(store, reviewStore, promoter);
    }

    // ---- 自动晋升（default→member）----

    @Test
    void checkPromotion_belowThreshold_doesNotPromote() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(30L); // 0.5h < 10h
        when(store.hasPromoted(id)).thenReturn(false);

        service.checkPromotion(id);

        verify(promoter, never()).promoteToNext(id);
        verify(store, never()).markPromoted(id);
    }

    @Test
    void checkPromotion_atThreshold_promotesDefaultToMember() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(600L); // 10h
        when(store.hasPromoted(id)).thenReturn(false);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
        verify(store).markPromoted(id);
    }

    @Test
    void checkPromotion_aboveThreshold_promotesDefaultToMember() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(720L); // 12h
        when(store.hasPromoted(id)).thenReturn(false);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
        verify(store).markPromoted(id);
    }

    @Test
    void checkPromotion_alreadyPromoted_doesNotPromoteAgain() {
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(600L);
        when(store.hasPromoted(id)).thenReturn(true);

        service.checkPromotion(id);

        verify(promoter, never()).promoteToNext(id);
    }

    @Test
    void checkPromotion_offlinePlayer_usesServerStats() {
        // 时长来自 stats（离线可读），玩家不在线也能判断
        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(600L);
        when(store.hasPromoted(id)).thenReturn(false);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
        verify(store).markPromoted(id);
    }

    // ---- 当前权限组推断 ----

    @Test
    void currentGroup_noPromotion_returnsDefault() {
        UUID id = UUID.randomUUID();
        when(store.hasPromoted(id)).thenReturn(false);
        when(reviewStore.listByApplicant(id)).thenReturn(List.of());

        assertEquals("default", service.currentGroup(id));
    }

    @Test
    void currentGroup_promoted_returnsMember() {
        UUID id = UUID.randomUUID();
        when(store.hasPromoted(id)).thenReturn(true);
        when(reviewStore.listByApplicant(id)).thenReturn(List.of());

        assertEquals("member", service.currentGroup(id));
    }

    @Test
    void currentGroup_approvedBuilderReview_returnsBuilder() {
        UUID id = UUID.randomUUID();
        ReviewRequest approved = new ReviewRequest(
                "r1",
                "builder-promotion",
                id,
                java.util.Map.of("target-group", "builder"),
                ReviewRequest.Status.APPROVED,
                0L,
                1L,
                "admin");
        when(reviewStore.listByApplicant(id)).thenReturn(List.of(approved));

        assertEquals("builder", service.currentGroup(id));
    }

    @Test
    void currentGroup_rejectedBuilderReview_returnsMember() {
        UUID id = UUID.randomUUID();
        ReviewRequest rejected = new ReviewRequest(
                "r1",
                "builder-promotion",
                id,
                java.util.Map.of("target-group", "builder"),
                ReviewRequest.Status.REJECTED,
                0L,
                1L,
                "admin");
        when(reviewStore.listByApplicant(id)).thenReturn(List.of(rejected));
        when(store.hasPromoted(id)).thenReturn(true);

        assertEquals("member", service.currentGroup(id));
    }

    // ---- 阈值配置 ----

    @Test
    void memberThresholdHours_configuredValue() {
        service = new RankService(store, reviewStore, promoter, 5); // 5h 阈值

        UUID id = UUID.randomUUID();
        when(store.getPlaytimeMinutes(id)).thenReturn(300L); // 5h
        when(store.hasPromoted(id)).thenReturn(false);

        service.checkPromotion(id);

        verify(promoter).promoteToNext(id);
        verify(store).markPromoted(id);
    }
}
