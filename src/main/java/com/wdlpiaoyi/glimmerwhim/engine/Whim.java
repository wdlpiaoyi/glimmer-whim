package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import java.util.UUID;

public final class Whim
{
    public static final String LIFETIME = "lifetime";
    public static final String VISIBILITY = "visibility";
    public static final String PLAYTIME = "playtime";

    private final UUID id;
    private final WhimAnchor anchor;
    private final WhimType type;
    private WhimData data;

    public Whim(UUID id, WhimAnchor anchor, WhimType type, WhimData data)
    {
        this.id = id;
        this.anchor = anchor;
        this.type = type;
        this.data = data;
    }

    public UUID id()
    {
        return this.id;
    }

    public WhimAnchor anchor()
    {
        return this.anchor;
    }

    public WhimType type()
    {
        return this.type;
    }

    public WhimData data()
    {
        return this.data;
    }

    public int lifetime()
    {
        try
        {
            return Integer.parseInt(this.data.get(LIFETIME).orElse("-1"));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    public boolean permanent()
    {
        return this.lifetime() < 0;
    }

    public int tick()
    {
        int lifetime = this.lifetime();

        if (lifetime < 0)
        {
            return lifetime;
        }

        lifetime = Math.max(0, lifetime - 1);
        this.data = this.data.with(LIFETIME, Integer.toString(lifetime));

        return lifetime;
    }
}
