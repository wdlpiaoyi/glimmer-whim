package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;

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
}
