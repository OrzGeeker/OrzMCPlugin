package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 内置语言包一致性护栏（资源级）：
 * <ul>
 *   <li>每个注册语言码都必须有 bundled 资源；</li>
 *   <li>全部语言包与 zh-CN 主目录 key 集完全一致（缺/多都报）；</li>
 *   <li>同 key 跨语言 {@code {var}} 占位符集一致；</li>
 *   <li>bundled 不允许空值（空串是 custom 覆盖层的「屏蔽」语义）。</li>
 * </ul>
 * 运行时同一逻辑由 {@link I18nHealth}（启动健康检查）执行。
 */
class I18nCatalogConsistencyTest {

    private static final ClassLoader CL = Thread.currentThread().getContextClassLoader();

    @Test
    void registeredCodes_allHaveBundledResource() {
        for (String code : I18nLoader.CODES) {
            assertNotNull(
                    CL.getResourceAsStream("messages/messages_" + code + ".yml"),
                    "缺少内置语言包资源: messages/messages_" + code + ".yml");
        }
    }

    @Test
    void zhMaster_hasNoEmptyValues() {
        MessageTable zh = I18nLoader.bundled(CL, "zh-CN");
        assertNotNull(zh);
        assertEquals(List.of(), zh.emptyValueKeys(), "zh-CN 内置包不允许空值");
    }

    @Test
    void allPacks_keySetMatchesZhMaster() {
        MessageTable zh = I18nLoader.bundled(CL, "zh-CN");
        assertNotNull(zh);
        for (String code : I18nLoader.CODES) {
            if ("zh-CN".equals(code)) {
                continue;
            }
            MessageTable other = I18nLoader.bundled(CL, code);
            assertNotNull(other, "缺少内置语言包: " + code);
            assertEquals(zh.keys(), other.keys(), "语言包 " + code + " 与 zh-CN key 集不一致");
        }
    }

    @Test
    void allPacks_placeholderSetMatchesZhPerKey() {
        MessageTable zh = I18nLoader.bundled(CL, "zh-CN");
        assertNotNull(zh);
        for (String code : I18nLoader.CODES) {
            if ("zh-CN".equals(code)) {
                continue;
            }
            MessageTable other = I18nLoader.bundled(CL, code);
            assertNotNull(other);
            for (String key : zh.keys()) {
                if (!other.has(key)) {
                    continue;
                }
                Set<String> zhVars = I18nHealth.placeholderSet(zh.get(key));
                Set<String> otherVars = I18nHealth.placeholderSet(other.get(key));
                assertEquals(zhVars, otherVars, "语言包 " + code + " key=" + key + " 占位符不一致");
            }
        }
    }

    @Test
    void health_checkReportsNoIssuesForConsistentPacks() {
        assertTrue(I18nHealth.check(CL).isEmpty(), "内置语言包应通过一致性健康检查");
    }
}
