package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.whim.Whim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 元素类型：被瞄上时长什么样。 */
public final class WhimTypes
{
    /** 画一条高亮。pose 已经挪到那条灵感的位置。 */
    @FunctionalInterface
    public interface Highlight
    {
        void draw(PoseStack pose, Vec3 dir);
    }

    private static final Map<ResourceLocation, Highlight> HIGHLIGHTS = new LinkedHashMap<>();

    static
    {
        register(Whim.DEV_ELEMENT, WhimRenderer::whiteOutline);
    }

    private WhimTypes()
    {
    }

    public static void register(ResourceLocation element, Highlight highlight)
    {
        HIGHLIGHTS.put(element, highlight);
    }

    public static Set<ResourceLocation> elements()
    {
        return Collections.unmodifiableSet(HIGHLIGHTS.keySet());
    }

    /** 没登记过的元素先按白描边画。 */
    public static Highlight highlight(ResourceLocation element)
    {
        return HIGHLIGHTS.getOrDefault(element, WhimRenderer::whiteOutline);
    }
}
