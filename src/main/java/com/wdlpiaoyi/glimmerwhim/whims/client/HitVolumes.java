package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.LinkedHashMap;
import java.util.Map;

import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.engine.WhimHits;

import net.minecraft.resources.ResourceLocation;

// 具名命中体积的实现表：{hit} 里写的 id 到这里取判定
public final class HitVolumes
{
    private static final Map<ResourceLocation, WhimRenderer.Hit> HITS = new LinkedHashMap<>();

    static
    {
        register(WhimHits.QUAD, DefaultRender::hitQuad);
        register(WhimHits.BOX, DefaultRender::hitBox);
        register(WhimHits.SPHERE, DefaultRender::hitSphere);
    }

    private HitVolumes()
    {
    }

    public static void register(ResourceLocation hit, WhimRenderer.Hit impl)
    {
        HITS.put(hit, impl);
    }

    public static WhimRenderer.Hit get(ResourceLocation hit)
    {
        return HITS.get(hit);
    }
}
