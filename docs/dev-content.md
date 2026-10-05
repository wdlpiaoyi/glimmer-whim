# 开发测试内容

本模组的正式灵感内容尚未加入。源码树里目前只保留开发中的 `dev_strike` 与它的蓄力视觉体 `dev_strike_charge`、遮挡测试用 `dev_sighttest`、以及两个只作链修饰符的 `dev_power`、`dev_range`，位于 `src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/`。

其余测试灵感已移到仓库根的 `parked/` 下（镜像原包的目录结构，不在源码树里、不参与构建）。需要时把对应文件移回 `whims/dev/`，并在 `DevWhims` / `DevRenders` 里重新登记即可。

## 启用

没有运行时开关，是否构建进模组取决于文件是否在 `whims/dev/` 里。服务端注册在 `whims/dev/DevWhims`、客户端渲染在 `whims/dev/client/DevRenders`，两者都通过 `@Mod.EventBusSubscriber` 自动注册。

## 类型

| id | 说明 |
| --- | --- |
| `dev_strike` | 近战命中时按概率在水平前方、抬到高空的天空处生成面片（被方块挡住则不生成），默认大小 ×4，只有该玩家可见，生成音效也只发给本人（该位置与寿命由它的 `spawn()` 声明，命令未指定锚时也复用）。拖到可攻击的实体上（优先该玩家最近攻击过的实体，若无任何记录则接受任意实体）：目标发光、warden 蓄力音效（视距内可见可闻），原地留一个自转、不可再交互的蓄力面片，动画结束后在目标（若已消失则在其消失原地）召唤闪电并造成大量伤害；选错目标播放火把熄灭音效；链的轨迹用电流样式（`glimmerwhim:current`）。 |
| `dev_strike_charge` | `dev_strike` 的蓄力视觉体：自转、不可交互、不可被瞄准或串联。 |
| `dev_sighttest` | 遮挡测试用：`{blocks:true\|false}`（默认 true）决定是否被不透明方块挡住，`{entities:true\|false}`（默认 false）决定是否被生物挡住，`{geometry:hitbox\|render}`（默认 hitbox）决定实体遮挡按攻击碰撞箱还是渲染剔除框判定；无锚时生成在视线前方 8 格。被使用时在聊天栏回显其当前开关。 |
| `dev_power` | 只作链修饰符：声明 `glimmerwhim:power` 域，`{amount:2}` 与该域其他值相乘（无修饰符时为中性值 1）。 |
| `dev_range` | 只作链修饰符：声明 `glimmerwhim:range` 域，`{amount:4}` 与该域其他值相加（无修饰符时为中性值 0）。 |

数值域：修饰符用 `WhimType.modifier(WhimData)` 返回 `WhimModifier(domain, value)`，`domain` 是它自己声明的 `WhimDomain`（id + 中性值 + 折叠规则），引擎不登记任何具体域；`WhimChain` 按 id 归并并折值，同名域以链上第一个声明它的修饰符为准，元素用 `chain.value(domain)` 读（链上没有该域即中性值）。示例见已移入 `parked/` 的 `DevExplosionWhim`：半径 = `radius` + range 域，伤害 = `damage` × power 域。

视线开关：每个灵感类型用 `occludedByBlocks(data)` / `occludedByEntities(data)` 声明「什么能挡住它」，`WhimType` 默认方块挡、生物不挡。实体判定几何用 `entityOcclusionRenderBox(data)` 选择攻击箱/渲染剔除框（默认攻击箱）。`dev_strike` 即用该默认（方块挡、生物不挡）。

渲染遮挡：声明被实体遮挡的灵感在绘制时开启深度测试，由场景深度真正挡住像素（因此可出现部分遮挡，露出一半）；其余灵感不做深度测试，保持透过。注意深度测试无法区分方块与实体。方块遮挡始终以灵感中心点单射线 + `canOcclude()` 判定。

## 稳定性

这些内容不受稳定性保证，行为与数值可能随时变动，也可能被直接删除。
