package com.wdlpiaoyi.glimmerwhim.whims;

import net.minecraft.resources.ResourceLocation;

// 链修饰符输出：dimension 指定数值域（见 WhimDimensions），value 为数值
public record WhimModifier(ResourceLocation dimension, double value)
{
}
