package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimDomain;

import net.minecraft.resources.ResourceLocation;

// 开发内容自带的数值域，定义随内容走
public final class DevDomains
{
    // 威力：缺省 1（相乘不改变结果），多个修饰符相乘
    public static final WhimDomain POWER = new WhimDomain(id("power"), 1.0D, (a, b) -> a * b);

    // 范围：缺省 0（相加不改变结果），多个修饰符相加
    public static final WhimDomain RANGE = new WhimDomain(id("range"), 0.0D, Double::sum);

    private DevDomains()
    {
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, path);
    }
}
