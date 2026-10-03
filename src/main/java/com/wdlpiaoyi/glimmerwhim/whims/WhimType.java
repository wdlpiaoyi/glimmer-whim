package com.wdlpiaoyi.glimmerwhim.whims;

import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.resources.ResourceLocation;

public interface WhimType
{
    ResourceLocation id();

    WhimParams params();

    default int defaultLifetime()
    {
        return -1;
    }

    default boolean acceptsTarget(WhimTarget target)
    {
        return true;
    }

    default void on(WhimEvent event)
    {
    }
}
