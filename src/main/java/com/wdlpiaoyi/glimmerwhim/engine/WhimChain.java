package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wdlpiaoyi.glimmerwhim.whims.WhimModifier;

import net.minecraft.resources.ResourceLocation;

public final class WhimChain
{
    private final List<Whim> order;
    private final WhimTarget target;

    public WhimChain(List<Whim> order, WhimTarget target)
    {
        this.order = List.copyOf(order);
        this.target = target;
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

    public double value(ResourceLocation dimension)
    {
        double total = 0.0D;

        for (Whim whim : this.modifiers())
        {
            total += modifier(whim, dimension);
        }

        return total;
    }

    public double factor()
    {
        Map<ResourceLocation, Double> totals = new LinkedHashMap<>();

        for (Whim whim : this.modifiers())
        {
            whim.type().modifier(whim.data()).ifPresent(m -> totals.merge(m.dimension(), m.value(), Double::sum));
        }

        double factor = 1.0D;

        for (double value : totals.values())
        {
            factor *= value;
        }

        return factor;
    }

    private static double modifier(Whim whim, ResourceLocation dimension)
    {
        return whim.type().modifier(whim.data())
                .filter(m -> m.dimension().equals(dimension))
                .map(WhimModifier::value)
                .orElse(0.0D);
    }
}
