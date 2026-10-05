# 正式内容（规划）

各扩展点的模板骨架见 [`docs/template.md`](template.md)。

正式内容指面向玩家的具体灵感，构建在引擎、交互、串联与渲染框架之上。目前只有一个正式灵感：落雷 `strike`。

## 与开发测试内容的区别

| | 正式内容 | 开发测试内容 |
| --- | --- | --- |
| 位置 | `whims/content/` | `whims/dev/` |
| 注册 | `ContentWhims`（服务端）/ `ContentRenders`（客户端），始终注册 | 源码中有 `whims/dev/` 即注册 |

## 每个正式灵感应记录

- id 与命名空间
- 锚与锚数据
- `{参数}`（写成 `{名称:最小值..最大值}` 时，灵感成形时在区间内取一个随机值；自然生成与指令召唤都适用）
- 角色（`ELEMENT` / `MODIFIER`）
- 修饰维度（`power` / `range`）
- 可接受的目标类型（`point` 命中点 / `entity` 实体 / `whim` 灵感；新种类在 `engine/WhimTargets` 登记，附带 payload 的位置与描述语义）
- 生命周期
- 生成规则
- 外观与命中体积（`shape` / `hit` 的 id，见 `engine/WhimShapes`、`engine/WhimHits` 与客户端的 `whims/client/Appearances`、`whims/client/HitVolumes`）
- 轨迹样式（`element_trace` / `modifier_trace` 的 id，见 `engine/WhimTraces` 与 `whims/client/Traces`，登记经 `whims/client/WhimRenders`）

## 生成

每个 `WhimType` 用 `spawn()` 声明自己的生成规则（生成位置与初始数据）。自然生成走生成管线 `WhimLifecycle.generate(...)`：抽取 `spawn()` → 生成点被方块挡住就不生成 → 召唤 → 调用类型的 `onGenerated(...)` 做生成表现（指令召唤不走这一步，所以命令召唤的灵感不会放生成音效）。命令未指定锚时也会调用 `spawn()` 取默认落点；引擎不再周期性询问。正式内容的生成规则写在这里。

## 落雷 `strike`

实现见 `whims/content/StrikeWhim.java`，id `glimmerwhim:strike`；触发条件与数值在配置的 `[strike]` 段。

- 生成：必须先处于战斗状态 —— `combat_window_ticks` 内造成或受到过有源伤害（本次命中只刷新标记，不自己当门槛）；随后要求近战命中（`LivingHurtEvent` 的直接来源是玩家）、本次伤害不超过目标最大生命 × `max_damage_ratio`、目标最大生命 ≥ `min_health`、玩家在主世界、距上次抽签 ≥ `roll_interval_ticks`、不在生成冷却中，再按 `chance` 抽签。抽中后沿视线水平方向随机 ±45°、仰角 20°~70°，距离取 128 格与「加载距离 - 32 格」的较小者；生成方向被方块挡住就不生成。本体的锚是 `ray`（用于表现固定方向上的远处物体：位置 = 眼睛 + 方向 × 距离），方向在生成时固定，所以它始终停在同一个方位；只对该玩家可见，寿命 = `cooldown_ticks`，同时也是生成冷却。战斗状态、抽签间隔与冷却都放在 `engine/WhimPlayerState`（键 → 到期 tick，以服务器全局 tick 计数计时，跨维度一致，不落盘、登出丢弃）。
- 蓄力：松开时若目标是生物，本体标上 `charging` 并公开给所有人（蓄力中不可再交互、不可再入链），目标获得 `{glow_ticks}` 刻发光，播放 warden 蓄力音效，`{charge_ticks}` 刻后落雷。蓄力地点固定，不跟随玩家。
- 落雷：本体消失，落点撒 `{bolts}` 道纯视觉闪电（水平散布 `{spread}`），再以**主目标最大生命 × `{damage_ratio}`** 为基准，对半径 `{radius}` 内每个生物按原版爆炸衰减（`g = (1 - d / 2r) × 曝光度`，衰减 `= (g² + g) / 2`）结算伤害与击退；施法者不受影响。伤害是自定义伤害类型 `glimmerwhim:strike`（不带 `BYPASSES_*` 标签，照常受护甲、保护、抗性与无敌帧影响），归属于施法者。
- 目标：只接受 `entity` 种类；没选目标、或目标不被接受（选中方块、选中的实体不是生物等）时播火把熄灭音并按浪费丢弃；目标在蓄力期间消失则打记录点。
- 参数：`{shape:glimmerwhim:glow}` `{hit:glimmerwhim:glow}` `{hit_scale:1}` `{size:4}` `{damage_ratio:1}` `{radius:2}` `{bolts}` `{spread:2}` `{charge_ticks:35}` `{glow_ticks:40}`。`{shape}` 与 `{hit}` 是具名 id（外观、命中体积，可省略 `glimmerwhim:` 前缀），跟美术资源绑定，因此默认值写在代码里而不进配置；`{hit_scale}` 只放大命中体积、不改外观。各数值默认值都来自配置段 `[strike]`（`size`/`damage_ratio`/`radius`/`spread`/`charge_ticks`/`glow_ticks`），参数名与配置键同名，单次召唤可用同名参数覆盖。`{bolts}` 没进配置：默认值由 `{damage_ratio}` 推算（`round(4 + 16 × log(比例 + 1) / log 8)`，比例越大闪电越多），写出来才覆盖。生成时 `spawn()` 把寿命、尺寸与伤害比例写成区间（分别上下浮动 ±20% / ±15% / ±25%），所以每次自然生成的观感与威力都略有不同。
- 瞄准：本体停在最远 128 格的空中，按原尺寸瞄太苛刻；命中体积与画法分开声明，`{hit}` 默认球形、半径对齐可见光核（`{size}` 的一半 × 2.6），`{hit_scale}` 再按倍率缩放判定（只影响判定），所以改成别种外观也不会连带改动手感。目标拾取用实体命中箱，候选实体的箱子按 `[aim] targetHitboxScale`（默认 1.5 倍）以几何中心放大后取最近的一个；多部件实体的部位（例如末影龙的各段）归到本体。
- 轨迹：`glimmerwhim:current`（电流）。
- 表现：不用贴图，画法是具名外观 `glimmerwhim:glow`——加色混合的光核 + 绕不同轴自转的晶体方框 + 光环（见 `whims/content/client/StrikeRender`）。尺寸照 `{size}` 并做距离补偿（`whims/client/Glow`），因此锚越远几何越大、屏幕上差不多；`{damage_ratio}` 越大光环越多越亮；剩余寿命越少脉动越快（倒计时在客户端本地，只影响外观）；蓄力时自转变快变亮，被瞄准时整体放大加亮。消散用「碎片散开」（`whims/client/ScatterVanish`，时长 `vanishMillis` = 550 ms）：本体先亮一下再炸成十几片光片向外飞散。绘制时开深度测试（`depthOcclusion(data)`），因此会被地形与实体部分遮挡。音效占位：生成只让本人听见 wither 环境音、蓄力用 warden 蓄力音、落雷用原版雷电音。

## 待补充

- 其余正式灵感的机制与数值。
