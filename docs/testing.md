# 测试记录

日期：2026-09-14。

环境：Minecraft 1.21.1、NeoForge 21.1.248、TaCZ 1.1.8-hotfix-r6、Create: TaCZ 1.0.2、Create 6.0.10，Windows / JDK 21。

最终执行：

```powershell
.\gradlew.bat test runClientSmoke build --console=plain
```

`runClientSmoke` 依赖服务端集成测试和隔离测试世界准备。最终结果：**BUILD SUCCESSFUL**。

## JUnit：7 / 7 通过

- 混装弹匣与独立弹膛的顺序。
- 管式弹仓顺序、手动枪机。
- 缺少供弹时不凭空增加弹药，重复上膛不重复扣弹。
- 连续段编码与边界检查。
- 100 个种子，每个 1,000 次装填、射击、转移、退弹随机操作，每步核对总数和精密弹数量。
- 配方成品替换保留其余内容、不修改原输入，重复执行保持结果一致。
- 未启用的弹药和工作台配方保持原样。

报告：[JUnit HTML](../build/reports/tests/test/index.html)。

## 真实服务端 GameTest：8 / 8 通过

| 项目 | 实际验证内容 |
| --- | --- |
| recipes | 24 条 Create: TaCZ 成品变成精密弹，流水线无残留普通成品；TaCZ 原版 9mm 工作台配方仍存在 |
| mixedReloadUnloadAndSave | 实际物品栏取弹、混装击发、弹膛品质、ItemStack 存档恢复、完整退弹 |
| boxesKeepQuality | 精密弹存取、拒绝普通/精密混存、创造弹药盒无限供弹及品质保持 |
| automaticChamberAndCancellation | TaCZ 原生自动上膛路径；GunShootEvent 取消后保留全部弹药 |
| originalReloadScriptsConserveAmmo | Glock、HK MK23、M870、M1014、Kar98、SPAS-12 的原生/脚本换弹，正常完成及三个中断时机，共 24 组场景；逐步核对总数及精密数量，结束后退弹复核 |
| inventoryFeedAndDummyAmmo | 背包直读的弹膛补弹/精密击发/退弹；虚拟备弹消费与返还 |
| projectileDamage | 实际创建 Glock 和 RPG 投射物，0 / 50 / 150 距离伤害比例为 1.5；RPG 爆炸伤害为 1.5 倍 |
| independentlyConfiguredDamage | 将直接伤害设为 2 倍、爆炸设为 3 倍，验证两个配置分别作用于实际 RPG 投射物 |

服务端测试使用 NeoForge FakePlayer。弹丸测试通过测试专用的实体加入事件取得新投射物引用；正式模组的增伤不依赖此事件或世界实体查询。

## 实际客户端自动检查：通过

- 进入隔离存档并加载 TaCZ 弹药资源。
- 绘制全部 24 个精密弹药图标，检查动态名称、原有贴图与附魔光效；客户端相关 Mixin 正常加载。
- 重建创造栏并断言 TaCZ 原弹药栏中包含 24 个精密变种。
- 从实际集成服务端赋予枪械，断言客户端收到“精密弹膛 + 一发普通弹匣”的数据组件。
- 实际渲染右下角 HUD，截图显示 `Ammo: Precise`，与 TaCZ 自身弹量 HUD 同时存在。
- 保存截图，正常关闭客户端及其集成服务端。

截图：[弹药图标](../build/client-smoke/screenshots/bsb-client-smoke.png)、[HUD](../build/client-smoke/screenshots/bsb-hud-smoke.png)。最终日志：`build/release-validation.log`。

## 发布包检查

`build/libs/tacz_bsb-1.0.0.jar`：50,738 字节。

SHA-256：`28a8827447e3b01d96169b2795a5243188f2b0ece1bd672f77a2abc688da3096`。

已核对模组 ID、名称、作者、MIT 许可证、必选依赖和 Mixin 声明。测试类及测试结构均未进入正式 JAR；源资源 JSON 全部可解析，主源码没有 ExampleMod 占位内容。

## 本次测试的范围

这是基础回归和集成检查，不是所有第三方枪包、复杂整合包或高并发多人服务器的完整验证。测试没有搭建一条实体 Create 生产线运行全套机械流程，而是核对实际加载后的 Create 配方对象；JEI 适配已编译，但本次未安装 JEI 做交互测试。英文客户端 HUD 已实际截图，中英文翻译资源均已包含。

原模组/开发环境存在可选兼容类缺失和弃用提示；本模组相关 Mixin、测试和最终构建均成功。兼容边界详见 [实现说明](implementation.md)。
