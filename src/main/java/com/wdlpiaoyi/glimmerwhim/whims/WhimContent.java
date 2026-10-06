package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.LinkedHashSet;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;

public final class WhimContent
{
    // 曾被提供过的内容（含因配置禁用而未注册的），用于启动时校验 id 是否真实存在
    private static final Set<ResourceLocation> OFFERED = new LinkedHashSet<>();

    private WhimContent()
    {
    }

    // 批量注册；引擎代为挂上 Forge 事件（实例与静态处理器），须在使用前调用
    public static void register(WhimType... types)
    {
        for (WhimType type : types)
        {
            OFFERED.add(type.id());

            // 被配置禁用的内容不注册、不绑事件，等同于不存在
            if (!WhimConfig.whimEnabled(type.id()))
            {
                continue;
            }

            Whims.register(type);
            type.bind();
            MinecraftForge.EVENT_BUS.register(type);
            MinecraftForge.EVENT_BUS.register(type.getClass());
        }
    }

    public static void unregister(WhimType... types)
    {
        for (WhimType type : types)
        {
            MinecraftForge.EVENT_BUS.unregister(type);
            MinecraftForge.EVENT_BUS.unregister(type.getClass());
        }
    }

    // 该 id 是否是真实存在的内容：被提供过，或已由直调入口注册进注册表
    public static boolean known(ResourceLocation id)
    {
        return OFFERED.contains(id) || Whims.contains(id);
    }
}
