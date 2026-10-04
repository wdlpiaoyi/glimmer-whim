package com.wdlpiaoyi.glimmerwhim.whims;

import net.minecraftforge.common.MinecraftForge;

public final class WhimContent
{
    private WhimContent()
    {
    }

    // 批量注册；引擎代为挂上 Forge 事件（实例与静态处理器），须在使用前调用
    public static void register(WhimType... types)
    {
        for (WhimType type : types)
        {
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
}
