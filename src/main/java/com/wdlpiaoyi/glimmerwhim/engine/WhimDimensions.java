package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;

public final class WhimDimensions
{
    // 修饰符数值域标识：power 威能 / range 范围（配合 WhimModifier.dimension）
    public static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "power");

    public static final ResourceLocation RANGE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "range");

    private WhimDimensions()
    {
    }
}
