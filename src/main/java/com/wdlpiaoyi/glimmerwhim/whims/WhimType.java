package com.wdlpiaoyi.glimmerwhim.whims;

import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.resources.ResourceLocation;

public interface WhimType
{
    ResourceLocation id();

    WhimParams params();

    default void on(WhimEvent event)
    {
    }
}
