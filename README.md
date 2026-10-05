# 微光奇想 Glimmer Whim

> magic flows through you

一个 Minecraft Forge 模组。核心是**灵感（whim）**——存在于世界中、可被瞄准与交互的非实体对象。

## 当前状态

版本 `0.1.0`，处于早期开发阶段。引擎、交互、串联与渲染框架已经成型，但**正式的灵感内容尚未加入**：源码树里目前只有一个开发中的 `dev_strike`（及其蓄力视觉体 `dev_strike_charge`），其余测试灵感已移到仓库根的 `parked/`（不在源码树里、不参与构建，需要时移回）。当前更适合作为框架与试验场，而不是可直接游玩的模组。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x（开发环境 47.4.10） |
| Java | 17 |

## 安装

1. 为 Minecraft 1.20.1 安装对应版本的 Forge。
2. 将本模组的 jar 放入游戏目录的 `mods/`。

## 核心理念

- **灵感不是实体**，由服务端持有、按维度管理。
- **看到 = 能交互，能交互 = 看到**：客户端只会收到它看得见的灵感；看不见的既不能瞄准也不能交互。
- 不做法力值，也不做知识锁。

## 操作

| 操作 | 默认键位 |
| --- | --- |
| 自由视角 | 左 Alt |
| 灵感交互 | 鼠标右键 |

键位可在「选项 → 控制 → 微光奇想」中修改。

- **看**：准星落在灵感上时，灵感高亮。
- **交互**：按住交互键抓住准星上的灵感；保持按住、用准星扫过其它灵感，会把它们依次接到链上；松开时对瞄准的目标结算。
- **自由视角**：按住可独立转动镜头（镜头位置仍跟随玩家），松开后回正。

## 命令

命令需要权限等级 `command.permissionLevel`（默认 `2`，即 OP）。

```
/glimmerwhim help
/glimmerwhim list [维度|all]
/glimmerwhim summon <灵感类型> [锚类型] [锚数据] {参数}
/glimmerwhim whim <get|kill> [uuid]
```

- `summon` 至少需要灵感类型；省略锚类型或写 `default` 时使用该灵感类型自身的生成规则（`spawn()`），未声明则报错；`default` 不接受锚数据。
- `whim get|kill` 省略 uuid 时作用于当前瞄准的灵感。
- 锚数据与 `{参数}` 均可用 TAB 补全。

示例：

```
/glimmerwhim summon dev_strike
/glimmerwhim summon dev_strike default {lifetime:600}
/glimmerwhim summon dev_strike ray ~ ~ ~ 8 {lifetime:600}
/glimmerwhim summon dev_strike pos ~ ~ ~ {visibility:me}
```

## 锚（anchor）

锚决定灵感出现在世界的什么位置。

| 锚 | 锚数据 | 说明 |
| --- | --- | --- |
| `default` | （无） | 使用该灵感类型自身的生成规则（`spawn()`），不接受锚数据。 |
| `ray` | `dx dy dz [distance]` | 自玩家眼睛沿方向偏移 `distance` 格，默认 `8`。任一分量写 `~` 表示视线方向。 |
| `pos` | `x y z` | 固定坐标；任一分量写 `~` 表示执行者当前位置。 |

## 灵感数据 `{参数}`

一组 `{名称:值}`，用 `,` 或 `;` 分隔。公共参数：

| 名称 | 含义 |
| --- | --- |
| `lifetime` | 寿命（tick）。`-1` = 永久；省略时取该灵感类型的默认值（多数为永久）。 |
| `visibility` | 可见性：`all`（所有人）/ `me`（仅自己）/ 玩家名。默认 `all`。 |

其余参数由各灵感类型自行声明。

## 串联（chain）

- 按住交互键抓住的灵感是**根**（角色 `ELEMENT`）。
- 保持按住、扫过的其它灵感作为**修饰**（角色 `MODIFIER`）依次接在链上。
- 松开时整条链一起结算、一起消耗。
- 链长上限由 `whim.maxChainLength` 决定（默认 `8`，`0` 表示禁止串联）。
- 牵引过程中根消失，或松开时没有有效目标，则整链作废。
- 链上的灵感寿命默认暂停（被按住或作为修饰）；类型声明 `pausesInChain() = false` 时继续倒计时。
- 修饰按维度（`power`、`range`）叠加。

## 生成

每个灵感类型用 `spawn()` 自行声明生成规则（生成位置与初始数据）。生成由触发器调用：命令未指定锚时、以及类型自身的事件处理里会调用它；引擎不再周期性自动生成。灵感超出所有玩家的加载距离时会被移除（原因「超出范围」）。

## 配置

生成于 `config/glimmerwhim-common.toml` 与 `config/glimmerwhim-client.toml`。

| 段 | 作用 |
| --- | --- |
| `aim` | 瞄准是否忽略方块遮挡。 |
| `freelook` | 自由视角模式（按住/切换）、回正时长、灵敏度、角度限制。 |
| `render` | 高亮轮廓、面明暗、瞄准颜色、牵引折线；`render.default` 为没有自定义绘制的元素的默认外观。 |
| `whim` | 串联上限。 |
| `command` | 命令权限等级、TAB 补全的拾取距离。 |
| `debug` | 日志详细程度。 |

## 文档

- [正式内容（规划）](docs/content.md)
- [开发测试内容](docs/dev-content.md)

## 从源码构建

需要 JDK 17。

```
./gradlew build
```

产物位于 `build/libs/`。开发时可运行 `./gradlew runClient`。

## 为开发者

- **新增灵感**：实现 `whims/WhimType`，通过 `whims/WhimContent.register(...)` 注册（引擎会自动把类型上的 `@SubscribeEvent` 挂上 Forge 总线，`bind()` 仅用于第三方总线）。它声明 id、`{参数}`、能否起链（`canRoot()`）、修饰（`modifier()`）、目标限制（`acceptsTarget()`）、事件处理（`on()`）、生成规则（`spawn()`）。
- **新增锚**：实现 `anchor/WhimAnchor`，在 `anchor/WhimAnchors` 的 `ANCHORS` 列表里加一项（类型、`read`、`parse`、`suggestData`、提示）。
- **维度**：`engine/WhimDimensions`（`power` / `range`）。
- **事件**：`engine/WhimEvent`（`SUMMON`、`HIGHLIGHT`、`UNHIGHLIGHT`、`TICK`、`HOLD`、`USE`、`EXPIRE`、`REMOVE`）；`TICK` 需 `ticks()` 开启。
- **调度**：`engine/WhimScheduler`（`schedule` / `scheduleRepeating`），任务以灵感为 owner，灵感移除即取消；灵感到期同样登记为任务，主循环不再逐 tick 扫描寿命。
- **渲染**：客户端用 `whims/client/WhimRenders` 登记某个元素的绘制 / 命中 / 高亮 / 牵引与轨迹样式（底层是 `client/WhimRenderer`）；该注册必须在客户端侧声明（渲染类不能在服务端加载），未登记的灵感用 `render.default` 外观。

## 许可

MIT，见 [LICENSE](LICENSE)。
