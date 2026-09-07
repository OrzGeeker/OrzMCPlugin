package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import com.jokerhub.paper.plugin.orzmc.infra.config.configs.I18nConfig;
import java.nio.file.Files;
import java.util.Map;

/**
 * 默认语言文案访问器（bot/后台等「非玩家 locale 受众」统一走默认语言 R1）。
 *
 * <p>组合根在 {@code PlatformModule.setup()} 用真实 {@link I18nService} 调 {@link #init} 注入
 * （含 default_lang 配置与数据目录 overlay）；未注入时回落默认 zh 语言包（单测/早期调用）。
 * 线程安全：volatile 引用 + 不可变 {@link I18nService}。</p>
 */
public final class I18nServiceHolder {

    private static volatile I18nService current;
    private static volatile I18nService fallback;

    private I18nServiceHolder() {}

    public static void init(I18nService service) {
        if (service != null) {
            current = service;
        }
    }

    public static I18nService get() {
        I18nService s = current;
        if (s != null) {
            return s;
        }
        I18nService f = fallback;
        if (f == null) {
            try {
                f = new I18nService(
                        I18nServiceHolder.class.getClassLoader(),
                        Files.createTempDirectory("orzmc-default-i18n"),
                        () -> I18nConfig.DEFAULT,
                        null);
            } catch (Exception ignored) {
                f = null;
            }
            fallback = f;
        }
        return f == null ? current : f;
    }

    /** 默认语言取文案。 */
    public static String msg(String key) {
        I18nService s = get();
        return s.msg(s.langFor(), key);
    }

    public static String msg(String key, Map<String, String> vars) {
        I18nService s = get();
        return s.msg(s.langFor(), key, vars);
    }
}
