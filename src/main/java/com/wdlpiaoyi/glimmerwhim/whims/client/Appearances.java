package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.LinkedHashMap;
import java.util.Map;

import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.engine.WhimShapes;

import net.minecraft.resources.ResourceLocation;

// 具名外观的实现表：{shape} 里写的 id 到这里取画法
public final class Appearances
{
    private static final Map<ResourceLocation, WhimRenderer.Drawer> DRAWERS = new LinkedHashMap<>();

    static
    {
        register(WhimShapes.QUAD, DefaultRender::quad);
        register(WhimShapes.CUBE, DefaultRender::cube);
    }

    private Appearances()
    {
    }

    public static void register(ResourceLocation shape, WhimRenderer.Drawer drawer)
    {
        DRAWERS.put(shape, drawer);
    }

    public static WhimRenderer.Drawer get(ResourceLocation shape)
    {
        return DRAWERS.get(shape);
    }
}
