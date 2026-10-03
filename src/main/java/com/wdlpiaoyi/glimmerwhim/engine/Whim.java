package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import java.util.UUID;

public final class Whim
{
    private final UUID id;
    private final WhimAnchor anchor;
    private final WhimType type;
    private final WhimData data;
    private int lifetime;

    public Whim(UUID id, WhimAnchor anchor, WhimType type, WhimData data, int lifetime)
    {
        this.id = id;
        this.anchor = anchor;
        this.type = type;
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
