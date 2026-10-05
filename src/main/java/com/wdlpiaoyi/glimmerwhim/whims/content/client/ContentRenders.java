package com.wdlpiaoyi.glimmerwhim.whims.content.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.client.ScatterVanish;
import com.wdlpiaoyi.glimmerwhim.whims.client.WhimRenders;
import com.wdlpiaoyi.glimmerwhim.whims.content.ContentHits;
import com.wdlpiaoyi.glimmerwhim.whims.content.ContentShapes;
import com.wdlpiaoyi.glimmerwhim.whims.content.ContentTraces;
import com.wdlpiaoyi.glimmerwhim.whims.content.StrikeWhim;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// 正式内容的客户端注册入口
@Mod.EventBusSubscriber(modid = GlimmerWhim.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ContentRenders
{
    private ContentRenders()
    {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // 本体只声明瞄准高亮与消散；画法与命中体积是具名资产，分别登记
        WhimRenders.register(StrikeWhim.INSTANCE,
                new WhimRenderer.RenderSpec().highlight(StrikeRender::highlight).vanish(ScatterVanish::draw));
        WhimRenders.shape(ContentShapes.GLOW, StrikeRender::draw);
        WhimRenders.hitVolume(ContentHits.GLOW, StrikeRender::hit);
        WhimRenders.register(ContentTraces.CURRENT, CurrentTrace::draw);
    }
}
