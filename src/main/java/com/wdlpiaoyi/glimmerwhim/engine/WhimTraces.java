package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;

// 轨迹样式的 id 注册表：服务端只需认识名字（校验/补全），绘制实现由客户端注册
public final class WhimTraces
{
    public static final ResourceLocation LINE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "line");
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "glow");
    public static final ResourceLocation HUE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "hue");
    public static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "none");

    private static final List<ResourceLocation> IDS = new ArrayList<>();

    static
    {
        register(LINE);
        register(GLOW);
        register(HUE);
        register(NONE);
    }

    private WhimTraces()
    {
    }

    // 新样式先在这里登记 id，再由客户端注册同名绘制实现
    public static void register(ResourceLocation id)
    {
        if (!IDS.contains(id))
        {
            IDS.add(id);
        }
    }

    public static List<ResourceLocation> ids()
    {
        return Collections.unmodifiableList(IDS);
    }

    public static boolean contains(ResourceLocation id)
    {
        return IDS.contains(id);
    }

    public static List<String> names()
    {
        return IDS.stream().map(ResourceLocation::toString).toList();
    }
}
