package com.jokerhub.paper.plugin.orzmc.infra.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 语言包加载器（包内可见）：内置资源（bundled）与数据目录覆盖层（custom）。
 *
 * <p><b>语言码注册点：{@link #CODES}。</b>新增语言 = 在 {@code src/main/resources/messages/} 添加
 * {@code messages_<code>.yml} + 在本列表追加一行，业务代码零改动。</p>
 *
 * <p>bundled 缺失/损坏返回 {@code null}（由调用方告警兜底）；custom 文件不存在返回 {@link MessageTable#EMPTY}，
 * 存在但读取失败抛 {@link IOException}（由调用方捕获并保留上一份覆盖表）。</p>
 */
final class I18nLoader {

    private I18nLoader() {}

    /** 内置资源目录前缀（相对插件 jar 根）。 */
    private static final String BUNDLED_DIR = "messages/";

    /** 已安装语言码（新增语言在此追加一行，与 bundled 资源一一对应）。 */
    static final List<String> CODES = List.of("zh-CN", "en-US");

    static MessageTable bundled(ClassLoader classLoader, String code) {
        String resource = BUNDLED_DIR + "messages_" + code + ".yml";
        try (InputStream in = classLoader.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return parse(in, resource);
        } catch (IOException e) {
            return null;
        }
    }

    static MessageTable custom(Path dataFolder, String code) throws IOException {
        Path file = dataFolder.resolve("messages_custom_" + code + ".yml");
        if (!Files.isRegularFile(file)) {
            return MessageTable.EMPTY;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return MessageTable.from(YamlConfiguration.loadConfiguration(reader));
        }
    }

    private static MessageTable parse(InputStream in, String resource) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        return MessageTable.from(yaml);
    }
}
