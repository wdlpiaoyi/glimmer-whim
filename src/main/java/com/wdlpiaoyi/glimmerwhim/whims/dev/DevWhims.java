package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

import net.minecraftforge.common.MinecraftForge;
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
        Whims.register(DevStrikeWhim.INSTANCE);
        Whims.register(DevStrikeChargeWhim.INSTANCE);
        MinecraftForge.EVENT_BUS.register(DevStrikeWhim.class);
    }
}
