package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
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

    default Set<WhimRole> roles()
    {
        return EnumSet.of(WhimRole.ELEMENT);
    }

    default Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.empty();
    }

    default boolean acceptsTarget(WhimTarget target)
    {
        return true;
    }

    default void on(WhimEvent event)
    {
    }
}
