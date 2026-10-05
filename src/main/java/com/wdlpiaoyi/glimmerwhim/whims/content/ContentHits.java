package com.wdlpiaoyi.glimmerwhim.whims.content;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimHits;

import net.minecraft.resources.ResourceLocation;

// 正式命中体积的 id 登记；判定实现见客户端 ContentRenders
public final class ContentHits
{
    // 落雷本体的球形命中体积
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "glow");

    private ContentHits()
    {
    }

    public static void register()
    {
        WhimHits.register(GLOW);
    }
}
