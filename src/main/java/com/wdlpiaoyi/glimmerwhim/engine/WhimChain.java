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
    private final Map<ResourceLocation, Double> values; // 各数值域的聚合结果

    public WhimChain(List<Whim> order, WhimTarget target)
    {
        this.order = List.copyOf(order);
        this.target = target;
        this.values = fold(this.order);
    }

    // 按域 id 归并修饰符数值；同名域以链上第一个声明它的修饰符的定义为准
    private static Map<ResourceLocation, Double> fold(List<Whim> order)
    {
        Map<ResourceLocation, WhimDomain> definitions = new LinkedHashMap<>();
        Map<ResourceLocation, List<Double>> collected = new LinkedHashMap<>();

        for (int i = 1; i < order.size(); i++)
        {
            Whim modifier = order.get(i);

            modifier.type().modifier(modifier.data()).ifPresent(output ->
            {
                ResourceLocation id = output.domain().id();

                definitions.putIfAbsent(id, output.domain());
                collected.computeIfAbsent(id, key -> new ArrayList<>()).add(output.value());
            });
        }

        Map<ResourceLocation, Double> folded = new LinkedHashMap<>();

        collected.forEach((id, values) -> folded.put(id, definitions.get(id).fold(values)));

        return folded;
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

    // 读取某数值域的聚合值；链上没有该域时取它自己的中性值
    public double value(WhimDomain domain)
    {
        return this.values.getOrDefault(domain.id(), domain.identity());
    }
}
