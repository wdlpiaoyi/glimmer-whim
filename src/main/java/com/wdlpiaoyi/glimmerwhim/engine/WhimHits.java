package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;

// 命中体积（怎么判中）的 id 注册表：服务端只需认识名字，判定实现由客户端注册
public final class WhimHits
{
    public static final ResourceLocation QUAD = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "quad");
    public static final ResourceLocation BOX = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "box");
    public static final ResourceLocation SPHERE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "sphere");

    private static final WhimIds IDS = new WhimIds();

    static
    {
        register(QUAD);
        register(BOX);
        register(SPHERE);
    }

    private WhimHits()
    {
    }

    public static void register(ResourceLocation id)
    {
        IDS.register(id);
    }

    public static List<ResourceLocation> ids()
    {
        return IDS.ids();
    }

    public static boolean contains(ResourceLocation id)
    {
        return IDS.contains(id);
    }

    public static List<String> names()
    {
        return IDS.names();
    }

    public static Optional<ResourceLocation> resolve(String raw)
    {
        return IDS.resolve(raw);
    }
}
