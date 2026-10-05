package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimHits;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimShapes;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSight;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTraces;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.client.Appearances;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
import com.wdlpiaoyi.glimmerwhim.whims.client.HitVolumes;
import com.wdlpiaoyi.glimmerwhim.whims.client.Tint;
import com.wdlpiaoyi.glimmerwhim.whims.client.Trace;
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
    // id 供需要按单个灵感维护状态的表现使用（例如本地倒计时）
    @FunctionalInterface
    public interface Drawer
    {
        void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id);
    }

    @FunctionalInterface
    public interface Hit
    {
        double test(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params);
    }

    @FunctionalInterface
    public interface Highlight
    {
        void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id);
    }

    // 消散表现：进度 0..1；时长由 WhimType.vanishMillis 声明
    @FunctionalInterface
    public interface Vanish
    {
        void draw(PoseStack pose, Vec3 dir, float progress, WhimData data, WhimParams params, UUID id);
    }

    // 本体画法与命中体积不走这里：它们由 data 里的 {shape}/{hit} 决定，见 Appearances/HitVolumes
    public static final class RenderSpec
    {
        private Highlight highlight = DefaultRender::outline;

        private Vanish vanish;

        public RenderSpec highlight(Highlight highlight)
        {
            this.highlight = highlight;
            return this;
        }

        public RenderSpec vanish(Vanish vanish)
        {
            this.vanish = vanish;
            return this;
        }
    }

    private record Drawable(ClientWhimCache.WhimView whim, WhimParams params, Vec3 at, boolean depth)
    {
    }

    // 按元素 id 索引；未注册都会回落到默认实现。外观、命中体积与轨迹样式不走这里，由 data 里的 id 决定
    private static final Map<ResourceLocation, Highlight> HIGHLIGHTS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Vanish> VANISHES = new LinkedHashMap<>();

    private WhimRenderer()
    {
    }

    public static void register(WhimType type, RenderSpec spec)
    {
        HIGHLIGHTS.put(type.id(), spec.highlight);
        VANISHES.put(type.id(), spec.vanish);
    }

    // 本体的画法：由 {shape} 决定；未登记/拼错的 id 回落到默认外观并打日志
    public static Drawer appearance(WhimParams params, WhimData data)
    {
        String raw = params.text(data, Whim.SHAPE);
        ResourceLocation id = WhimShapes.resolve(raw).orElse(null);
        Drawer drawer = id == null ? null : Appearances.get(id);

        if (drawer != null)
        {
            return drawer;
        }

        if (raw != null && !raw.isBlank() && UNKNOWN_SHAPES.add(raw))
        {
            GlimmerWhim.log("未知的外观: " + raw + "，回落到 " + WhimShapes.QUAD);
        }

        return Appearances.get(WhimShapes.QUAD);
    }

    // 本体的命中体积：由 {hit} 决定；未登记/拼错的 id 回落到默认命中体积并打日志
    public static Hit hitVolume(WhimParams params, WhimData data)
    {
        String raw = params.text(data, Whim.HIT);
        ResourceLocation id = WhimHits.resolve(raw).orElse(null);
        Hit hit = id == null ? null : HitVolumes.get(id);

        if (hit != null)
        {
            return hit;
        }

        if (raw != null && !raw.isBlank() && UNKNOWN_HITS.add(raw))
        {
            GlimmerWhim.log("未知的命中体积: " + raw + "，回落到 " + WhimHits.QUAD);
        }

        return HitVolumes.get(WhimHits.QUAD);
    }

    public static Highlight highlight(ResourceLocation id)
    {
        return HIGHLIGHTS.getOrDefault(id, DefaultRender::outline);
    }

    // 消散样式没有默认实现：没登记就不演消散
    public static Vanish vanish(ResourceLocation id)
    {
        return VANISHES.get(id);
    }

    // 链的轨迹样式：data 覆盖、类型默认兜底；未登记/拼错的 id 回落到 fallback 并打日志
    public static Trace elementTrace(WhimParams params, WhimData data)
    {
        String raw = params.text(data, Whim.ELEMENT_TRACE);

        // 元素位置声明成合成样式时，仍需一条基础线供它染色
        return isTint(raw) ? Traces::line : resolve(raw, WhimTraces.LINE);
    }

    public static Trace modifierTrace(WhimParams params, WhimData data)
    {
        String raw = params.text(data, Whim.MODIFIER_TRACE);

        // 合成样式不自己画
        return isTint(raw) ? Traces.NONE : resolve(raw, WhimTraces.NONE);
    }

    public static Tint elementTint(WhimParams params, WhimData data)
    {
        return tint(params.text(data, Whim.ELEMENT_TRACE));
    }

    public static Tint modifierTint(WhimParams params, WhimData data)
    {
        return tint(params.text(data, Whim.MODIFIER_TRACE));
    }

    private static Tint tint(String raw)
    {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id == null ? null : Traces.tint(id);
    }

    private static boolean isTint(String raw)
    {
        return tint(raw) != null;
    }

    // 已就未登记样式打过日志的原文，避免每帧重复打
    private static final Set<String> UNKNOWN_TRACES = new HashSet<>();

    private static final Set<String> UNKNOWN_SHAPES = new HashSet<>();

    private static final Set<String> UNKNOWN_HITS = new HashSet<>();

    private static Trace resolve(String raw, ResourceLocation fallback)
    {
        ResourceLocation id = WhimTraces.resolve(raw).orElse(null);
        Trace trace = id == null ? null : Traces.get(id);

        if (trace != null)
        {
            return trace;
        }

        // 写的本来就是 fallback（含省略命名空间的写法）时不刷日志
        boolean fallbackText = raw != null && (raw.equals(fallback.toString()) || raw.equals(fallback.getPath()));

        if (id == null && !fallbackText && raw != null && !raw.isBlank() && UNKNOWN_TRACES.add(raw))
        {
            GlimmerWhim.log("未知的轨迹样式: " + raw + "，回落到 " + fallback);
        }

        return Traces.get(fallback);
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
                || ClientWhimCache.all().isEmpty() && !WhimTrace.active() && !WhimVanish.active())
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

            // 声明深度遮挡的类型开深度测试，使方块与实体都能挡住它
            boolean depth = whim.type().depthOcclusion(whim.data());

            if (!depth && whim.type().requiresLineOfSight()
                    && WhimSight.occluded(level, eye, at, Minecraft.getInstance().player,
                            whim.type().occludedByBlocks(whim.data()), null, false))
            {
                continue;
            }

            drawable.add(new Drawable(whim, whim.type().effectiveParams(), at, depth));
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
                highlight(whim.type().id()).draw(pose, dir, whim.data(), entry.params(), whim.id());
            }
            else
            {
                appearance(entry.params(), whim.data()).draw(pose, dir, whim.data(), entry.params(), whim.id());
            }

            pose.popPose();
        }

        RenderSystem.disableDepthTest();
        WhimVanish.render(pose, eye, camera);
        WhimTrace.render(pose, level, eye, look, camera, partialTick);

        // 恢复渲染状态，避免影响后续阶段
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
