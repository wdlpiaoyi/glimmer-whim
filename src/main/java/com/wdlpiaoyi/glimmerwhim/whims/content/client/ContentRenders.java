package com.wdlpiaoyi.glimmerwhim.whims.content.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.client.ScatterVanish;
import com.wdlpiaoyi.glimmerwhim.whims.client.WhimRenders;
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
        // 本体同时声明绘制（含蓄力自转）、瞄准高亮、角度命中判定与消散；电流样式注册到具名表
        WhimRenders.register(StrikeWhim.INSTANCE,
                new WhimRenderer.RenderSpec().draw(StrikeRender::draw).hit(StrikeRender::hit)
                        .highlight(StrikeRender::highlight).vanish(ScatterVanish::draw));
        WhimRenders.register(ContentTraces.CURRENT, CurrentTrace::draw);
    }
}
