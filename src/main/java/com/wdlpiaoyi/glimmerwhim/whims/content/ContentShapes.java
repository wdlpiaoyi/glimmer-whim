package com.wdlpiaoyi.glimmerwhim.whims.content;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimShapes;

import net.minecraft.resources.ResourceLocation;

// 正式外观的 id 登记；画法实现见客户端 ContentRenders
public final class ContentShapes
{
    // 落雷本体的光效
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "glow");

    private ContentShapes()
    {
    }

    public static void register()
    {
        WhimShapes.register(GLOW);
    }
}
