package com.wdlpiaoyi.glimmerwhim.whims;

import net.minecraft.resources.ResourceLocation;

// 链修饰符输出：domain 是数值域 id、value 为数字；折叠规则由读它的元素定义
public record WhimModifier(ResourceLocation domain, double value)
{
}
