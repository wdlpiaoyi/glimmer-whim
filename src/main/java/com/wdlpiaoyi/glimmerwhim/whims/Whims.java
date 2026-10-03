package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;

public final class Whims
{
    private static final Map<ResourceLocation, WhimType> TYPES = new LinkedHashMap<>();

    static
    {
        register(DevWhim.INSTANCE);
        register(RemoveOnHoldWhim.INSTANCE);
        register(RemoveOnReleaseWhim.INSTANCE);
        register(VoidTestWhim.INSTANCE);
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

    public static Optional<ResourceLocation> resolve(String text)
    {
        try
        {
            return Optional.of(text.indexOf(':') >= 0
                    ? ResourceLocation.parse(text)
                    : ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, text));
        }
        catch (RuntimeException e)
        {
            return Optional.empty();
        }
    }
}
