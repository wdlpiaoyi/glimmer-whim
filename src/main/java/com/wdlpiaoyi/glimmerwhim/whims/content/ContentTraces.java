package com.wdlpiaoyi.glimmerwhim.whims.content;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTraces;

import net.minecraft.resources.ResourceLocation;

// 正式轨迹样式的 id 登记；绘制实现见客户端 ContentRenders
public final class ContentTraces
{
    // 电流
    public static final ResourceLocation CURRENT = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "current");

    private ContentTraces()
    {
    }

    public static void register()
    {
        WhimTraces.register(CURRENT);
    }
}
