# 扩展点模板

`parked/src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/template/` 放着各扩展点的模板骨架（在 `parked/` 里、不参与构建），正式内容开发时按需复制进源码树。模板文件都写了「这是什么 / 想改时改哪里 / 怎么登记」。其中模板灵感 `glimmerwhim:whim_template` 是可直接运行的活示例——把 `whims/dev/` 整个移回源码树（含 `DevWhims`、`client/DevRenders`）后即可 `/glimmerwhim summon whim_template`；其余模板自身不注册。

下表路径相对 `parked/src/main/java/com/wdlpiaoyi/glimmerwhim/`。

| 扩展点 | 模板文件 | 登记在哪 | 可参考的现存实现 |
| --- | --- | --- | --- |
| 灵感（`WhimType`） | `whims/dev/template/WhimTemplate.java` | `whims/WhimContent.register(...)`（模板灵感已在 `whims/dev/DevWhims` 登记） | `whims/content/StrikeWhim.java`、`whims/dev/DevEntityWhim.java` |
| 绘制与轨迹 | `whims/dev/template/client/WhimTemplateRender.java` | 外观：客户端 `WhimRenders.shape(shapeId, 画法)`（id 须先在 `engine/WhimShapes` 登记）；轨迹：`WhimRenders.register(traceId, impl)`。入口必须来自 `Dist.CLIENT` 类（如 `whims/content/client/ContentRenders`） | 外观 `whims/content/client/StrikeRender.java`、轨迹 `whims/content/client/CurrentTrace.java` |
| 命中体积 | `whims/dev/template/client/WhimTemplateRender.java` | 客户端 `WhimRenders.hitVolume(hitId, 判定)`（id 须先在 `engine/WhimHits` 登记），类型用 `defaultHit()` 给默认值；判定大小读 `{hit_scale}`（通用参数，默认 1，只影响判定） | `whims/content/client/StrikeRender.java` 的球形命中；内置 `quad` / `box` / `sphere`（`whims/client/HitVolumes`） |
| 瞄准高亮 | `whims/dev/template/client/WhimTemplateRender.java` | 客户端 `WhimRenders.register(...)` 的 `RenderSpec.highlight(...)` | `whims/client/DefaultRender.outline`、`whims/content/client/StrikeRender.highlight` |
| 消散 | `whims/dev/template/client/WhimTemplateRender.java` 的 `vanish(...)`（骨架「闪烁消散」） | 客户端 `WhimRenders.register(...)` 的 `RenderSpec.vanish(...)`，时长由 `WhimType.vanishMillis(data)` 声明（毫秒，0 = 不演） | `whims/client/ScatterVanish.java`（碎片散开，具名可复用） |
| 锚 | `whims/dev/template/AnchorTemplate.java` | `anchor/WhimAnchors.java` 的 `ANCHORS` 加一行（type + read/parse/suggestData + hint） | `anchor/PosAnchor.java`、`anchor/RayAnchor.java`、`anchor/EntityAnchor.java` |
| 目标种类 | `whims/dev/template/TargetTemplate.java`（服务端校验）+ `whims/dev/template/client/TargetTemplatePick.java`（客户端产生） | `engine/WhimTargets.register(...)` + `client/WhimTargeters.register(priority, producer)` | `engine/WhimTargets` 内置 `point`/`entity`/`whim`、`client/WhimTargeting` |
| 数值域 | 无独立文件：域定义写在读取它的元素里（示例见 `WhimTemplate.SCALE`） | 无（定义随内容走，修饰符只交 `WhimModifier(id, value)`） | `parked/` 的 `DevDomains` 及消费它的 `DevExplosionWhim` |

登记与架构的完整说明见仓库根的 `AGENTS.md`，内容规划见 `docs/content.md`。

## 开发经验（落雷 `strike` 的实践）

写新内容时可以直接照抄的做法：

- **数值默认值读配置**：`params()` 里各参数的默认值别硬编码，读 `WhimConfig`（`strike` 的 `{size}`/`{damage_ratio}`/`{radius}`/`{bolts}`/`{spread}`/`{charge_ticks}`/`{glow_ticks}` 都对应 `[strike]` 里的同名键，参数名跟配置键保持一致）。跟美术资源绑定的留在代码里（如 `{shape}` / `{hit}` 的 id 与类型的 `defaultShape()` / `defaultHit()`）。
- **每次生成略有不同**：在 `spawn()` 里把数值写成区间（`WhimData.range(min, max)`），成形时抽一个值；两端都是整数就抽整数。
- **两段式使用**：要让本体「用完不消失、转入下一状态」就 `consumedOnUse(data)` 返回假，然后在 `on(USE)` 里依次做：`WhimRegistry.update(id, data.with(状态键, "true"), 新可见性)` 改数据与可见性 → `WhimRegistry.freeze(id)` 冻结寿命（不冻结的话本体会在结算前自然到期，连带取消你排的定时任务）→ `WhimScheduler.schedule(...)` 排延迟结算 → 由那次结算自行 `removeWhim(id, USED)`。
- **可见性先独占后公开**：生成时 `{visibility:me}`，命中后用 `update(...)` 改成 `WhimVisibility.ALL`；引擎会给新可见者补发、给不再可见者发移除。
- **冷却与时间窗用玩家状态表**：`WhimPlayerState.mark/active` 按 tick 记在引擎里，跨维度一致。别用 `MobEffect` —— 牛奶、`/effect clear`、其他模组的净化物品都会清掉它。
- **延迟效果用调度器**：`WhimScheduler.schedule(level, owner, delayTicks, action)`，寿命归零或灵感被移除时连带取消；不必自己开静态表 + `ServerTickEvent`。
- **使用被拒**：目标为空/不合法/不被接受时，引擎丢弃整链前会给链根发一次 `REJECT`，在那里给玩家反馈（`strike` 播火把熄灭音）。
- **只给召唤者的生成音效**：写在 `onGenerated(context, placement, whim)` 里（只有生成管线调用，指令召唤不走），用 `player.connection.send(new ClientboundSoundPacket(...))` 定向发包；想让远处也听得见就把音量设成视距 —— `SoundEvent.getRange(volume)` = 16 × 音量。
- **锚的选择**：固定在世界上用 `PosAnchor`；`RayAnchor` 表达的是「视线方向上的无限远物体」（位置按观察者眼睛重算），别把它当固定坐标。
- **遮挡**：`requiresLineOfSight()` 决定会不会被挡，`occludedByBlocks` / `entityOccluders` / `entityOcclusionRenderBox` 决定用什么挡，`depthOcclusion(data)` 返回真则绘制时开深度测试（被挡住的部分不画）。
- **客户端表现**：光效用 `whims/client/Glow` 的图元，尺寸走 `Glow.coreRadius`（跟 `{size}` 与到相机的距离挂钩，屏幕上近似恒定）；画法登记成具名外观（`WhimRenders.shape(id, 画法)` + `defaultShape()`），命中体积单独登记（`WhimRenders.hitVolume(id, 判定)` + `defaultHit()`，内置球/箱/面片，判定大小再乘通用参数 `{hit_scale}`）——两者拆开，改美术不会连带改手感；消散时长由 `vanishMillis` 声明，可复用的消散样式单独放在 `whims/client`（如 `ScatterVanish`）。
- **单位**：逻辑相关用 tick，渲染相关用毫秒。

