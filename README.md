# TACZ: Better Streamline Bullet

Minecraft 1.21.1 / NeoForge 附属模组。Create: TaCZ 原流水线生产改良弹药；新增高能材料流水线生产精密弹药；TaCZ 工作台生产普通弹药。

- 模组 ID：`tacz_bsb`
- 版本：`1.0.0-beta`（NeoForge，发行文件包含 `neoforge` 标识）
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
- 新增 **85 个材料及加工中物品、64 条配方**，完成全部 24 种精密弹药的生产链。38 个非加工中材料加入 **Create: TaCZ 创造栏**；47 个加工中物品及 Create: TaCZ 原有加工中物品从创造栏和搜索、JEI 搜索中隐藏，复用对应原材料模型并显示原版附魔光效。
- 普通、改良、精密三档可混装；分别记录弹匣顺序、弹膛和装填中的弹药，右下角显示即将击发的类型。
- 两档的直接命中和爆炸倍率可分别配置，保留原距离衰减、爆炸范围及击退。

### 新材料配方

| 材料 / 工序 | 配方 |
| --- | --- |
| 高能火药饼 | 2 火药 + 1 木炭 + 250 mB 水 + 2 玫瑰石英，加热搅拌 → 10 个 |
| 高能干燥火药饼 | 高能火药饼烟熏，时间与原配方相同 |
| 高能火药柱 | 10 高能干燥火药饼，塑型 → 1 个 |
| 高能发射药 | 1 高能干燥火药饼，冲压 → 4 个 |
| 火药衍生物 | 1 高能发射药 ⇄ 5 高能火药丸；1 高能火药丸 ⇄ 4 高能火药颗粒 |
| 高能炸药 | 1 黑曜石粉末 + 2 烈焰粉 + 2 火药，搅拌 → 4 个，无加热要求 |
| 硬化弹头 | 普通小型弹头、大型弹头、金属弹丸分别熔炉烧炼，1 → 1，200 tick |
| 高能已装药弹壳 | 原弹壳与原底火，火药换成高能版本；12g 同时使用硬化金属弹丸 |
| 精密常规弹药 | 高能已装药弹壳 + 硬化弹头，按原装配次数和产量完成 |
| 精密 RPG 战斗部 | 原动力合成中的火药换为高能炸药 |
| 精密 RPG 发动机 / 发射药 | 原动力合成中的火药柱换为高能火药柱 |
| 精密 40mm 榴弹部件 | 炸药装药、引信使用对应高能火药；无引信榴弹使用精密装药及高能发射药 |

不添加高能底火药，也不添加高能发射药与红石合成底火药的配方。40mm 的 `tacz_c:booster_charge_40mm` 保留原物品。RPG 和榴弹部件均使用“精密的”名称，不使用硬化名称。

高能火药饼生产时可在工作盆过滤器中指定高能火药饼，防止材料尚未到齐时提前生产普通火药饼。

所有序列装配严格保留原配方顺序及材料数量。例如 .22 温彻斯特先装底火、后装高能火药；12g 按纸壳、底火、高能火药、弹托、硬化弹丸的顺序进行。共同步骤完成后，机械手按实际投入的火药接续对应产线；一旦投入不同火药便锁定分支。加工中物品独立记录 Create 的装配进度。详细物品 ID 见 [材料清单](docs/materials.md)。

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

| 模组 | 锁定版本 | 构建仓库 |
| --- | --- | --- |
| [TaCZ 1.21.1 NeoForge Port](https://modrinth.com/mod/tacz-1.21.1) | 1.1.8-hotfix-r6 | Modrinth Maven |
| [Create: TaCZ Port](https://www.curseforge.com/minecraft/mc-mods/create-timeless-and-classics-zero-tacz-port/files/8087867) | 1.0.2+neoforge.1.21.1 | Curse Maven |
| [Create](https://modrinth.com/mod/create/version/UjX6dr61) | 6.0.10 | Modrinth Maven |

NeoForge：21.1.248；JDK：21。构建自动下载模组及其发布包内嵌的必要库，无需手动放入 `libs`。JEI 为可选兼容，只在编译时下载其 API；使用 JEI 时自行安装对应游戏版本。

本模组发布 JAR 不内嵌这三个前置模组，游戏客户端和服务端均须安装它们及本模组。Mixin 按表中版本验证；上游调整装填或射击方法后可能需要适配。

## 开发约定

当前及后续代码只维护当前版本的数据格式和行为，不提供跨版本数据迁移、旧格式回退或历史版本兼容分支。TaCZ 新物品的首次初始化、当前依赖接口适配与数据一致性检查属于正常功能。

## 构建与测试

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClientSmoke
```

Linux / macOS 对应使用 `./gradlew`。首次执行需要联网。正式产物：`build/libs/tacz_bsb-neoforge-1.0.0-beta.jar`。

- `build` 包含 JUnit 单元测试，报告在 `build/reports/tests/test/index.html`。
- `runGameTestServer` 在 `build/gametest-run` 运行真实前置模组和集成测试，完成后退出。
- `runClientSmoke` 先运行服务端测试并复制隔离测试世界，再启动客户端，检查 24 种口径的三档弹药渲染、创造栏、状态同步和 HUD，保存截图后自动退出。需要图形环境；截图位于 `build/client-smoke/screenshots/`。
- 日常开发使用 `runClient` / `runServer`。正式 JAR 排除游戏测试类和测试结构。

测试范围与技术边界见 [测试记录](docs/testing.md)。实现细节见 [实现说明](docs/implementation.md)。

本项目基于 NeoForged MDK，其 MIT 声明保留于 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。
