package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTraces;

import net.minecraft.resources.ResourceLocation;

// 开发用轨迹样式的 id；绘制实现见 DevStrikeTrace（客户端）
public final class DevTraces
{
    public static final ResourceLocation CURRENT = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "current");

    private DevTraces()
    {
    }

    public static void register()
    {
        WhimTraces.register(CURRENT);
    }
}
