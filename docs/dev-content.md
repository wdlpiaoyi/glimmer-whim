# 开发测试内容

本模组的正式灵感内容尚未加入。目前仓库里的所有灵感都是供开发与试验用的测试内容，集中放在 `src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/`，默认**不注册**。

## 启用

在 `config/glimmerwhim-common.toml` 中打开开关：

```toml
[dev]
enabled = true
```

开启后，以下类型在 `glimmerwhim:` 命名空间下注册。客户端渲染注册在 `whims/dev/client/DevRenders`。

## 类型

| id | 说明 |
| --- | --- |
| `dev` | 基础方块/面片；可生成、可被串联，多数测试的默认对象。 |
| `dev_mark` | 接受任意目标，回显目标后消耗。 |
| `dev_entity` | 只接受实体目标。 |
| `dev_coord` | 只接受坐标目标。 |
| `dev_root` | 只有元素角色；回显链的 `power` / `range` / `factor`。 |
| `dev_boost` | 同时可作元素与修饰；提供 `power` 修饰。 |
| `dev_power` / `dev_range` | 纯修饰，分别提供 `power` / `range`。 |
| `highlight_test` | 自声明高亮（色相循环）。 |
| `trace_test` | 自声明牵引折线；`playtime` 控制播放时长。 |
| `expires` | 默认寿命 300 tick 的示例。 |
| `dev_removeonhold` | 被按住时自身消耗。 |
| `dev_removeonrelease` | 被使用时自身消耗。 |
| `spawn_test` | 周期性自动生成（约每 5 秒，附近有数量上限）。 |
| `dev_voidtest` | 持续瞄准后造成伤害、并掉入虚空的测试锚。 |

## 稳定性

这些内容不受稳定性保证，行为与数值可能随时变动，也可能被直接删除。
