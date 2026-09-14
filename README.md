# TACZ: Better Streamline Bullet

Minecraft 1.21.1 / NeoForge 附属模组。Create: TaCZ 流水线生产改良弹药；TaCZ 工作台继续生产普通弹药。

- 模组 ID：`tacz_bsb`
- 版本：`1.0.0-beta`（NeoForge，发行文件包含 `neoforge` 标识）
- 作者：Sange
- 许可证：[MIT](LICENSE)
- Java 包：`com.sange.tacz_bsb`

## 已实现的功能

- 自动替换 Create: TaCZ 的 24 条弹药流水线成品输出，保留配方 ID、产量、概率、循环次数和中间步骤。不会另加一份并存的普通弹药流水线配方。
- 新物品 `tacz_bsb:precise_ammo` 使用 TaCZ 的 `AmmoId` 区分口径。默认覆盖全部 24 种弹药，包括榴弹、火箭弹；标准枪包新增弹药也自动获得对应变种。
- 名称自动显示“改良的原弹药名称”，复用原贴图/模型并叠加附魔光效，提供伤害说明，全部加入 TaCZ 原弹药创造栏。提供可选 JEI 子类型识别。
- 同口径普通弹和改良弹可混装。枪械物品分别保存弹匣的逐发顺序、弹膛类型，以及装填过程中尚未落位的弹药。状态随物品存档和网络同步。
- 改良弹的直接命中和爆炸伤害默认分别乘 **1.5**，可以独立配置；保留原有距离衰减、爆炸范围和击退规则。
- TaCZ 右下角 HUD 显示当前弹药类型；空枪与手动枪机尚未上膛时有对应提示。
- 支持原版换弹、逐发装填、拉栓、取消换弹、背包直读、虚拟备弹，以及弹药盒的品质保持。

## 混装与退弹规则

从玩家物品栏按 TaCZ 的槽位顺序取弹，使用实际取出的数量与类型记账。弹膛中的子弹优先击发，闭膛枪械击发后从弹匣补入下一发；手动枪机必须拉栓才能继续上膛。

默认将新装入的一批弹药放在剩余弹匣弹药之前，批内保持实际取弹顺序；`tacz:12g` 默认按管式弹仓规则放在末尾。可通过 `fifoAmmo` 调整。这是确定的装填规则，不提供独立的弹药选择快捷键。

TaCZ 自己的退弹路径返还弹匣中的相应品质弹药，并保持其原来的弹膛行为。要连弹膛一起卸空，手持枪械执行：

```mcfunction
/tacz_bsb unload
```

该命令也结束当前换弹/拉栓流程。正常生存弹药按品质返还至物品栏，放不下的部分掉落在玩家身边。

单个弹药盒只存一种口径和一种品质，拒绝混存；取弹不会将改良弹变回普通弹。创造弹药盒维持无限供弹规则。虚拟备弹没有改良品质；燃料式武器的已装入燃料沿用 TaCZ 的卸载不返还规则，避免将一个燃料单位转换成多发物品。无限装填生成的弹药不通过退弹转换成物品。

TaCZ 新生成且尚未建立本模组记录的枪械，按原生装弹数量初始化为普通弹。禁用某个口径的生成不会删除已有改良弹。

## 配置与枪包扩展

首次启动后生成两个配置文件。

`config/tacz_bsb-common.toml`：在配方加载前读取，控制生成范围。客户端和服务端建议使用相同设置，以保持创造栏一致。

```toml
enabledAmmo = ["*"]
recipeNamespaces = ["tacz_c"]
```

`enabledAmmo` 支持 `*`、`tacz:*` 或完整弹药 ID；留空则不生成改良变种/自动替换配方。`recipeNamespaces` 是需要自动替换流水线成品的配方命名空间列表。修改后重启最稳妥；服务器配方也可以用 `/reload` 刷新。

`tacz_bsb-server.toml`：本次验证环境生成于 `config/`，使用存档专用配置的环境请修改对应 `serverconfig/` 文件。此配置由 NeoForge 同步给客户端。

```toml
directDamageMultiplier = 1.5
explosionDamageMultiplier = 1.5
fifoAmmo = ["tacz:12g"]
damageOverrides = []
hudOffsetX = 0
hudOffsetY = 0
```

例如，为一个标准 TaCZ 扩展包启用自动配方替换，并让它的特殊弹药使用不同增伤：

```toml
# common 文件；示例名称需要换成实际包中的 ID
enabledAmmo = ["tacz:*", "my_pack:*"]
recipeNamespaces = ["tacz_c", "my_pack"]
```

```toml
# server 文件：依次为直接伤害和爆炸伤害倍率
damageOverrides = ["my_pack:special_ammo=1.8,2.0"]
fifoAmmo = ["tacz:12g", "my_pack:shotgun_shell"]
```

枪械通过枪包里的弹药 ID 自动匹配两种品质，不需要为每把枪编写 Java 代码。枪包仍需提供其自身的有效 TaCZ 弹药定义与客户端资源。模组不会为没有流水线配方的扩展弹药凭空设计生产步骤；可用数据包添加符合 Create 格式的配方，再将其命名空间加入上述列表。

也可在数据包的流水线 `results` 中直接指定改良成品：

```json
{
  "id": "tacz_bsb:precise_ammo",
  "count": 16,
  "components": {
    "minecraft:custom_data": { "AmmoId": "my_pack:special_ammo" }
  }
}
```

这是一个成品条目，需放入完整的 Create 配方中。自动替换识别当前移植版使用的对象形式 `minecraft:custom_data`；其他自定义格式可直接使用上面的改良成品格式。

用于调试的获取命令：

```mcfunction
/give @s tacz_bsb:precise_ammo[minecraft:custom_data={AmmoId:"tacz:9mm"}] 64
```

## 必选依赖

| 模组 | 本次验证版本 | 构建仓库 |
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
- `runClientSmoke` 先运行服务端测试并复制隔离测试世界，再启动客户端，检查 24 种弹药渲染、创造栏、状态同步和 HUD，保存截图后自动退出。需要图形环境；截图位于 `build/client-smoke/screenshots/`。
- 日常开发使用 `runClient` / `runServer`。正式 JAR 排除游戏测试类和测试结构。

测试范围与技术边界见 [测试记录](docs/testing.md)。实现细节见 [实现说明](docs/implementation.md)。

本项目基于 NeoForged MDK，其 MIT 声明保留于 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。
