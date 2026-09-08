package com.jokerhub.paper.plugin.orzmc.infra.config.configs;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 模板数据选项（非 UI 文案，i18n P4c-2 后仅剩服务器数据/单位 token，留在 templates.yml）：
 * rate/eta 单位、世界别名 {@code world_alias}、坐标格式化 {@code coord}。
 * 阶段显示名映射已迁 {@link MaintenanceTexts}（maintenance.stage.* 语言包承载）。
 */
public record TemplateOptions(
        String rateUnit,
        String etaUnit,
        Map<String, String> worldAlias,
        double coordScale,
        int coordPrecision,
        String coordUnitLabel) {

    public static TemplateOptions from(ConfigurationSection cfg) {
        String rate = cfg.getString("templates.progress_units.rate", "per_sec");
        String eta = cfg.getString("templates.progress_units.eta", "ms");
        Map<String, String> worldAlias = new HashMap<>();
        Object wa = cfg.get("templates.world_alias");
        if (wa instanceof ConfigurationSection sec2) {
            for (String k : sec2.getKeys(false)) {
                String v = sec2.getString(k);
                if (v != null) worldAlias.put(k, v);
            }
        }
        worldAlias.putIfAbsent("world", "主世界");
        worldAlias.putIfAbsent("world_nether", "下界");
        worldAlias.putIfAbsent("world_the_end", "末地");
        double coordScale = cfg.getDouble("templates.coord.scale", 1.0);
        int coordPrecision = cfg.getInt("templates.coord.precision", 2);
        String coordUnitLabel = cfg.getString("templates.coord.unit_label", "block");
        return new TemplateOptions(rate, eta, worldAlias, coordScale, coordPrecision, coordUnitLabel);
    }
}
