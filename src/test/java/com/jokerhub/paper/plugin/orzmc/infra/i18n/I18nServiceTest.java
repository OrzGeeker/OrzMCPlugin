package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jokerhub.paper.plugin.orzmc.infra.config.configs.I18nConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * I18nService 行为测试：语言决议链、zh 回落、custom 覆盖/空串屏蔽、热重载。
 *
 * <p>bundled 资源为空骨架（key 随迁移 PR 补入），本测试通过数据目录覆盖层
 * （messages_custom_<code>.yml）注入语料，验证 bundled ⊕ custom 的分层语义。</p>
 */
class I18nServiceTest {

    @TempDir
    Path dataDir;

    private I18nService service(I18nConfig cfg) {
        return new I18nService(getClass().getClassLoader(), dataDir, () -> cfg, null);
    }

    private void writeCustom(String code, String yamlBody) throws IOException {
        Files.writeString(dataDir.resolve("messages_custom_" + code + ".yml"), yamlBody, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------
    // 语言决议
    // ---------------------------------------------------------------

    @Test
    void langFor_defaultIsZhCn_whenNoConfig() {
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals(Lang.ZH_CN, s.langFor());
        assertEquals(Lang.ZH_CN, s.langFor("qq"));
    }

    @Test
    void langFor_defaultLangUninstalled_fallsBackZhCn() {
        I18nService s = service(new I18nConfig("fr-FR", Map.of(), Map.of()));
        assertEquals(Lang.ZH_CN, s.langFor());
    }

    @Test
    void langFor_defaultLangInstalled_used() {
        I18nService s = service(new I18nConfig("en-US", Map.of(), Map.of()));
        assertEquals(Lang.of("en-US"), s.langFor());
    }

    @Test
    void langFor_playerLocale_resolvesPerClient() {
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals(Lang.of("en-US"), s.langFor(playerWithLocale(new Locale("en", "US"))));
        assertEquals(Lang.of("en-US"), s.langFor(playerWithLocale(Locale.ENGLISH))); // 基础码唯一命中
        assertEquals(Lang.of("zh-CN"), s.langFor(playerWithLocale(Locale.SIMPLIFIED_CHINESE)));
        assertEquals(Lang.ZH_CN, s.langFor(playerWithLocale(Locale.GERMANY))); // 未安装 → 默认
        assertEquals(Lang.ZH_CN, s.langFor((Player) null));
    }

    @Test
    void langFor_platformOverride_applies() {
        I18nService s = service(new I18nConfig("zh-CN", Map.of("qq", "en-US"), Map.of()));
        assertEquals(Lang.of("en-US"), s.langFor("qq"));
        assertEquals(Lang.ZH_CN, s.langFor("discord")); // 未覆盖 → 默认
    }

    @Test
    void langFor_aliasMapsUninstalledCode() {
        I18nService s = service(new I18nConfig("en-US", Map.of(), Map.of("zh-TW", "zh-CN")));
        assertEquals(Lang.of("zh-CN"), s.langFor(playerWithLocale(Locale.TRADITIONAL_CHINESE)));
        assertEquals(Lang.of("en-US"), s.langFor(playerWithLocale(Locale.UK)));
    }

    // ---------------------------------------------------------------
    // 文案读取 / 分层与兜底
    // ---------------------------------------------------------------

    @Test
    void msg_rendersPlaceholders() throws IOException {
        writeCustom("zh-CN", "greet: \"你好 {name}\"");
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals("你好 小明", s.msg(Lang.ZH_CN, "greet", Map.of("name", "小明")));
    }

    @Test
    void msg_missingInEn_fallsBackToZh() throws IOException {
        writeCustom("zh-CN", "only_zh: 仅中文");
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals("仅中文", s.msg(Lang.of("en-US"), "only_zh"));
    }

    @Test
    void msg_enOwnText_usedWhenPresent() throws IOException {
        writeCustom("zh-CN", "greet: 你好");
        writeCustom("en-US", "greet: Hello");
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals("Hello", s.msg(Lang.of("en-US"), "greet"));
        assertEquals("你好", s.msg(Lang.ZH_CN, "greet"));
    }

    @Test
    void msg_missingEverywhere_returnsKey() throws IOException {
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals("no.such.key", s.msg(Lang.ZH_CN, "no.such.key"));
    }

    @Test
    void msg_emptyCustomValue_suppressesMessage() throws IOException {
        writeCustom("zh-CN", "blocked: \"\"\nvisible: 可见");
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals("", s.msg(Lang.ZH_CN, "blocked"));
        assertEquals("可见", s.msg(Lang.ZH_CN, "visible"));
    }

    @Test
    void reloadCustom_picksUpDiskEdits() throws IOException {
        I18nService s = service(I18nConfig.DEFAULT);
        assertEquals("greet", s.msg(Lang.ZH_CN, "greet")); // 覆盖层尚不存在
        writeCustom("zh-CN", "greet: 你好");
        s.reloadCustom();
        assertEquals("你好", s.msg(Lang.ZH_CN, "greet"));
    }

    @Test
    void has_andInstalledLangs() throws IOException {
        writeCustom("zh-CN", "only_custom: 自定义");
        I18nService s = service(I18nConfig.DEFAULT);
        assertTrue(s.installedLangs().contains("zh-CN"));
        assertTrue(s.installedLangs().contains("en-US"));
        assertTrue(s.has(Lang.ZH_CN, "only_custom"));
        assertFalse(s.has(Lang.of("en-US"), "only_custom"));
        assertFalse(s.has(Lang.ZH_CN, "not.there"));
    }

    private static Player playerWithLocale(Locale locale) {
        Player p = mock(Player.class);
        when(p.locale()).thenReturn(locale);
        return p;
    }
}
