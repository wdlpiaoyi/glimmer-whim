package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.whims.WhimContent;
import com.wdlpiaoyi.glimmerwhim.whims.dev.template.WhimTemplate;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = GlimmerWhim.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DevWhims
{
    private DevWhims()
    {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event)
    {
        // 开发内容：源码存在 whims/dev/ 即注册为可用灵感
        DevTraces.register();
        WhimContent.register(DevStrikeWhim.INSTANCE, DevStrikeChargeWhim.INSTANCE, DevEntityWhim.INSTANCE,
                WhimTemplate.INSTANCE);
    }
}
