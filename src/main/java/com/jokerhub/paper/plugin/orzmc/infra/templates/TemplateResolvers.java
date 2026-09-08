package com.jokerhub.paper.plugin.orzmc.infra.templates;

import com.jokerhub.paper.plugin.orzmc.infra.config.configs.TemplateOptions;

public final class TemplateResolvers {
    private TemplateResolvers() {}

    public static String worldAlias(String worldName, String environment, TemplateOptions opt) {
        String alias = opt.worldAlias().getOrDefault(worldName, null);
        if (alias != null) return alias;
        String env = environment == null ? "" : environment.toUpperCase();
        if ("NETHER".equals(env)) return opt.worldAlias().getOrDefault("world_nether", "下界");
        if ("THE_END".equals(env)) return opt.worldAlias().getOrDefault("world_the_end", "末地");
        return opt.worldAlias().getOrDefault("world", "主世界");
    }
}
