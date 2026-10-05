package com.wdlpiaoyi.glimmerwhim.whims.dev.template.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
import com.wdlpiaoyi.glimmerwhim.whims.client.Glow;
import com.wdlpiaoyi.glimmerwhim.whims.client.Traces;
import com.wdlpiaoyi.glimmerwhim.whims.client.WhimRenders;
import com.wdlpiaoyi.glimmerwhim.whims.dev.template.WhimTemplate;

import net.minecraft.world.phys.Vec3;

// 灵感模板的客户端一半：绘制、命中、瞄准高亮与轨迹样式。
// 渲染类只能在客户端加载，登记必须来自 Dist.CLIENT 的类（这里是 DevRenders 的 onClientSetup）。
// 只做服务端内容的正式灵感，对应绘制可以没有；没有绘制器的元素用 [render.default] 外观。
public final class WhimTemplateRender
{
    // 轨迹固定颜色（RGB）与不透明度由 fade 决定
    private static final float RED = 0.40F;
    private static final float GREEN = 0.90F;
    private static final float BLUE = 1.00F;

    private static final float[] FLASH_COLOR = { 1.0F, 1.0F, 1.0F, 1.0F };

    private WhimTemplateRender()
    {
    }

    // 本体绘制；委托默认外观，要自定义就照 DefaultRender 的顶点流程画。id 是本条灵感的 UUID，可用于按单个灵感维护表现状态。
    // 光效（辉光盘、光环、光片）用 Glow 的图元；尺寸走 Glow.coreRadius，它会跟 {size} 与到相机的距离挂钩，屏幕上近似恒定
    public static void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        DefaultRender.draw(pose, dir, data, params);
    }

    // 准星命中测试：返回命中距离，未命中返回 -1。
    // 想更好瞄就把几何体积放大（DefaultRender.hit 的 scale 重载），别用视锥夹角
    public static double hit(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        return DefaultRender.hit(eye, look, at, data, params);
    }

    // 被瞄准时的高亮；委托默认外扩轮廓
    public static void highlight(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        DefaultRender.outline(pose, dir, data, params);
    }

    // 轨迹样式示例：把折线连同终点一次画成固定颜色的带子，线宽取配置
    public static void trace(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        List<Vec3> all = new ArrayList<>(points.size() + 1);
        all.addAll(points);

        if (endpoint != null)
        {
            all.add(endpoint);
        }

        Traces.polyline(pose, all, RED, GREEN, BLUE, 1.0F - fade, (float) WhimConfig.traceWidth());
    }

    // 消散样式骨架「闪烁消散」：本体闪几下后淡出。要复用的样式应单独放到 whims/client 并按具体形态命名（参考 ScatterVanish）
    public static void vanish(PoseStack pose, Vec3 dir, float progress, WhimData data, WhimParams params, UUID id)
    {
        double core = Glow.coreRadius(pose, params, data);
        double flash = 1.0D - progress;
        float brightness = (float) (flash * Math.abs(Math.sin(progress * Math.PI * 6.0D)));

        Glow.additive();
        Glow.disc(pose, dir, core * (1.0D + 0.5D * flash), FLASH_COLOR, brightness);
        Glow.restore();
    }

    // 客户端登记入口：灵感的绘制/命中/高亮/消散，以及本类型的轨迹样式 id
    public static void register()
    {
        WhimRenders.register(WhimTemplate.INSTANCE, new WhimRenderer.RenderSpec().draw(WhimTemplateRender::draw)
                .hit(WhimTemplateRender::hit).highlight(WhimTemplateRender::highlight)
                .vanish(WhimTemplateRender::vanish));
        WhimRenders.register(WhimTemplate.TRACE, WhimTemplateRender::trace);
    }
}
