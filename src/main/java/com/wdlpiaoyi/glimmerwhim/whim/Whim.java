package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * 一条灵感。
 * <p>
 * intensity、source、visibility 这些可选字段这一版一个都不写。
 * 不是实体，不落盘 —— 活几十秒的东西没有存档价值。
 */
public final class Whim
{
    /** 这一版所有灵感都用这个 element。 */
    public static final ResourceLocation DEV_ELEMENT = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev");

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

    /** 这个元素类型自己的参数；没写就是空的。 */
    public WhimData data()
    {
        return this.data;
    }

    /** 消散前还能存在多久，单位 tick；负数表示永久。 */
    public int lifetime()
    {
        return this.lifetime;
    }

    /** 不会因为时间走掉。 */
    public boolean permanent()
    {
        return this.lifetime < 0;
    }

    /** 过一 tick。
     *
     * @return 减完之后的剩余 tick，{@code 0} 表示该走了；永久的一直是负数
     */
    public int tick()
    {
        if (this.permanent())
        {
            return this.lifetime;
        }

        // 停在 0，别减成负数 —— 负数在这个类里是"永久"的意思。
        this.lifetime = Math.max(0, this.lifetime - 1);
        return this.lifetime;
    }
}
