# TACZ: Better Streamline Bullet 功能可行性分析

分析日期：2026-09-14。结论基于静态源码、实际发布 JAR、配方资源检查，尚未实现功能或进行游戏内验证。

## 结论

当前项目锁定版本下，可以实现精密弹药、流水线产物替换、双类型装填、逐发增伤、复用外观和数据驱动扩展。装填状态与自定义渲染需要 Java/Mixin 适配，不能只用数据包实现完整功能。

两项承诺需要限定：
- 如果每个口径必须是独立注册 ID，不能通过普通世界数据包在 /reload 时新增 Item。应在启动阶段读取专用注册配置，客户端和服务端保持一致，并重启。若使用一个新注册物品加 AmmoId，则可按数据动态扩展口径，符合 TaCZ 自身的数据模型。
- 使用标准 TaCZ 枪械和脚本接口的枪包可按弹药 ID 自动兼容；绕开这些接口、自行改写物品数据或产生自定义伤害的枪包，不能保证只改配置即可兼容。

## 检查对象与证据

| 对象 | 版本 / 发布标识 | 检查方式 |
| --- | --- | --- |
| TaCZ NeoForge Port | 1.1.8-hotfix-r6 / wPvGWIj9 | 当前版本发布源码包及运行 JAR |
| Create: TaCZ Port | 1.0.2+neoforge.1.21.1 / CurseForge 8087867 | 实际 JAR 的四个业务类反编译及所有配方 JSON |
| Create | 6.0.10 / UjX6dr61 | 实际 JAR 中 SequencedAssemblyRecipe、ProcessingOutput 反编译 |
| Minecraft / NeoForge | 1.21.1 / 21.1.248 | 当前项目配置；NeoForge 1.21.1 注册文档 |

发布来源：
- [TaCZ](https://modrinth.com/mod/tacz-1.21.1/version/wPvGWIj9)
- [TaCZ 对应源码包](https://cdn.modrinth.com/data/OypNE65K/versions/wPvGWIj9/tacz-neoforge-1.21.1-1.1.8-hotfix-r6-sources.jar)
- [Create: TaCZ 对应文件](https://www.curseforge.com/minecraft/mc-mods/create-timeless-and-classics-zero-tacz-port/files/8087867)
- [Create 对应文件](https://modrinth.com/mod/create/version/UjX6dr61)
- [NeoForge 注册机制](https://docs.neoforged.net/docs/1.21.1/concepts/registries/)

解包及反编译材料位于 build/compat-analysis；该目录为临时分析材料，clean 会删除。未改动 src 中的功能代码。

## 弹药物品与扩展模型

TaCZ 的 ModItems.java:23 只注册一个 tacz:ammo。AmmoItemDataAccessor.java:20–66 使用 minecraft:custom_data 中的 AmmoId 识别口径；isAmmoOfGun 比较 AmmoId 与枪械 GunData.getAmmoId()，没有逐枪检查物品注册 ID。

建议注册 tacz_bsb:precise_ammo，保留原 AmmoId：
- 普通 9mm：tacz:ammo，AmmoId=tacz:9mm。
- 精密 9mm：tacz_bsb:precise_ammo，AmmoId=tacz:9mm。
- 扩展包精密弹：tacz_bsb:precise_ammo，AmmoId=扩展包命名空间:弹药名。

精密弹是真正新增的 Item，不是给普通弹改名。不同口径由组件区分，分别展示、分别堆叠；普通和精密物品也不会堆叠。实现 IAmmo/AmmoItemDataAccessor 或继承 AmmoItem，可复用原枪械的口径匹配，无需逐枪补丁。

数据文件可控制启用口径、倍率、描述以及配方匹配范围。服务端加载后同步到客户端，并在重新加载资源或切换服务器后更新物品展示，不能只读取客户端配置。新增口径仍须由枪包提供有效的 TaCZ 弹药定义及客户端资源；本模组不会凭空生成原弹药数据。

若选择独立注册 ID：可在启动期从专用配置注册如 tacz_bsb:precise/tacz/9mm，数据包负责后续行为和配方；不能依赖普通 COMMON/SERVER 配置的加载时机临时注册，因为 RegisterEvent 发生在普通配置加载之前。

## 流水线配方

Create: TaCZ 的业务类仅负责入口、物品、创造栏和描述；生产逻辑来自数据配方。

实际统计：
- 104 个配方 JSON，另有 recipe 目录条目。
- 51 个 create:sequenced_assembly 配方，其余为切割、机械合成、混合等。
- 其中 24 个成品弹药配方输出 tacz:ammo。
- 输出 AmmoId 的集合与 TaCZ 默认枪包的 24 个弹药索引完全一致，没有缺失或多余项。
- 包括 22 种其他弹药，以及 40mm 榴弹和 rpg_rocket 火箭弹。

例如 tacz_c:bullet_9mm_cap 的 results 使用 id=tacz:ammo，components.minecraft:custom_data.AmmoId=tacz:9mm。

建议在服务端配方加载/重载时，按配方命名空间、类型、输出物品、AmmoId 精确匹配，只替换成品结果。保留配方 ID、材料、加工步骤、循环次数、数量、概率和中间产物。此处的“删除原配方”是有效配方表中不再保留普通弹产出的版本，无需物理删除第三方 JAR 文件，也不应添加一条同材料竞争配方。

保留配方 ID 还有额外好处：Create 的未完成装配物品通过 SequencedAssembly 数据组件引用原配方 ID。替换结果但保留 ID 可避免仅因重命名配方而使在制品失去对应流程。

TaCZ 工作台配方使用 tacz:gun_smith_table_crafting，例如默认包的 recipe/ammo/9mm.json，不属于以上替换范围，因此仍制作普通弹。

ProcessingOutput 已有组件序列化及网络同步，可携带新物品的 AmmoId。应处理完整 results 列表，不只改 getResultItem 的展示结果；SequencedAssemblyRecipe 实际产出读取 resultPool。

新枪包若已有相同结构的流水线配方，扩展匹配规则即可；若没有对应生产配方，仍须通过数据包提供配方，不能自动推断材料与工序。其他模组/整合包额外添加的普通弹自动化途径不在默认替换范围，需要显式纳入规则。

## 装填、逐发类型与持久化

原枪械仅保存 GunCurrentAmmoCount 和膛内有无子弹，没有弹药品质字段。

关键代码：
- AbstractGunItem.findAndExtractInventoryAmmo（238）：逐格抽取 IAmmo，或减少 IAmmoBox 数量，仅返回总数，丢弃具体弹药类型。
- ModernKineticGunItem.defaultReloadFinishing（417）：抽取、补入弹匣、必要时上膛。
- ModernKineticGunScriptAPI.consumeAmmoFromPlayer（542）、putAmmoInMagazine（589）、removeAmmoFromMagazine（611）、setAmmoInBarrel（647）：标准脚本也使用的底层接口。
- ModernKineticGunScriptAPI.reduceAmmoOnce（234）：区分开膛、闭膛、手动拉栓、背包直接供弹。
- AbstractGunItem.dropAllAmmo（169）：现有实现会把退弹统一重建为普通弹，而且不退膛内弹。
- AmmoBoxItem.overrideStackedOnOther（99）：原弹药箱只保存 AmmoId 和数量，取出时使用普通 AmmoItemBuilder。

如果允许混装，必须保存按射出顺序排列的弹匣批次及独立膛内类型，例如 普通×5、精密×10，而不只是一个 precise=true 标记。该状态存入枪械 ItemStack 的持久化数据，并同步必要的客户端显示信息。

类型记录必须来自实际成功抽取的弹药，在实际入匣、上膛和射出时转移。仅在换弹开始时读取背包，或只监听换弹结束事件，无法可靠处理混装、动画中断、背包变化和逐发装填。

如果禁止混装，必须连同“是否能换弹”“可用备弹数量”“实际抽取”一起按品质过滤，并决定膛内剩余弹是否阻止切换；不能只更改伤害逻辑。该路线更简单，但会改变剩弹时的装填规则。

原版闭膛逻辑在弹匣有弹时只减弹匣数量、保持膛内标记不变，这是数量上的简化。新增精确类型后仍需模拟“发射当前膛内弹、下一发进入膛内”的转移。

必须同时适配弹药箱存入/取出/供弹、退弹、换扩容弹匣、丢弃拾取、死亡掉落和重登。否则精密弹经过弹药箱或退弹会变回普通弹，或普通弹错误继承精密属性。弹药箱可选择单品质存储或按品质分批存储，但不可把同 AmmoId 的品质直接合并。

旧存档缺少品质数据时应解释为普通弹；未知或失效 AmmoId 应有明确诊断和保留/禁用策略，不能静默兑换成其他口径。

## 射击与伤害

伤害来自枪械 GunData / BulletData 和配件缓存，不是弹药物品内的一项固定数值。

ModernKineticGunScriptAPI.shootOnce（106）创建连发任务；每次实际击发先调用 reduceAmmoOnce，再生成一个或多个 EntityKineticBullet（188），随后应用霰弹分摊与 shotDamageMultiplier。GunShootEvent 每次扣扳机一次，GunFireEvent 才是每次实际击发，但后者发生在扣弹前且可取消。

因此应在服务端实际扣弹成功后确定本发品质，在生成弹丸时固化该品质与倍率。连发每发独立取值；同一霰弹药产生的所有弹丸共享本发倍率。不能在整个 burst 创建时只取一次品质，也不能在命中时检查玩家当时手持枪或背包来推断旧弹丸品质。

EntityKineticBullet.getDamage（517）按命中距离选择伤害段，经过枪械脚本修正后乘 shotDamageMultiplier；爆头和护甲处理发生在之后。
建议明确定义：相同枪械状态、相同命中距离下，精密弹的“爆头/护甲处理前直接命中伤害”是普通弹的 M 倍，默认 M=1.5。所有原距离阈值与相对衰减保持不变，脚本原有的蓄力等 shotDamageMultiplier 与本模组倍率相乘，不能覆盖。

这一定义是对原本伤害结果加弹药倍率。若“基础伤害”严格指枪包 JSON 中原始 damage 数值，且要求在配件加法/脚本非线性修正之前乘 1.5，则与最终直接命中伤害恒为 1.5 倍不是同一个承诺，需要另定运算顺序。尤其不能只改原始伤害而漏掉 extra_damage 中显式配置的远距离伤害段。

EntityHurtByGunEvent.Pre 可以修改普通目标命中伤害，但靶标分支在此事件前已返回，爆炸也独立处理；仅监听该事件不能完整覆盖所有场景。

榴弹/火箭弹：explosionDamage 在构造函数独立计算，爆炸调用不经过 getDamage。若希望它们的精密版整体有增益，需单独对爆炸伤害应用倍率，保持半径、击退、方块破坏规则和原有爆炸距离计算不变。用户已确认直接命中与爆炸伤害均默认乘 1.5，并分别配置；爆炸半径、衰减和击退保持原版。

避免原地修改共享 GunData 或配件 damageAmount 缓存，以免普通弹或其他同枪型玩家也被增伤。

## 名称、贴图、光效与创造栏

AmmoItem.getName（49）按 AmmoId 取 ClientAmmoIndex 的翻译键；可将该动态名称作为“精密的 %s”翻译参数，不硬编码中文口径名。描述显示实际同步后的倍率，可叠加原弹药说明。

AmmoItemRenderer.renderByItem（72）按 AmmoId 读取：
- GUI 的 slot 贴图。
- 非 GUI 的 Bedrock 模型、材质和变换。

可以直接复用当前枪包提供的资源，无需新增弹药 PNG，也可跟随资源包替换。新物品仍需接入模型入口与自定义物品渲染注册；“不需要新贴图”不代表不需要模型/渲染配置。

该渲染器直接请求普通缓冲区，未读取 stack.hasFoil；BedrockModel 的三维渲染还会自行获取缓冲区。因此只重写 isFoil 或添加附魔光效组件不够，需给 GUI、手持、掉落物、展示框等路径接入光效渲染。仅代理外部 MultiBufferSource 也不足以覆盖自行取缓冲区的三维路径。

ModCreativeTabs.AMMO_TAB 的注册 ID 是 tacz:ammo。可用创造栏构建事件按 TaCZ 当前有效弹药索引加入全部启用精密弹，建议紧邻对应普通弹显示。

若支持 JEI，还需给新物品注册按 AmmoId 区分的 subtype interpreter；TaCZ 当前只对自己的 ModItems.AMMO 注册该解释器。否则可能出现精密弹口径合并和配方检索串项。JEI 可保持可选依赖。

## 枪包兼容边界

可数据驱动覆盖：
- 使用已有 AmmoId 的新枪械。
- 新增有效 AmmoId、名称和材质的标准 TaCZ 枪包。
- 调用上述标准装填/射击脚本 API 的常规自定义动作。

需要重点验证：
- 逐发装填、闭膛与开膛、手动上膛。
- 一次装入多发、一次扳机生成多次射击。
- FUEL 一件燃料换多次射击、INVENTORY 背包直读、DummyAmmo、无限弹药及创造模式。
- 当前默认包已存在 Lua 脚本：如 m870、m1014、kar98、hk_mk23，不能只修改 Java 默认换弹函数。

不能提前承诺仅配置兼容：
- 直接写 NBT/组件、完全绕过标准抽取和入匣接口的脚本。
- 自行生成非 EntityKineticBullet 投射物或自定义伤害的代码模组。
- 无法由标准接口判定弹药物品与射击次数关系的自定义资源消耗机制。

应按装填/射击机制提供可复用适配策略和诊断，而不是为每把枪写死逻辑。

## 实施前需要确定的语义

1. 已确认允许混装，记录弹匣与膛内弹的类型和顺序；HUD 显示当前类型。装填优先级及不同供弹机制的顺序仍需定义。
2. “新增物品”是否接受一个新 Item + AmmoId；独立注册 ID 路线需要启动配置与重启。
3. 已确认：直接命中和爆炸伤害均默认乘 1.5，分别配置；保留原有爆炸范围、衰减和击退。
4. 无限弹药/创造模式没有实际消耗物品时如何选择品质，以及旧枪械默认普通弹的迁移规则。

第 1 项的混装和类型记录要求、第 3 项的伤害倍率已经确认；其余细节尚未全部确定。

## 验收重点

- 24/24 默认弹药精密变种与对应成品配方；原工作台配方结果仍为普通弹。
- 配方 ID、材料、步骤、概率、数量不变；有效配方表没有重复的普通弹流水线结果。
- 两种品质的数量守恒，装填取消、换弹匣、退弹、弹药箱操作及重登后不丢失类型。
- 不同距离的增伤比例正确；普通弹不受影响；霰弹、连发、靶标、爆头和护甲流程正常。
- 榴弹/火箭弹按最终确定的爆炸规则工作。
- 中文/英文名称、动态描述与 GUI/三维光效正确；创造栏和可选 JEI 独立显示各口径。
- 服务端配置同步；加入/离开不同服务器和重新加载数据后不使用旧映射。
- 用一个标准扩展枪包验证无需逐枪代码即可获得精密弹及射击效果。




## 补充：混装、HUD 与避开弹丸运行期改动

用户已确认允许混装，要求保存弹匣及膛内弹药的类型和顺序，并仅在右下角 HUD 显示当前类型。以下是针对新约束的静态分析结论，尚未进行实现验证。

### 推荐状态表示与守恒规则

普通弹匣可使用“弹药类型序列 + 独立膛内弹”。推荐内部使用双端队列或连续同类型批次，例如：
- 膛内：普通。
- 弹匣下一发起：精密×3、普通×5。

栈可以表达顶部装入、顶部供弹的弹匣，但不能预设所有枪械都后进先出。管式弹仓通常从一端装填、另一端供弹；需要按供弹机制选择压入和取出方向。TaCZ 的逻辑模型也不保证为现实中的每个物理弹膛提供单独字段。当前状态模型可保存全部逻辑射出顺序；若要求真实左/右枪管或转轮弹巢位置，需增加相应机制适配，不能由原来的 hasBulletInBarrel 布尔值推断。

HUD 的“当前类型”应明确为：
- 已上膛：当前膛内弹类型。
- 开膛待击：下次从弹匣取出的类型。
- 手动枪膛空、弹匣有弹：显示“未上膛”，可附带待上膛类型，不能暗示已经能射击。
- 全部为空：空仓。
- 背包直读：使用相同选弹规则确定待发类型，实际伤害仍由服务端成功抽取的那发决定。

服务端为唯一权威。一次装填/上膛/退弹是状态转移，而不是在多个事件监听器中分别增减数量。每种 AmmoId + 品质应分别满足：
库存 + 弹药箱 + 弹匣 + 膛内 + 尚未完成归属转移的弹药 = 初始数量 + 合法获得 - 合法消耗。
退弹产生的背包物品、地面掉落及容器转移均须计入，不能只核对枪内总数。

实现约束：
1. 记录实际成功抽取的类型和数量，不以请求抽取数量或之前的背包快照作为依据。
2. 原版数量字段与新增类型序列在同一次服务端逻辑操作结束时一致；不能因嵌套调用原版 setter 再次扣除同一发。
3. 分开跟踪“库存抽出”“装入弹匣”“装入膛内”，防止脚本先抽多发再拆分装填时丢失类型。
4. 中断时只返还尚未提交归属的弹药，已经真正装入的保留。不能因取消动画把已装填弹药全部复制返还。
5. 退弹按实际移出的类型重建物品，保留未被退出的膛内弹；换扩容弹匣、弹药箱存取及供弹均经过同一规则。
6. 旧枪没有新增状态时，将已有数量迁移为普通弹。已有类型数据却发生矛盾时，应诊断和处理异常，不能用“缺多少就补多少精密弹”静默修复。
7. 供弹模式 FUEL、INVENTORY、DummyAmmo、创造/无限弹药需要单独的转换规则；不能把燃料物品数量等同于射击次数。
8. 若需要跨 tick 保留待分配弹药，状态必须有明确归属及生命周期；方法内临时变量不能承担重登恢复。

不能仅靠全局监听 setCurrentAmmoCount 推导变化原因：当前 GunHudOverlay.handleCacheCount 在客户端背包直读模式下还会调用这个 setter，把显示缓存写进去。这不是实际装填，不能触发服务端弹药账本。只有有明确操作语义的服务端入口才能改变精密弹库存状态。

普通按发供弹结束一个完整操作后可检查：
- 弹匣类型序列长度 = GunCurrentAmmoCount。
- 膛内类型是否存在 = HasBulletInBarrel（按枪栓机制解释）。
- 每种品质的减少量 = 实际射出、退还或转移的数量。

可以以这些不变量设计随机操作序列测试，覆盖抽取不足、满背包退弹、动画取消、上膛、战术换弹、空仓换弹、扩容变更、重登与双类型弹药箱。测试及游戏内验证完成前不能承诺“绝对零 bug”，尤其不能对绕开标准接口的第三方脚本作同样保证。

### 不采用用户排除的三种增伤方案

当前源码 ModernKineticGunScriptAPI.shootOnce 中的实际击发回调顺序为：
1. 检查射手/持枪状态和可取消的 GunFireEvent。
2. reduceAmmoOnce 成功后继续。
3. 创建原版 EntityKineticBullet。
4. applyShotgunDamageSpread。
5. setShotDamageMultiplier。
6. 设置弹道并 addFreshEntity。

可在第 2 步确定本发品质，将其作为“本次击发上下文”；在第 5 步把传入倍率改成 原倍率 × 精密直接命中倍率。该调用点直接持有新弹丸对象，不需要通过实体 ID 查询。

应在实际击发回调内取值，而不是在 shootOnce 方法入口取一次值，因为原版把 burst 多发安排在延迟任务中。击发上下文限于本枪、本次实际发射，并在退出时清理；若处理可重入调用，则使用有作用域的上下文栈。不得使用一个全局 lastAmmoType，也不得让已取消/失败的击发沿用上一发类型。

每发散弹只消耗一份弹药，各弹丸均获得同一份倍率。这样直接复用 TaCZ 的距离伤害、爆头、穿甲、靶标、穿透等处理，不改 EntityKineticBullet.tick、getDamage 或 onHitEntity，也不建立子弹子类。

爆炸伤害需另外处理：explosionDamage 是私有字段，没有公共 setter，且不会自动乘 shotDamageMultiplier。两个可行选项：
- 在上述已持有弹丸对象的初始化位置，通过一个字段 Accessor 或 Access Transformer 将现有 explosionDamage 乘精密爆炸倍率；只改本弹丸一次。Accessor 是字段访问桥，AT 是放宽可见性，均不替换爆炸算法。半径、击退、延迟等字段保持不变。
- 若连实体字段访问桥也希望避免，可研究在本次创建弹丸的严格作用域内，适配 ModernKineticGunItem.modifyProperty 对 explosion_damage 的返回值。当前构造函数确实通过该入口计算爆炸伤害；需保证倍率在脚本修正后组合，并验证上下文不会影响其他属性查询。此方案不在弹丸类增加 Mixin，但耦合枪械属性计算路径，未必比单字段访问更稳妥。

第一种更直接。爆炸数值在 addFreshEntity 前完成初始化，原版 writeSpawnData 也会同步该字段。整个方案没有本模组的逐 tick 品质判断，没有按实体 ID 查找增伤，没有新弹丸实体类型。

这仍不是“完全无字节码适配”。至少需要装填/退弹适配与射击调用点适配；爆炸字段也需要访问方式。若进一步要求完全不允许 Mixin、AT 或上游接口改动，则当前公开 API 不足以精确覆盖全部要求。

### HUD 可使用公开事件

TaCZ 在 ClientSetupEvent.onRegisterGuiLayers 将原 HUD 注册为 tacz:tac_gun_hud_overlay。可以监听 NeoForge RenderGuiLayerEvent.Post，识别该图层后在其右下角区域追加一行类型文字，无须 Mixin 或替换 GunHudOverlay.render。

事件中仍需检查隐藏 HUD、TaCZ GUN_HUD_ENABLE、玩家持枪及有效枪械资源等条件。Post 事件只表示图层回调执行结束，不代表该回调一定画出了内容；原 HUD 有多处提前返回。

NeoForge RegisterGuiLayersEvent 的 registerAbove/registerBelow 明确不建议拿其他模组的图层作为注册顺序锚点，因此这里优先使用按图层名称匹配的渲染事件。若其他 UI 模组移动或完全替换了 TaCZ HUD，需要提供坐标偏移或对应 UI 适配，不能保证自动跟随任意第三方布局。

显示状态由服务端同步。客户端只读当前弹类型；不必每帧扫描弹匣或背包，也不必每帧请求网络。若只同步显示摘要，应带持枪标识/状态修订号，避免延迟消息把换枪后的 HUD 改成上一把枪的状态。

### 性能与兼容性判断

类型栈/双端队列的端点操作可做到均摊 O(1)；连续同品质的批次可合并。普通弹匣规模下预期开销很小，但不能称为零开销：持久化复制、序列编码、物品同步仍可能随记录长度增长。尤其将长列表嵌入 custom_data 会影响 TaCZ 原有的 copyTag 调用，不能只估算出栈成本。

应尽量只在状态变化时更新序列和缓存计数，HUD 读缓存摘要；同步整枪物品与本模组摘要的实际开销需在实现后测量，避免额外的重复全量同步。

适配点优先使用保留原调用、可串联的 WrapOperation，而非覆盖整个射击方法；直接命中倍率组合、爆炸只初始化一次，不改共享 GunData/配件缓存。WrapOperation 的串联能力能减少注入冲突，但不能消除其他模组删除调用点、后续重设伤害或绕开标准供弹机制的语义冲突。

依据：
- [MixinExtras WrapOperation 官方说明](https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation)
- [NeoForge Access Transformers](https://docs.neoforged.net/docs/1.21.1/advanced/accesstransformers/)
- NeoForge 21.1.248 本地源码中的 RenderGuiLayerEvent、RegisterGuiLayersEvent。
- TaCZ 1.1.8-hotfix-r6 的 GunHudOverlay、ModernKineticGunScriptAPI、EntityKineticBullet。


