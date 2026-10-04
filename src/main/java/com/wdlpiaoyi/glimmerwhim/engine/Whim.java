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
    private final WhimData data;
    private int remaining;

    public Whim(UUID id, WhimAnchor anchor, WhimType type, WhimData data)
    {
        this.id = id;
        this.anchor = anchor;
        this.type = type;
        this.data = data;
        this.remaining = lifetime(data);
    }

    private static int lifetime(WhimData data)
    {
        try
        {
            return Integer.parseInt(data.get(LIFETIME).orElse("-1"));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
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
        return this.remaining < 0 ? this.data : this.data.with(LIFETIME, Integer.toString(this.remaining));
    }

    public int lifetime()
    {
        return this.remaining;
    }

    public boolean permanent()
    {
        return this.remaining < 0;
    }

    public int tick()
    {
        if (this.remaining < 0)
        {
            return this.remaining;
        }

        this.remaining = Math.max(0, this.remaining - 1);

        return this.remaining;
    }
}
