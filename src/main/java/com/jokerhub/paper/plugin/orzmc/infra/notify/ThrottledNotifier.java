package com.jokerhub.paper.plugin.orzmc.infra.notify;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按固定周期限频的通知/告警抑制器。
 *
 * <p>key 在 periodMs 内最多放行一次。已废弃的 {@code tnt.notify_throttle_ms} 读取
 * （上下线限流）已由 {@code player_notify.window_ms} 聚合窗口取代，相关 {@code shouldRunDefault}
 * /{@code runDefault} 死代码一并移除；本类仅保留固定周期限频供与 TNT 无关的告警使用。</p>
 */
public final class ThrottledNotifier {
    private final ConcurrentHashMap<String, Long> last = new ConcurrentHashMap<>();
    private volatile long lastCleanup = 0L;

    /** 按固定周期限频：key 在 periodMs 内最多放行一次，用于与 TNT 无关的告警限频。 */
    public boolean shouldRun(String key, long periodMs) {
        return shouldRun(key, periodMs, periodMs);
    }

    private boolean shouldRun(String key, long periodMs, long ttlMs) {
        long now = System.currentTimeMillis();
        boolean[] updated = {false};
        // compute 原子完成「判定 + 更新」：Folia 多 region 线程并发登录时，同 key 同窗口
        // 的并发调用在 compute 的 bin 锁下串行，杜绝旧实现 check-then-act 的竞态（同窗口发 2 条）。
        last.compute(key, (k, prev) -> {
            if (prev == null || now - prev >= periodMs) {
                updated[0] = true;
                return now;
            }
            return prev;
        });
        maybeCleanup(now, ttlMs);
        return updated[0];
    }

    private void maybeCleanup(long now, long ttlMs) {
        long lc = lastCleanup;
        if (now - lc >= ttlMs) {
            lastCleanup = now;
            last.entrySet().removeIf(e -> now - e.getValue() >= ttlMs);
        }
    }
}
