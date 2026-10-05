package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public final class WhimChain
{
    private final List<Whim> order; // order[0] 为根元素，其余为按点击顺序叠加的修饰符
    private final WhimTarget target;
    private final Map<ResourceLocation, List<Double>> values; // 各数值域收到的数字，折叠规则由读它的元素定义

    public WhimChain(List<Whim> order, WhimTarget target)
    {
        this.order = List.copyOf(order);
        this.target = target;
        this.values = collect(this.order);
    }

    // 按域 id 收集修饰符交出的数字；修饰符不定义折叠规则
    private static Map<ResourceLocation, List<Double>> collect(List<Whim> order)
    {
        Map<ResourceLocation, List<Double>> collected = new LinkedHashMap<>();

        for (int i = 1; i < order.size(); i++)
        {
            Whim modifier = order.get(i);

            modifier.type().modifier(modifier.data()).ifPresent(output ->
                    collected.computeIfAbsent(output.domain(), key -> new ArrayList<>()).add(output.value()));
        }

        return collected;
    }

    public List<Whim> order()
    {
        return this.order;
    }

    public Whim root()
    {
        return this.order.get(0);
    }

    public List<Whim> modifiers()
    {
        return this.order.subList(1, this.order.size());
    }

    public WhimTarget target()
    {
        return this.target;
    }

    // 用调用者给的定义折叠该域的数值；链上没有该域时取定义的中性值
    public double value(WhimDomain domain)
    {
        List<Double> numbers = this.values.get(domain.id());

        return numbers == null ? domain.identity() : domain.fold(numbers);
    }
}
