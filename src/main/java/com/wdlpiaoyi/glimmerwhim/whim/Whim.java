package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.List;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

public final class Whim
{
    public static final ResourceLocation DEV_ELEMENT = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev");

    public static final List<ResourceLocation> ELEMENTS = List.of(DEV_ELEMENT);

    private final UUID id;
    private final WhimAnchor anchor;
    private final ResourceLocation element;
    private final WhimData data;
    private int lifetime;

    public Whim(UUID id, WhimAnchor anchor, ResourceLocation element, WhimData data, int lifetime)
    {
        this.id = id;
        this.anchor = anchor;
        this.element = element;
        this.data = data;
        this.lifetime = lifetime;
    }

    public UUID id()
    {
        return this.id;
    }

    public WhimAnchor anchor()
    {
        return this.anchor;
    }

    public ResourceLocation element()
    {
        return this.element;
    }

    public WhimData data()
    {
        return this.data;
    }

    public int lifetime()
    {
        return this.lifetime;
    }

    public boolean permanent()
    {
        return this.lifetime < 0;
    }

    public int tick()
    {
        if (this.permanent())
        {
            return this.lifetime;
        }

        this.lifetime = Math.max(0, this.lifetime - 1);
        return this.lifetime;
    }
}
