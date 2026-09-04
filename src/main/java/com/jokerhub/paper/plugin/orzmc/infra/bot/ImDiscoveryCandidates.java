package com.jokerhub.paper.plugin.orzmc.infra.bot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 未绑定会话自动发现（方案 D11）：builtin 平台收到未绑定会话消息时记录候选，
 * 供控制台日志与 /config im status 候选列表提示（不向陌生会话回消息）。
 *
 * <p>绑定成功后调用 {@link #clear(String)} 移除候选。线程安全（WS 回调线程写入、命令线程读取）。</p>
 */
public final class ImDiscoveryCandidates {

    /** 候选容量上限：防止陌生会话轰炸刷爆内存（超限丢弃最旧语义退化为直接丢弃新记录）。 */
    private static final int MAX_CANDIDATES = 256;

    private final ConcurrentMap<String, Long> seen = new ConcurrentHashMap<>(); // target → lastSeenMs

    /** 记录一条未绑定会话候选。 */
    public void record(String target) {
        if (target == null || target.isEmpty()) {
            return;
        }
        if (seen.size() >= MAX_CANDIDATES) {
            seen.clear(); // 粗粒度保护：候选仅提示用途，丢旧保新
        }
        seen.put(target, System.currentTimeMillis());
    }

    /** 绑定成功后移除该会话候选。 */
    public void clear(String target) {
        if (target != null) {
            seen.remove(target);
        }
    }

    /** 当前候选快照（按最近出现倒序），供 status 展示。 */
    public List<Candidate> snapshot() {
        List<Candidate> list = new ArrayList<>();
        seen.forEach((target, lastSeenMs) -> list.add(new Candidate(target, lastSeenMs)));
        list.sort(Comparator.comparingLong(Candidate::lastSeenMs).reversed());
        return list;
    }

    public boolean isEmpty() {
        return seen.isEmpty();
    }

    /** 一条候选：目标会话 + 最近出现时间（毫秒）。 */
    public record Candidate(String target, long lastSeenMs) {}
}
