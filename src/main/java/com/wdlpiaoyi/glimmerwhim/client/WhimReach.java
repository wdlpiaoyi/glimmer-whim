package com.wdlpiaoyi.glimmerwhim.client;

import net.minecraft.client.Minecraft;

public final class WhimReach
{
    private WhimReach()
    {
    }

    // 触及距离随有效渲染距离缩放，由引擎侧统一计算
    public static double blocks()
    {
        return com.wdlpiaoyi.glimmerwhim.engine.WhimReach.blocks(
                Minecraft.getInstance().options.getEffectiveRenderDistance());
    }
}
