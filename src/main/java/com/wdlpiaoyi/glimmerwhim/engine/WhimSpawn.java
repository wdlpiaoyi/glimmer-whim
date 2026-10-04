package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;

// 召唤落点：锚点 + 已校验参数
public record WhimSpawn(WhimAnchor anchor, WhimData data)
{
}
