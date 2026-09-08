package com.jokerhub.paper.plugin.orzmc.infra.notify;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.core.bot.MessageEnvelope;
import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ReviewNotifierAdapter 群广播渲染测试（i18n P4b-2）。
 *
 * <p>正文迁语言包 event.* 后 groupEvent 统一走 {@code configs.renderEvent(key, vars)}（renderEvent
 * 内部磁盘正文优先→语言包回落），适配器不再持有内联 fallback——zh 原文与「非字面 {message}」护栏
 * 由 TemplateKeysTest（语言包 event.* 已备）与 TemplateResourceSmokeTest（event.* zh 真实文案）承接。</p>
 */
class ReviewNotifierAdapterTest {

    private TypedConfigProvider configs;
    private Notifier notifier;
    private ReviewNotifierAdapter adapter;

    @BeforeEach
    void setUp() {
        configs = mock(TypedConfigProvider.class);
        notifier = mock(Notifier.class);
        adapter = new ReviewNotifierAdapter(configs, notifier);
        when(configs.renderEvent(anyString(), anyMap())).thenReturn(MessageEnvelope.publicMessage("rendered"));
    }

    @Test
    void groupEvent_rankPromoted_forwardsKeyAndVars() {
        adapter.groupEvent("rank_promoted", Map.of("player", "Alice", "group", "管理员"));

        verify(configs)
                .renderEvent(
                        eq("rank_promoted"),
                        argThat(vars -> "Alice".equals(vars.get("player")) && "管理员".equals(vars.get("group"))));
        verify(notifier).event(eq("rank_promoted"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_rankDemoted_forwardsKeyAndVars() {
        adapter.groupEvent("rank_demoted", Map.of("player", "Alice", "group", "成员"));

        verify(configs).renderEvent(eq("rank_demoted"), anyMap());
        verify(notifier).event(eq("rank_demoted"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_reviewSubmitted_forwardsVars() {
        adapter.groupEvent(
                "review_submitted",
                Map.of(
                        "player", "StyleApp",
                        "type", "晋升建造者",
                        "summary", "申请晋升建造者：想用WorldEdit"));

        verify(configs)
                .renderEvent(
                        eq("review_submitted"),
                        argThat(vars -> "StyleApp".equals(vars.get("player"))
                                && vars.get("summary").contains("WorldEdit")));
        verify(notifier).event(eq("review_submitted"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_reviewCancelled_forwardsKeyAndVars() {
        adapter.groupEvent(
                "review_cancelled", Map.of("player", "StyleApp", "type", "晋升建造者", "summary", "申请晋升 builder：样式测试申请-撤回"));

        verify(configs).renderEvent(eq("review_cancelled"), anyMap());
        verify(notifier).event(eq("review_cancelled"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_reviewApproved_includesReviewerVar() {
        adapter.groupEvent(
                "review_approved",
                Map.of(
                        "player", "StyleApp",
                        "type", "晋升建造者",
                        "summary", "申请晋升建造者：异步修复验证",
                        "reviewer", "StyleAdm"));

        verify(configs)
                .renderEvent(
                        eq("review_approved"),
                        argThat(vars ->
                                "StyleAdm".equals(vars.get("reviewer")) && "StyleApp".equals(vars.get("player"))));
        verify(notifier).event(eq("review_approved"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_reviewRejected_forwardsKeyAndVars() {
        adapter.groupEvent(
                "review_rejected",
                Map.of(
                        "player", "StyleApp",
                        "type", "晋升建造者",
                        "summary", "申请晋升 builder：样式测试申请-拒绝",
                        "reviewer", "StyleAdm"));

        verify(configs).renderEvent(eq("review_rejected"), anyMap());
        verify(notifier).event(eq("review_rejected"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_prisonReleased_forwardsKeyAndVars() {
        adapter.groupEvent("prison_released", Map.of("player", "Steve", "group", "成员"));

        verify(configs).renderEvent(eq("prison_released"), anyMap());
        verify(notifier).event(eq("prison_released"), any(MessageEnvelope.class));
    }

    @Test
    void groupEvent_unknownKey_stillForwardsToRenderEvent() {
        // 未登记键直接透传 renderEvent（其内部对非语言包键走模板记录/空），不抛错不落字面 {message}
        adapter.groupEvent("unknown_event", Map.of());

        verify(configs).renderEvent(eq("unknown_event"), anyMap());
        verify(notifier).event(eq("unknown_event"), any(MessageEnvelope.class));
    }
}
