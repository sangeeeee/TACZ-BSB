# TACZ: Better Streamline Bullet

[English](README.md) | **简体中文**

![TACZ: Better Streamline Bullet](cores/src/main/resources/logo.png)

Minecraft 1.21.1 / NeoForge 附属模组。Create: TaCZ 原流水线生产改良弹药；新增高能材料流水线生产精密弹药；TaCZ 工作台生产普通弹药。

- 模组 ID：`tacz_bsb`
- 版本：`1.0.0-beta`（产物文件名包含加载器标识）
- 作者：Sange
- 许可证：[MIT](LICENSE)
- Java 包：`com.sange.tacz_bsb`

## 弹药与生产

| 弹药 | 物品 ID | 名称颜色 | 默认直接命中 / 爆炸伤害 | 生产方式 |
| --- | --- | --- | --- | --- |
| 普通 | `tacz:ammo` | TaCZ 原色 | 1 / 1 | TaCZ 工作台 |
| 改良 | `tacz_bsb:improved_ammo` | 橙色 | 1.5 / 1.5 | 原 Create: TaCZ 流水线 |
| 精密 | `tacz_bsb:precise_ammo` | 紫色 | 2 / 2 | 高能材料与硬化弹头流水线 |

- 每档物品通过 TaCZ 的 `AmmoId` 区分口径，默认包含全部 24 种弹药及枪包新增的有效弹药。两档强化弹药均自动使用原弹药名称、模型和原版附魔光效，加入 TaCZ 弹药创造栏，并提供 JEI 子类型识别。
- 原 Create: TaCZ 的 24 条最终装配配方替换为改良弹，保留原配方 ID、产量、概率和工序；工作台配方保留。
- 新增 **88 个材料及加工中物品、64 条配方**，完成全部 24 种精密弹药的生产链。38 个非加工中材料加入 **Create: TaCZ 创造栏**；50 个加工中物品及 Create: TaCZ 原有加工中物品从创造栏和搜索、JEI 搜索中隐藏，复用对应原材料模型并显示原版附魔光效。
- 普通、改良、精密三档可混装；分别记录弹匣顺序、弹膛和装填中的弹药，右下角显示即将击发的类型。
- 两档的直接命中和爆炸倍率可分别配置，保留原距离衰减、爆炸范围及击退。

### 新材料配方

| 材料 / 工序 | 配方 |
| --- | --- |
| 高能火药饼 | 2 火药 + 1 木炭 + 250 mB 水 + 1 玫瑰石英，加热搅拌 → 10 个 |
| 高能干燥火药饼 | 高能火药饼烟熏，时间与原配方相同 |
| 高能柱状火药 | 10 高能干燥火药饼，塑型 → 1 个 |
| 高能发射药 | 1 高能干燥火药饼，冲压 → 4 个 |
| 火药衍生物 | 1 高能发射药 ⇄ 5 高能火药丸；1 高能火药丸 ⇄ 4 高能火药颗粒 |
| 高能炸药 | 1 黑曜石粉末 + 2 烈焰粉 + 2 火药，搅拌 → 4 个，无加热要求 |
| 硬化弹头 | 普通小型弹头、大型弹头、金属弹丸分别注入 100 mB 岩浆 → 辊压一次 → 注入 100 mB 水；序列仅 1 轮，1 → 1 |
| 高能已装药弹壳 | 原弹壳与原底火，火药换成高能版本；12g 同时使用硬化金属弹丸 |
| 精密常规弹药 | 高能已装药弹壳 + 硬化弹头，按原装配次数和产量完成 |
| 精密 RPG 战斗部 | 原动力合成中的火药换为高能炸药 |
| 精密 RPG 发动机 / 发射药 | 原动力合成中的火药柱换为高能柱状火药 |
| 精密 40mm 榴弹部件 | 炸药装药、引信使用对应高能火药；无引信榴弹使用精密装药及高能发射药 |

不添加高能底火药，也不添加高能发射药与红石合成底火药的配方。40mm 的 `tacz_c:booster_charge_40mm` 保留原物品。RPG 和榴弹部件均使用“精密的”名称，不使用硬化名称。

高能火药饼生产时可在工作盆过滤器中指定高能火药饼，防止材料尚未到齐时提前生产普通火药饼。

原有 47 条弹药序列装配严格保留原配方顺序及材料数量。例如 .22 温彻斯特先装底火、后装高能火药；12g 按纸壳、底火、高能火药、弹托、硬化弹丸的顺序进行。共同步骤完成后，机械手按实际投入的火药接续对应产线；一旦投入不同火药便锁定分支。加工中物品独立记录 Create 的装配进度。详细物品 ID 见 [材料注册表](versions/mc-1.21.1/neoforge/src/main/java/com/sange/tacz_bsb/BsbMaterials.java)。

## 混装与退弹规则

从玩家物品栏按 TaCZ 的槽位顺序取弹，使用实际取出的数量与类型记账。弹膛中的子弹优先击发，闭膛枪械击发后从弹匣补入下一发；手动枪机必须拉栓才能继续上膛。

默认将新装入的一批弹药放在剩余弹匣弹药之前，批内保持实际取弹顺序；`tacz:12g` 默认按管式弹仓规则放在末尾。可通过 `fifoAmmo` 调整。这是确定的装填规则，不提供独立的弹药选择快捷键。

TaCZ 自己的退弹路径返还弹匣中的相应品质弹药，并保持其原来的弹膛行为。要连弹膛一起卸空，手持枪械执行：

```mcfunction
/tacz_bsb unload
```

该命令也结束当前换弹/拉栓流程。正常生存弹药按品质返还至物品栏，放不下的部分掉落在玩家身边。

单个弹药盒只存一种口径和一种品质，拒绝混存；取弹不会将改良弹变回普通弹。悬浮窗口中的弹药名称和图标直接显示盒内等级：改良为橙色名称、精密为紫色名称，两者图标均带附魔光效，不另加“内含……”描述。创造弹药盒维持无限供弹规则。虚拟备弹仅为普通品质；燃料式武器的已装入燃料沿用 TaCZ 的卸载不返还规则，避免将一个燃料单位转换成多发物品。无限装填生成的弹药不通过退弹转换成物品。

新增两种全类型创造模式弹药盒，位于 TaCZ 原弹药盒所在的创造栏：

- `tacz_bsb:improved_universal_ammo_box`：改良的全类型创造模式弹药盒，橙色名称。
- `tacz_bsb:precise_universal_ammo_box`：精密的全类型创造模式弹药盒，紫色名称。

两者无限供应当前枪械所需口径的对应品质弹药，无需预先指定口径；新装入的弹药按盒子品质记录，已有弹药保留原品质与顺序。支持标准 TaCZ 枪包及背包直接供弹的枪械，使用原全类型创造盒模型与附魔光效，不添加生存合成配方。

TaCZ 新生成且尚未建立本模组记录的枪械，按原生装弹数量初始化为普通弹。调整创造栏生成范围不会删除已有弹药。

## 配置与枪包扩展

首次启动后生成两个配置文件。添加标准 TaCZ 枪包无需改 Java；模组按枪械的 AmmoId 自动识别三档弹药。枪包需要提供有效的 TaCZ 弹药定义及原始名称、贴图资源。

`config/tacz_bsb-common.toml`：控制两档弹药的创造栏生成范围及改良弹的自动配方替换。建议客户端和服务端配置一致，修改后重启。

```toml
enabledImprovedAmmo = ["*"]
enabledPreciseAmmo = ["*"]
recipeNamespaces = ["tacz_c"]
```

启用列表支持 `*`、`tacz:*`、完整 AmmoId，允许分别留空。默认自动给所有有效弹药提供两档变种。`recipeNamespaces` 中的 Create 序列装配最终普通弹药输出会替换为改良弹。新枪包配方由整合包作者自行添加；本模组只提供默认 Create: TaCZ 的精密生产链。

`tacz_bsb-server.toml`：NeoForge 同步给客户端；修改实例 `config/` 或存档 `serverconfig/` 中实际生成的文件。

```toml
improvedDirectDamageMultiplier = 1.5
improvedExplosionDamageMultiplier = 1.5
preciseDirectDamageMultiplier = 2.0
preciseExplosionDamageMultiplier = 2.0
improvedDamageOverrides = []
preciseDamageOverrides = []
fifoAmmo = ["tacz:12g"]
hudOffsetX = 0
hudOffsetY = 0
```

只为某个扩展包启用改良弹、为另一个启用精密弹的示例：

```toml
# common 文件：替换示例中的枪包 ID
enabledImprovedAmmo = ["tacz:*", "my_pack:custom_round"]
enabledPreciseAmmo = ["tacz:*", "another_pack:custom_round"]
```

```toml
# server 文件：每项依次指定直接命中、爆炸倍率；范围 0–100
improvedDamageOverrides = ["my_pack:custom_round=1.8,1.6"]
preciseDamageOverrides = ["another_pack:custom_round=2.5,3.0"]
```

KubeJS 或数据包配方中的成品条目可直接使用以下格式，不需要改模组代码。将 `precise_ammo` 改为 `improved_ammo` 即得到改良版本：

```json
{
  "id": "tacz_bsb:precise_ammo",
  "count": 16,
  "components": {
    "minecraft:custom_data": { "AmmoId": "my_pack:custom_round" }
  }
}
```

例如 KubeJS 服务端脚本添加搅拌配方：

```js
ServerEvents.recipes(event => {
  event.custom({
    type: 'create:mixing',
    ingredients: [{ item: 'minecraft:iron_ingot' }], // 示例材料，自行设计
    results: [{
      id: 'tacz_bsb:precise_ammo', count: 16,
      components: { 'minecraft:custom_data': { AmmoId: 'my_pack:custom_round' } }
    }]
  })
})
```

直接获取两档弹药：

```mcfunction
/give @s tacz_bsb:improved_ammo[minecraft:custom_data={AmmoId:"tacz:9mm"}] 64
/give @s tacz_bsb:precise_ammo[minecraft:custom_data={AmmoId:"tacz:9mm"}] 64
```

两档为独立注册物品，同一档不同口径是带 AmmoId 的物品变种。配置控制展示和自动替换范围，不清除已存在的物品；固定精密配方也可以由数据包或 KubeJS 修改、移除。

## 必选依赖

以上弹药功能目前由 **NeoForge 1.21.1** 实现。**Forge 1.20.1** 已建立项目结构、加载器入口、依赖及依赖游戏测试，尚未移植弹药功能。

### NeoForge 1.21.1

| 模组 | 锁定版本 | 构建仓库 |
| --- | --- | --- |
| [TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1) | 1.1.8-hotfix-r6 | Modrinth Maven |
| [Create: TaCZ Port](https://www.curseforge.com/minecraft/mc-mods/create-timeless-and-classics-zero-tacz-port/files/8087867) | 1.0.2+neoforge.1.21.1 | Curse Maven |
| [Create](https://modrinth.com/mod/create/version/UjX6dr61) | 6.0.10 | Modrinth Maven |

NeoForge：21.1.248；JDK：21。构建自动下载模组及其发布包内嵌的必要库，无需手动放入 `libs`。JEI 为可选兼容，只在编译时下载其 API；使用 JEI 时自行安装对应游戏版本。

本模组发布 JAR 不内嵌这三个前置模组，游戏客户端和服务端均须安装它们及本模组。Mixin 按表中版本验证；上游调整装填或射击方法后可能需要适配。

### Forge 1.20.1 开发目标

| 模组 | 锁定版本 | 构建仓库 |
| --- | --- | --- |
| [TaCZ](https://modrinth.com/mod/timeless-and-classics-zero/version/AzCBJlex) | 1.1.8-hotfix2 | Modrinth Maven |
| [Create: TaCZ](https://www.curseforge.com/minecraft/mc-mods/tacz-create/files/7327274) | 1.0.2 | Curse Maven |
| [Create](https://modrinth.com/mod/create/version/8amzvn9x) | 6.0.8 | Modrinth Maven |

Forge：47.4.10。游戏运行和目标编译使用 JDK 17；Gradle 启动及 `cores` 编译使用 JDK 21。TaCZ 和 Create 的完整发行包内嵌 SimpleBedrockModel、LuaJ、BCEL、Commons Math、MixinExtras、Registrate、Flywheel 和 Ponder。所有前置包自动下载，不打包进 BSB 产物；无需手动放入 `libs`，也不需要 Forge Config API Port。

## 开发约定

当前及后续代码只维护当前版本的数据格式和行为，不提供跨版本数据迁移、旧格式回退或历史版本兼容分支。TaCZ 新物品的首次初始化、当前依赖接口适配与数据一致性检查属于正常功能。

## 项目结构与任务选择

```text
cores/                          纯 Java 弹药状态逻辑及共用 Logo
versions/
  mc-1.20.1/
    gradle.properties           Minecraft 1.20.1 与 Java 17
    common/                     后续 1.20.1 不依赖加载器的代码及资源
    forge/                      Forge 基础入口、依赖及依赖测试
  mc-1.21.1/
    gradle.properties           Minecraft、映射及 Java 版本
    common/                     1.21.1 不依赖加载器的代码及游戏资源
    neoforge/                   NeoForge 实现、元数据及游戏测试
      gradle.properties         固定的加载器与前置模组版本
```

根目录的 `build`、`assemble`、`check`、`clean` 汇总所有目标；`:mc-1.21.1:build` 构建该版本，`:mc-1.21.1:neoforge:build` 构建其 NeoForge 发行包并检查所用共用模块。各模块的构建产物及开发存档彼此独立。`cores` 与 `common` 的 JAR 为开发库，发行时使用已合并必要内容的 NeoForge JAR。

参考 TravelingMerchantWagon，Gradle 自动发现 `versions/mc-*` 中带有构建脚本的 `common`、`neoforge`、`fabric`、`forge` 模块。1.20.1 Forge 基础项目与 1.21.1 NeoForge 实现分别拥有自己的 common 模块，依赖特定加载器的代码留在各自目标中。4 条使用 NeoForge 流体格式的配方也保留在 NeoForge 资源目录。

运行时指定完整加载器任务。若希望本地使用简短任务名，可创建不提交的 `gradle-local.properties`：

```properties
runTarget=:mc-1.21.1:neoforge
# 可选本地 JDK 路径，多个路径以逗号分隔：
# org.gradle.java.installations.paths=D:/dev/java/jdk17,D:/dev/java/jdk21
```

也可传入 `-PrunTarget=:mc-1.21.1:neoforge`。未选择加载器时，简短运行任务会提示选择目标，避免同时启动多个版本。

## 构建与测试

```powershell
.\gradlew.bat build
.\gradlew.bat :mc-1.20.1:forge:build
.\gradlew.bat :mc-1.20.1:forge:runGameTestServer
.\gradlew.bat :mc-1.21.1:neoforge:runGameTestServer
.\gradlew.bat :mc-1.21.1:neoforge:runClientSmoke
```

Linux / macOS 对应使用 `./gradlew`。首次执行需要联网。正式产物：`versions/mc-1.21.1/neoforge/build/libs/tacz_bsb-neoforge-1.0.0-beta.jar`。

Forge 基础项目产物：`versions/mc-1.20.1/forge/build/libs/tacz_bsb-forge-1.0.0-beta.jar`。它是开发骨架，尚不能使用本模组的弹药功能。`verifyDependencies` 检查三个前置包及其九个内嵌库；`runGameTestServer` 在 `versions/mc-1.20.1/forge/build/gametest-run` 检查实际模组加载、物品注册、TaCZ 枪包加载与 Create: TaCZ 配方解析。日常开发使用 `:mc-1.20.1:forge:runClient` / `:mc-1.20.1:forge:runServer`，工作目录为 `versions/mc-1.20.1/forge/run`。测试代码及结构文件不进入产物。

- `build` 包含 JUnit 单元测试，报告分别在 `cores/build/reports/tests/test/index.html` 和 `versions/mc-1.21.1/common/build/reports/tests/test/index.html`。
- `runGameTestServer` 在 `versions/mc-1.21.1/neoforge/build/gametest-run` 运行真实前置模组和集成测试，完成后退出。
- `runClientSmoke` 先运行服务端测试并复制隔离测试世界，再启动客户端，检查 24 种口径的三档弹药渲染、创造栏、状态同步和 HUD，保存截图后自动退出。需要图形环境；截图位于 `versions/mc-1.21.1/neoforge/build/client-smoke/screenshots/`。
- 日常开发使用 `:mc-1.21.1:neoforge:runClient` / `:mc-1.21.1:neoforge:runServer`。游戏测试独立存放于 `src/gameTest`，正式 JAR 排除其代码和资源。

集成检查见 [CompatibilityGameTests.java](versions/mc-1.21.1/neoforge/src/gameTest/java/com/sange/tacz_bsb/gametest/CompatibilityGameTests.java)。这些检查不代表已穷尽所有第三方枪包和整合包的兼容性。

本项目基于 NeoForged MDK，其 MIT 声明保留于 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。
