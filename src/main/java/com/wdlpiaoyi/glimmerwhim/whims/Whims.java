package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public final class Whims
{
    private static final Map<ResourceLocation, WhimType> TYPES = new LinkedHashMap<>();

    static
    {
        register(DevWhim.INSTANCE);
    }

    private Whims()
    {
    }

    public static void register(WhimType type)
    {
        TYPES.put(type.id(), type);
    }

    public static WhimType get(ResourceLocation id)
    {
        return TYPES.get(id);
    }

    public static boolean contains(ResourceLocation id)
    {
        return TYPES.containsKey(id);
    }

    public static Collection<ResourceLocation> ids()
    {
        return TYPES.keySet();
    }
}
