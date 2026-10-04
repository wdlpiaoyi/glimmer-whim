# parked

这里存放暂时从源码树移出的开发测试灵感。它们**不参与构建**（Gradle 只编译 `src/main/java`），保留是为了以后需要时能直接移回去。

目录结构镜像原来的包路径，例如 `parked/src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/DevWhim.java` 对应原来的 `src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/DevWhim.java`。

## 恢复方法

把要恢复的文件移回 `src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/`（客户端渲染文件移到 `.../whims/dev/client/`）：

- 服务端：在 `whims/dev/DevWhims.onCommonSetup` 的 `WhimContent.register(...)` 里加上实例。
- 客户端：若有自定义外观，在 `whims/dev/client/DevRenders.onClientSetup` 里补 `WhimRenderer.register(...)`。

**恢复前必须先按当前引擎 API 校对**（这些文件停更于旧版本）：

- modid 用 `GlimmerWhim.MODID`，不要再写 `"glimmerwhim"`。
- `WhimChain` 已无 `value(...)` / `factor()`（`DevRootWhim` 受影响）。
- 周期性生成器 `WhimSpawner` / `spawnInterval()` 已删除（`SpawnTestWhim` 受影响）。

## 移动原因

当前只保留正在开发的 `dev_strike`，其余测试类型（`dev`、`dev_mark`、`spawn_test`、`trace_test` 等）从默认构建里移出，避免它们自动生成或出现在补全列表里。参见 `docs/dev-content.md`。
