# parked

这里存放暂时从源码树移出的开发测试灵感。它们**不参与构建**（Gradle 只编译 `src/main/java`），保留是为了以后需要时能直接移回去。

目录结构镜像原来的包路径，例如 `parked/src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/DevWhim.java` 对应原来的 `src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/DevWhim.java`。

## 恢复方法

把要恢复的文件移回源码树，包路径不变：

- 整棵恢复：`parked/src/main/java/com/wdlpiaoyi/glimmerwhim/` 下的内容复制回 `src/main/java/com/wdlpiaoyi/glimmerwhim/`。注册入口 `whims/dev/DevWhims.java` 与 `whims/dev/client/DevRenders.java` 也在这里，必须一起移回。
- 只恢复一部分：类型文件移回 `whims/dev/`（客户端渲染文件移到 `whims/dev/client/` 或 `whims/dev/template/client/`），服务端在 `DevWhims.onCommonSetup` 的 `WhimContent.register(...)` 里加上实例，客户端若有自定义外观在 `DevRenders.onClientSetup` 里补 `WhimRenderer.register(...)`（写法参考模板灵的 `WhimTemplateRender.register()`）。

**恢复前必须先按当前引擎 API 校对**（这些文件停更于旧版本）：

- modid 用 `GlimmerWhim.MODID`，不要再写 `"glimmerwhim"`。
- `WhimChain` 已无 `value(...)` / `factor()`（`DevRootWhim` 受影响）。
- 周期性生成器 `WhimSpawner` / `spawnInterval()` 已删除（`SpawnTestWhim` 受影响）。
- `WhimDimensions` 已删除；修饰符自己声明并携带 `WhimDomain`（`DevBoostWhim`、`DevExplosionWhim` 受影响）。
- `DevPowerWhim` / `DevRangeWhim` / `DevExplosionWhim` 共用同包的 `DevDomains`，恢复时一起移回。
- 本体的画法与命中体积已拆成具名资产：`{shape}` 走 `engine/WhimShapes` + 客户端 `WhimRenders.shape(id, 画法)`、`{hit}` 走 `engine/WhimHits` + `WhimRenders.hitVolume(id, 判定)`，`RenderSpec` 只剩 `highlight` / `vanish`。旧写法 `DefaultRender.draw(...)` / `DefaultRender.hit(..., scale)` / `WhimParam.choice("shape", "quad", "cube")` 都已失效（`WhimTemplate`、`WhimTemplateRender`、`DevWhim`、`DevEntityWhim`、`HighlightTestWhim`、`TraceTestWhim` 受影响），恢复时照 `whims/content/client/StrikeRender` + `ContentRenders` 改。

## 移动原因

正式内容已经开工（`whims/content/` 的落雷 `strike`），源码树里不再保留会自动生成、破坏地形或出现在补全列表里的测试内容：整棵 `whims/dev/`（`dev_entity`、模板 `whim_template`、各扩展点骨架，以及两个注册入口 `DevWhims`/`DevRenders`）都停在这里。引擎侧与目标、数值域、遮挡等说明见 `docs/dev-content.md`。
