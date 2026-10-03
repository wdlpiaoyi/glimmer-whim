package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * 一条灵感。
 * <p>
 * 只有四个必填字段，可选字段（intensity、source、visibility）这一版一个都不写。
 * 不是实体，不落盘 —— 活几十秒的东西没有存档价值。
 */
public final class Whim
{
    /** 这一版所有灵感都用这个 element。 */
    public static final ResourceLocation DEV_ELEMENT = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev");

    private final UUID id;
    private final WhimAnchor anchor;
    private final ResourceLocation element;
    private int lifetime;

    public Whim(UUID id, WhimAnchor anchor, ResourceLocation element, int lifetime)
    {
        this.id = id;
        this.anchor = anchor;
        this.element = element;
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

    /** 消散前还能存在多久，单位 tick。 */
    public int lifetime()
    {
        return this.lifetime;
    }

    /** 过一 tick。
     *
     * @return 减完之后的剩余 tick，&lt;= 0 表示该走了
     */
    public int tick()
    {
        return --this.lifetime;
    }
}
