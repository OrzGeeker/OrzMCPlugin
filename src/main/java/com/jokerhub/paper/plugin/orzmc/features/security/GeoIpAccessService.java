package com.jokerhub.paper.plugin.orzmc.features.security;

import com.jokerhub.paper.plugin.orzmc.core.ports.config.TypedConfigProvider;
import com.jokerhub.paper.plugin.orzmc.infra.net.GeoIpClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GeoIpAccessService {
    /**
     * 单次登录时 GeoIP 查询的阻塞等待上限（毫秒）。
     *
     * <p>阻塞发生在异步的 AsyncPlayerPreLoginEvent 处理器线程（netty 线程），不会阻塞主线程。
     * 超时未拿到结果则 fail-open 放行，并告警到日志与群。</p>
     */
    public static final long DECISION_TIMEOUT_MS = 3_000L;

    public record Decision(boolean allowed, String countryCode, List<String> allowList, String rawJson) {}

    private final GeoIpClient client;
    private final TypedConfigProvider configs;

    public GeoIpAccessService(TypedConfigProvider configs) {
        this(new GeoIpClient(), configs);
    }

    GeoIpAccessService(GeoIpClient client, TypedConfigProvider configs) {
        this.client = client;
        this.configs = configs;
    }

    public CompletableFuture<Decision> decide(String ipAddress) {
        List<String> allow = configs.ipWhitelist().allowCountryCode();
        if (allow.isEmpty()) {
            return CompletableFuture.completedFuture(new Decision(true, "", allow, ""));
        }
        return client.lookup(ipAddress).handle((res, ex) -> {
            if (ex != null || res == null) {
                return new Decision(true, "", allow, "");
            }
            String cc = res.countryCode() == null ? "" : res.countryCode();
            boolean ok = allow.contains(cc);
            return new Decision(ok, cc, allow, res.rawJson());
        });
    }
}
