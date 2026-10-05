# 正式内容（规划）

各扩展点的模板骨架见 [`docs/template.md`](template.md)。

正式灵感内容尚未开始加入，本文件用于记录它的定位与规划。

## 是什么

正式内容指面向玩家的具体灵感，构建在引擎、交互、串联与渲染框架之上。

## 与开发测试内容的区别

| | 正式内容 | 开发测试内容 |
| --- | --- | --- |
| 位置 | `whims/` | `whims/dev/` |
| 注册 | 始终注册 | 源码中有 `whims/dev/` 即注册 |

## 每个正式灵感应记录

- id 与命名空间
- 锚与锚数据
- `{参数}`
- 角色（`ELEMENT` / `MODIFIER`）
- 修饰维度（`power` / `range`）
- 可接受的目标类型（`point` 命中点 / `entity` 实体 / `whim` 灵感；新种类在 `engine/WhimTargets` 登记，附带 payload 的位置与描述语义）
- 生命周期
- 生成规则
- 轨迹样式（`element_trace` / `modifier_trace` 的 id，见 `engine/WhimTraces` 与 `whims/client/Traces`，登记经 `whims/client/WhimRenders`）

## 生成

每个 `WhimType` 用 `spawn()` 声明自己的生成规则（生成位置与初始数据）。生成由触发器调用 —— 命令未指定锚时会调用它，类型自身的事件处理也可以调用；引擎不再周期性询问。正式内容的生成规则写在这里。

## 待补充

- 各正式灵感的机制与数值。
- 正式生成规则。
