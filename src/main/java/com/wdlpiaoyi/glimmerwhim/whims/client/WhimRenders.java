package com.wdlpiaoyi.glimmerwhim.whims.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

// 客户端内容登记入口，与 common 侧的 WhimContent 对称：那里登记灵感本体，这里登记它的绘制、外观、命中体积与轨迹样式
@Mod.EventBusSubscriber(modid = GlimmerWhim.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WhimRenders
{
    private WhimRenders()
    {
    }

    // 一个灵感的瞄准高亮与消散表现
    public static void register(WhimType type, WhimRenderer.RenderSpec spec)
    {
        WhimRenderer.register(type, spec);
    }

    // 具名外观的画法；id 须先在 common 侧 WhimShapes 登记
    public static void shape(ResourceLocation shape, WhimRenderer.Drawer impl)
    {
        Appearances.register(shape, impl);
    }

    // 具名命中体积的判定；id 须先在 common 侧 WhimHits 登记
    public static void hitVolume(ResourceLocation hit, WhimRenderer.Hit impl)
    {
        HitVolumes.register(hit, impl);
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
}
