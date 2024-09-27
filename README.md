# OrzMCPlugin

OrzMC Paper Server Plugin

本插件为[私服](https://minecraft.jokerhub.cn)开服自研插件，用来辅助运维。

本插件针对[PaperMC](https://papermc.io/)服务器进行开发，由于`PaperAPI`兼容`BukkitAPI`和`SpigotAPI`，
所以插件开发对有Bukkit和Spigot服务器插件开发经验的开发者比较友好。

[PaperAPI文档](https://papermc.io/javadocs)

[SpigotAPI文档](https://hub.spigotmc.org/javadocs/spigot/)

[Bukkit Wiki](https://bukkit.fandom.com/wiki/Main_Page)

[TextComponent](https://docs.adventure.kyori.net/text.html#creating-components)

---

## Development

本插件开发已同时支持 maven & gradle 构建，具体使用什么方式构建可以根据自己的喜好进行选择

支持命令行方式构建，也可以使用 IntelliJ IDEA

推荐使用 **IntelliJ IDEA**

### maven 构建

使用命令行构建，需要安装 maven 构建工具: `brew install maven`

```bash
$ mvn clean package
```

使用 IntelliJ IDEA 构建：

![maven build](./images/maven_build_guide.png)

### Gradle 构建

使用命令行构建，需要安装 gradle 构建工具： `brew install gradle`
```bash
$ gradle clean shadowJar
```

命令行自动下载服务端并启动运行插件：
```bash
$ gradle runServer
```

使用 IntelliJ IDEA 构建和运行插件：

![gradle build](./images/gradle_build_guide.png)