package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSight;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
import com.wdlpiaoyi.glimmerwhim.whims.client.Traces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WhimRenderer
{
    @FunctionalInterface
    public interface Drawer
    {
        void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params);
    }

    @FunctionalInterface
    public interface Hit
    {
        double test(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params);
    }

    @FunctionalInterface
    public interface Highlight
    {
        void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params);
    }

    @FunctionalInterface
    public interface Trace
    {
        void draw(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data, WhimParams params);

        // 默认淡出时长，单位毫秒
        default float fadeMillis()
        {
            return 300.0F;
        }
    }

    public static final class RenderSpec
    {
        private Drawer draw = DefaultRender::draw;

        private Hit hit = DefaultRender::hit;

        private Highlight highlight = DefaultRender::outline;

        private Trace elementTrace;

        private Trace modifierTrace;

        public RenderSpec draw(Drawer draw)
        {
            this.draw = draw;
            return this;
        }

        public RenderSpec hit(Hit hit)
        {
            this.hit = hit;
            return this;
        }

        public RenderSpec highlight(Highlight highlight)
        {
            this.highlight = highlight;
            return this;
        }

        public RenderSpec elementTrace(Trace elementTrace)
        {
            this.elementTrace = elementTrace;
            return this;
        }

        public RenderSpec modifierTrace(Trace modifierTrace)
        {
            this.modifierTrace = modifierTrace;
            return this;
        }
    }

    private record Drawable(ClientWhimCache.WhimView whim, WhimParams params, Vec3 at, boolean depth)
    {
    }

    // 注册表均按元素 id 索引；除 modifierTrace 外未注册都会回落到默认实现
    private static final Map<ResourceLocation, Drawer> DRAWERS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Hit> HITS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Highlight> HIGHLIGHTS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Trace> ELEMENT_TRACES = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Trace> MODIFIER_TRACES = new LinkedHashMap<>();

    private WhimRenderer()
    {
    }

    public static void register(WhimType type, RenderSpec spec)
    {
        DRAWERS.put(type.id(), spec.draw);
        HITS.put(type.id(), spec.hit);
        HIGHLIGHTS.put(type.id(), spec.highlight);

        if (spec.elementTrace != null)
        {
            ELEMENT_TRACES.put(type.id(), spec.elementTrace);
        }

        if (spec.modifierTrace != null)
        {
            MODIFIER_TRACES.put(type.id(), spec.modifierTrace);
        }
    }

    public static Drawer drawer(ResourceLocation id)
    {
        return DRAWERS.getOrDefault(id, DefaultRender::draw);
    }

    public static Hit hit(ResourceLocation id)
    {
        return HITS.getOrDefault(id, DefaultRender::hit);
    }

    public static Highlight highlight(ResourceLocation id)
    {
        return HIGHLIGHTS.getOrDefault(id, DefaultRender::outline);
    }

    public static Trace elementTrace(ResourceLocation id)
    {
        return ELEMENT_TRACES.getOrDefault(id, Traces::line);
    }

    public static Trace modifierTrace(ResourceLocation id)
    {
        return MODIFIER_TRACES.get(id);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        // 选在粒子之后：深度已就绪，可做实体遮挡判定
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null
                || ClientWhimCache.all().isEmpty() && !WhimTrace.active())
        {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        Vec3 look = FreeLook.viewVector(minecraft.player, partialTick);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        UUID aimed = WhimAim.update(level, eye, look, partialTick);

        // 半透明叠加：开混合、关剔除、关深度写入
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        List<Drawable> drawable = new ArrayList<>();
        double reach = WhimReach.blocks();

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            WhimAnchor anchor = whim.anchor();
            Vec3 at = anchor.position(level, eye, partialTick).orElse(null);

            if (at == null || at.subtract(eye).lengthSqr() < 1.0E-8D)
            {
                continue;
            }

            if (at.distanceToSqr(eye) > reach * reach)
            {
                continue;
            }

            // 声明被实体遮挡的类型开深度测试，使其能被实体挡住
            boolean depth = whim.type().occludedByEntities(whim.data());

            if (!depth && whim.type().requiresLineOfSight()
                    && WhimSight.occluded(level, eye, at, Minecraft.getInstance().player,
                            whim.type().occludedByBlocks(whim.data()), false, false))
            {
                continue;
            }

            drawable.add(new Drawable(whim, whim.type().params(), at, depth));
        }

        // 远到近排序，保证半透明叠加顺序
        drawable.sort(Comparator.comparingDouble(entry -> -entry.at().distanceToSqr(eye)));

        for (Drawable entry : drawable)
        {
            if (entry.depth())
            {
                RenderSystem.enableDepthTest();
            }
            else
            {
                RenderSystem.disableDepthTest();
            }

            ClientWhimCache.WhimView whim = entry.whim();
            Vec3 dir = entry.at().subtract(eye).normalize();
            Vec3 position = entry.at().subtract(camera);

            pose.pushPose();
            pose.translate(position.x, position.y, position.z);

            if (whim.id().equals(aimed))
            {
                highlight(whim.type().id()).draw(pose, dir, whim.data(), entry.params());
            }
            else
            {
                drawer(whim.type().id()).draw(pose, dir, whim.data(), entry.params());
            }

            pose.popPose();
        }

        RenderSystem.disableDepthTest();
        WhimTrace.render(pose, level, eye, look, camera, partialTick);

        // 恢复渲染状态，避免影响后续阶段
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
