package com.jokerhub.paper.plugin.orzmc.infra.templates;

import com.jokerhub.paper.plugin.orzmc.infra.config.configs.TemplateOptions;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TemplateResolversTest {

    private static TemplateOptions options(Map<String, String> world) {
        return new TemplateOptions("per_sec", "ms", world, 1.0, 2, "block");
    }

    @Test
    public void testWorldAliasInference() {
        TemplateOptions opt = options(new HashMap<>());
        Assertions.assertEquals("主世界", TemplateResolvers.worldAlias("custom_world", "NORMAL", opt));
        Assertions.assertEquals("下界", TemplateResolvers.worldAlias("dim-1", "NETHER", opt));
        Assertions.assertEquals("末地", TemplateResolvers.worldAlias("end", "THE_END", opt));
        Assertions.assertEquals("主世界", TemplateResolvers.worldAlias("unknown_world", null, opt));
        Assertions.assertEquals("主世界", TemplateResolvers.worldAlias("unknown_world", "", opt));
        Map<String, String> m = new HashMap<>();
        m.put("my_world", "我的世界");
        TemplateOptions opt2 = options(m);
        Assertions.assertEquals("我的世界", TemplateResolvers.worldAlias("my_world", "NORMAL", opt2));
    }
}
