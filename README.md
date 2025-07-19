# OrzMCPlugin

[私服](https://minecraft.jokerhub.cn)开服自研插件，用来辅助管理员运维。

本插件针对[PaperMC](https://papermc.io/)服务器进行开发，由于`PaperAPI`兼容`BukkitAPI`和`SpigotAPI`，

所以插件开发对有 Bukkit 和 Spigot 插件开发经验的开发者也比较友好。

## Development

本插件构建支持 maven 或 gradle，具体使用什么方式构建可以根据自己的喜好进行选择

支持命令行方式构建，也支持使用IDE开发，推荐使用
**[IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download)** + 
**[Minecraft Development插件](https://plugins.jetbrains.com/plugin/8327-minecraft-development)** 
进行插件开发


> 以下假设你在MacOS上进行插件开发

### 使用 maven 构建

命令行构建，需安装 maven 工具链: `brew install maven`，执行以下命令进行打包：

```bash
$ mvn clean package
```

使用 IntelliJ IDEA CE(社区免费版) 构建：

![maven build](./images/maven_build_guide.png)

### 使用 Gradle 构建

命令行构建，需安装 gradle 工具链： `brew install gradle`，执行以下命令进行打包：

```bash
$ gradle clean shadowJar
```

命令行本地直接调试，自动下载服务端并启动运行插件：

```bash
$ gradle runServer
```

使用 IntelliJ IDEA CE(社区免费版) 构建和运行插件，可以打断点调试，参考文档
[README.md](https://github.com/jpenilla/run-task#basic-usage)
和 [Wiki](https://github.com/jpenilla/run-task/wiki)

![gradle build](./images/gradle_build_guide.png)


## 相关链接

- [PaperAPI文档](https://papermc.io/javadocs)

- [SpigotAPI文档](https://hub.spigotmc.org/javadocs/spigot/)

- [Bukkit Wiki](https://bukkit.fandom.com/wiki/Main_Page)

- [TextComponent](https://docs.adventure.kyori.net/text.html#creating-components)
