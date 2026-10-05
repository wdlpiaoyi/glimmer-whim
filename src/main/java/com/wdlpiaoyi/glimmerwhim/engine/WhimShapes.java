package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;

// 外观（怎么画）的 id 注册表：服务端只需认识名字（校验/补全），绘制实现由客户端注册
public final class WhimShapes
{
    public static final ResourceLocation QUAD = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "quad");
    public static final ResourceLocation CUBE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "cube");

    private static final WhimIds IDS = new WhimIds();

    static
    {
        register(QUAD);
        register(CUBE);
    }

    private WhimShapes()
    {
    }

    // 新外观先在这里登记 id，再由客户端注册同名绘制实现
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
