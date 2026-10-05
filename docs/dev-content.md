# 开发测试内容

正式内容见 [`docs/content.md`](content.md)。源码树里目前**没有任何开发测试内容**：原先的 `dev_entity`、模板灵感 `whim_template` 与各扩展点骨架（见 [`docs/template.md`](template.md)）都已移到仓库根的 `parked/` 下（镜像原包的目录结构，不在源码树里、不参与构建）。

## 启用

没有运行时开关，是否构建进模组取决于文件是否在 `src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/` 里。现在连两个注册入口 `whims/dev/DevWhims`（服务端）与 `whims/dev/client/DevRenders`（客户端）也停在 `parked/` 里，所以要先移回它们，再把要用的类型文件移回并登记进去。恢复清单与注意事项见 `parked/README.md`。

## 类型（下列类型的文件都在 `parked/` 里）

| id | 说明 |
| --- | --- |
| `whim_template` | 开发正式内容时的活示例骨架：参数、角色、数值域（`glimmerwhim:template_scale`）、目标接受、遮挡、全部事件分支、生成规则与消散都显式写出，轨迹用 `glimmerwhim:template_trace`；正式灵感从 `parked/src/main/java/com/wdlpiaoyi/glimmerwhim/whims/dev/template/WhimTemplate.java` 复制。 |
| `dev_entity` | 用原版望远镜盯住同一实体满 1 秒后，在该实体碰撞箱中心生成跟随该实体移动的棋盘格面片（锚为 `glimmerwhim:entity`），存活 5 秒（数据里记下该实体）；有效目标只能是它自己——先按住（进入），挪开视线，再重新瞄回它，松开时才算命中，成功后在聊天栏输出该实体的 uuid。它的牵引轨迹用曲线样式 `glimmerwhim:curve`（起终点重合时画正对相机的小圆环）。 |

交互目标：目标是「种类 id + payload」；payload 复用 `WhimData`，带命中点。内置种类 `point`（命中点）、`entity`（带实体 uuid）、`whim`（带灵感 uuid）。种类在 `engine/WhimTargets` 登记（id + 提示 + 服务端校验 + payload 的位置/描述语义），客户端产生器在 `client/WhimTargeters` 按优先级登记（内置在 `client/WhimTargeting`：灵感优先于视线命中）。灵感目标在准星于按住之后进入该灵感时产生（链根因为按下时就已进入，必须先离开再回来才会被捕获），服务端再校验该灵感仍在且对玩家可见。

数值域：修饰符用 `WhimType.modifier(WhimData)` 只交出 `WhimModifier(id, value)`（域 id + 数字）；`WhimDomain`（id + 中性值 + 折叠规则）由**读它的一方**（元素）持有，元素用 `chain.value(domain)` 按自己的定义折叠该 id 收到的全部数字（链上没有该 id 即取该定义的中性值）。引擎不登记任何具体域，修饰符也不定义规则。示例见 `parked/`：`DevDomains` 声明 `glimmerwhim:power`（中性值 1、相乘）与 `glimmerwhim:range`（中性值 0、相加），`DevPowerWhim`/`DevRangeWhim` 交出数字，`DevExplosionWhim` 消费（半径 = `radius` + range 域，伤害 = `damage` × power 域）。

视线开关：每个灵感类型用 `occludedByBlocks(data)` / `entityOccluders(data)` 声明「什么能挡住它」，`WhimType` 默认方块挡、生物不挡（`entityOccluders` 返回实体谓词，null 表示不判实体）。实体判定几何用 `entityOcclusionRenderBox(data)` 选择攻击箱/渲染剔除框（默认攻击箱）。`dev_sighttest` 在 `parked/`。正式内容落雷 `strike` 即用该默认（方块挡、生物不挡）；逐类型开关的测试示例 `dev_sighttest` 在 `parked/`。

渲染遮挡：声明 `depthOcclusion(data)` 的灵感在绘制时开启深度测试（默认沿用 `entityOccluders`），由场景深度真正挡住像素（因此可出现部分遮挡，露出一半）；其余灵感不做深度测试，保持透过。注意深度测试无法区分方块与实体。方块遮挡始终以灵感中心点单射线 + `canOcclude()` 判定。

## 稳定性

这些内容不受稳定性保证，行为与数值可能随时变动，也可能被直接删除。
