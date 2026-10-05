# 扩展点模板

`src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/template/` 放着各扩展点的模板骨架，正式内容开发时按需复制。模板文件都写了「这是什么 / 想改时改哪里 / 怎么登记」。其中模板灵感 `glimmerwhim:whim_template` 已作为 dev 内容注册，可直接 `/glimmerwhim summon whim_template` 当活示例；其余模板自身不注册。

| 扩展点 | 模板文件 | 登记在哪 | 可参考的现存实现 |
| --- | --- | --- | --- |
| 灵感（`WhimType`） | `whims/dev/template/WhimTemplate.java` | `whims/WhimContent.register(...)`（模板灵感已在 `whims/dev/DevWhims` 登记） | `whims/dev/DevStrikeWhim.java`、`whims/dev/DevEntityWhim.java` |
| 绘制与轨迹 | `whims/dev/template/client/WhimTemplateRender.java` | 客户端 `whims/client/WhimRenders.register(...)`；入口必须来自 `Dist.CLIENT` 类（如 `whims/dev/client/DevRenders`） | `whims/dev/client/DevStrikeRender.java`、`whims/dev/client/DevStrikeTrace.java` |
| 锚 | `whims/dev/template/AnchorTemplate.java` | `anchor/WhimAnchors.java` 的 `ANCHORS` 加一行（type + read/parse/suggestData + hint） | `anchor/PosAnchor.java`、`anchor/RayAnchor.java`、`anchor/EntityAnchor.java` |
| 目标种类 | `whims/dev/template/TargetTemplate.java`（服务端校验）+ `whims/dev/template/client/TargetTemplatePick.java`（客户端产生） | `engine/WhimTargets.register(...)` + `client/WhimTargeters.register(priority, producer)` | `engine/WhimTargets` 内置 `point`/`entity`/`whim`、`client/WhimTargeting` |
| 数值域 | 无独立文件：域定义写在读取它的元素里（示例见 `WhimTemplate.SCALE`） | 无（定义随内容走，修饰符只交 `WhimModifier(id, value)`） | `parked/` 的 `DevDomains` 及消费它的 `DevExplosionWhim` |

登记与架构的完整说明见仓库根的 `AGENTS.md`，内容规划见 `docs/content.md`。
