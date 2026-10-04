package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
import com.wdlpiaoyi.glimmerwhim.whims.dev.DevStrikeChargeWhim;
import com.wdlpiaoyi.glimmerwhim.whims.dev.DevStrikeWhim;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GlimmerWhim.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DevRenders
{
    private DevRenders()
    {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        WhimRenderer.register(DevStrikeWhim.INSTANCE, DefaultRender::draw, DefaultRender::hit, DevStrikeRender::highlight, null,
                null);
        WhimRenderer.register(DevStrikeChargeWhim.INSTANCE, DevStrikeChargeRender::draw, DefaultRender::hit,
                DefaultRender::outline, null, null);
    }
}
