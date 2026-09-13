# TACZ: Better Streamline Bullet

面向 Minecraft 1.21.1 / NeoForge 的 TaCZ 与 Create: TaCZ 附属模组。
目前仅完成基础配置，尚未实现子弹相关功能。

- 模组 ID：`tacz_bsb`
- 作者：Sange
- 许可证：[MIT](LICENSE)
- Java 包：`com.sange.tacz_bsb`
- 入口类：`TaczBetterStreamlineBullet`

## 必选依赖

| 模组 | 构建使用的版本 | 下载仓库 |
| --- | --- | --- |
| [TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1) | 1.1.8-hotfix-r6 | Modrinth Maven |
| [Create: Timeless and Classics Zero Port](https://www.curseforge.com/minecraft/mc-mods/create-timeless-and-classics-zero-tacz-port/files/8087867) | 1.0.2+neoforge.1.21.1 | Curse Maven |
| [Create](https://modrinth.com/mod/create/version/UjX6dr61) | 6.0.10 | Modrinth Maven |

这些依赖同时加入编译和开发运行环境，并在模组元数据中声明为必选。
Create 的完整发布包已内嵌 Registrate、Flywheel 和 Ponder；TaCZ 的发布包已内嵌其必要库，随依赖包一同下载并由 NeoForge 加载。
不需要手动将依赖复制到 `libs`，也不需要额外下载这些内嵌库。

## 构建与开发

需要 JDK 21。Windows 下运行：

```powershell
.\gradlew.bat build
```

Linux / macOS 下运行 `./gradlew build`。
首次构建需要联网，Gradle 自动从远程仓库下载依赖至 Gradle 用户缓存，并显示在 IDE 的依赖库中。
构建产物位于 `build/libs/tacz_bsb-1.0.0.jar`。
版本与依赖坐标统一配置在 `gradle.properties` 中。

开发客户端与服务端分别使用 `runClient` 和 `runServer` 任务。
发布的本模组 JAR 不打包上述前置模组；实际游戏环境需安装表中三个模组及 NeoForge。

本项目使用 NeoForged MDK，原模板的 MIT 声明保留于 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。
