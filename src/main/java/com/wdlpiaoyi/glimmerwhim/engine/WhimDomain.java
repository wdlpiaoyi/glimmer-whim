package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;
import java.util.function.DoubleBinaryOperator;

import net.minecraft.resources.ResourceLocation;

// 数值域定义：identity 为没有修饰符时的中性值，combine 决定多个修饰符如何折成一个值
// 定义随内容走（由声明该域的修饰符给出），引擎不登记任何具体域
public record WhimDomain(ResourceLocation id, double identity, DoubleBinaryOperator combine)
{
    public double fold(List<Double> values)
    {
        double result = this.identity;

        for (double value : values)
        {
            result = this.combine.applyAsDouble(result, value);
        }

        return result;
    }
}
