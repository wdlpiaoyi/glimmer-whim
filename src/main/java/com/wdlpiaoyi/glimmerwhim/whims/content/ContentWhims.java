package com.wdlpiaoyi.glimmerwhim.whims.content;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.whims.WhimContent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

// 正式内容的注册入口（两端都加载）
@Mod.EventBusSubscriber(modid = GlimmerWhim.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ContentWhims
{
    private ContentWhims()
    {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        ContentTraces.register();
        WhimContent.register(StrikeWhim.INSTANCE);
    }
}
