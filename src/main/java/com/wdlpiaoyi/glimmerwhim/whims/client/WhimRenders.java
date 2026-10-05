package com.wdlpiaoyi.glimmerwhim.whims.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

// 客户端内容登记入口，与 common 侧的 WhimContent 对称：那里登记灵感本体，这里登记它的绘制与轨迹样式
@Mod.EventBusSubscriber(modid = GlimmerWhim.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WhimRenders
{
    private WhimRenders()
    {
    }

    // 一个灵感的绘制、命中与瞄准高亮
    public static void register(WhimType type, WhimRenderer.RenderSpec spec)
    {
        WhimRenderer.register(type, spec);
    }

    // 轨迹样式的实现；合成样式（如 hue）不画几何，改用 tint
    public static void register(ResourceLocation trace, Trace impl)
    {
        Traces.register(trace, impl);
    }

    public static void tint(ResourceLocation trace, Tint impl)
    {
        Traces.tint(trace, impl);
    }

    // 加载完成后点名仍用默认外观的元素，便于发现漏登记
    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event)
    {
        for (WhimType type : Whims.types())
        {
            if (!WhimRenderer.custom(type.id()))
            {
                GlimmerWhim.log("元素 " + type.id() + " 未登记绘制，使用默认外观");
            }
        }
    }
}
