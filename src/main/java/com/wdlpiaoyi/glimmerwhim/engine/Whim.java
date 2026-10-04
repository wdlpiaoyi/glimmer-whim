package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import java.util.UUID;

public final class Whim
{
    // 参数键：lifetime 存续 tick（-1 永久）；visibility 可见性；playtime 客户端播放 tick
    public static final String LIFETIME = "lifetime";
    public static final String VISIBILITY = "visibility";
    public static final String PLAYTIME = "playtime";

    private final UUID id;
    private final WhimAnchor anchor;
    private final WhimType type;
    private final WhimData data;
    private final WhimVisibility visibility;
    private int remaining; // 剩余 tick；负值表示永久

    public Whim(UUID id, WhimAnchor anchor, WhimType type, WhimData data, WhimVisibility visibility)
    {
        this.id = id;
        this.anchor = anchor;
        this.type = type;
        this.data = data;
        this.visibility = visibility;
        this.remaining = lifetime(data);
    }

    // 解析 lifetime 参数；缺失或非法一律按永久处理
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

    // 存活时返回递减后的 lifetime；永久时原样返回
    public WhimData data()
    {
        return this.remaining < 0 ? this.data : this.data.with(LIFETIME, Integer.toString(this.remaining));
    }

    public WhimVisibility visibility()
    {
        return this.visibility;
    }

    public int lifetime()
    {
        return this.remaining;
    }

    public boolean permanent()
    {
        return this.remaining < 0;
    }

    // 每服务端 tick 递减一次并钳到 0；永久（负）不递减
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
