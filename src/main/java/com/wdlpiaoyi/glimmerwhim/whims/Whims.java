package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.resources.ResourceLocation;

public final class Whims
{
    // id -> 类型；保持注册顺序
    private static final Map<ResourceLocation, WhimType> TYPES = new LinkedHashMap<>();

    private Whims()
    {
    }

    // 同 id 覆盖
    public static void register(WhimType type)
    {
        // 直调入口绕过了 WhimContent 的禁用闸口：只提醒，不拦截，否则会留下半成品（进了注册表却没绑事件）
        if (!WhimConfig.whimEnabled(type.id()))
        {
            GlimmerWhim.LOGGER.warn("灵感 {} 已被配置禁用，但仍被直接注册进注册表（受控入口为 WhimContent.register）", type.id());
        }

        TYPES.put(type.id(), type);
    }

    public static WhimType get(ResourceLocation id)
    {
        return TYPES.get(id);
    }

    public static boolean contains(ResourceLocation id)
    {
        return TYPES.containsKey(id);
    }

    public static Collection<ResourceLocation> ids()
    {
        return TYPES.keySet();
    }

    public static Collection<WhimType> types()
    {
        return TYPES.values();
    }

}
