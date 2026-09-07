package com.jokerhub.paper.plugin.orzmc.testutil;

import com.jokerhub.paper.plugin.orzmc.infra.config.configs.I18nConfig;
import com.jokerhub.paper.plugin.orzmc.infra.i18n.I18nService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * i18n 测试工具：构造基于真实 bundled 语言包（主资源 classpath）的 {@link I18nService}。
 *
 * <p>bundled 随迁移 PR 持续补 key，测试断言可直接用 zh 原文；数据目录用共享临时目录
 * （默认不写覆盖层，等价「未定制服主」路径）。</p>
 */
public final class TestI18n {

    private static final Path DATA_DIR = createTempDir();

    private TestI18n() {}

    public static I18nService newService() {
        return new I18nService(TestI18n.class.getClassLoader(), DATA_DIR, () -> I18nConfig.DEFAULT, null);
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("orzmc-i18n-test");
        } catch (IOException e) {
            throw new IllegalStateException("无法创建 i18n 测试临时目录", e);
        }
    }
}
