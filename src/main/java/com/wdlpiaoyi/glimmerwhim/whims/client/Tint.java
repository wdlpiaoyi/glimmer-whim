package com.wdlpiaoyi.glimmerwhim.whims.client;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

// 「合成」样式的染色：不自己画几何，只给出颜色，覆盖它之后的所有绘制
// stack 是同一种合成样式在链上叠了几层（从 1 起），叠加效果由样式自己定义
@FunctionalInterface
public interface Tint
{
    float[] color(WhimData data, WhimParams params, int stack);
}
