package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.client.WhimRenders;
import com.wdlpiaoyi.glimmerwhim.whims.dev.DevStrikeChargeWhim;
import com.wdlpiaoyi.glimmerwhim.whims.dev.DevStrikeWhim;
import com.wdlpiaoyi.glimmerwhim.whims.dev.DevTraces;

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
        // strike 只注册瞄准高亮；charge 只注册本体绘制；电流样式注册到具名表
        WhimRenders.register(DevStrikeWhim.INSTANCE,
                new WhimRenderer.RenderSpec().highlight(DevStrikeRender::highlight));
        WhimRenders.register(DevStrikeChargeWhim.INSTANCE,
                new WhimRenderer.RenderSpec().draw(DevStrikeChargeRender::draw));
        WhimRenders.register(DevTraces.CURRENT, DevStrikeTrace::draw);
    }
}
