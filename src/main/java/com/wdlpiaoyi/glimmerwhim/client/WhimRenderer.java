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
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.client.DevRender;
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

        default float fadeMillis()
        {
            return 300.0F;
        }
    }

    private record Drawable(ClientWhimCache.WhimView whim, WhimParams params, Vec3 at)
    {
    }

    private static final Map<ResourceLocation, Drawer> DRAWERS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Hit> HITS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Highlight> HIGHLIGHTS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Trace> ELEMENT_TRACES = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Trace> MODIFIER_TRACES = new LinkedHashMap<>();

    private WhimRenderer()
    {
    }

    public static void register(WhimType type, Drawer drawer, Hit hit, Highlight highlight, Trace elementTrace,
            Trace modifierTrace)
    {
        DRAWERS.put(type.id(), drawer);
        HITS.put(type.id(), hit);
        HIGHLIGHTS.put(type.id(), highlight);

        if (elementTrace != null)
        {
            ELEMENT_TRACES.put(type.id(), elementTrace);
        }

        if (modifierTrace != null)
        {
            MODIFIER_TRACES.put(type.id(), modifierTrace);
        }
    }

    public static Drawer drawer(ResourceLocation id)
    {
        return DRAWERS.getOrDefault(id, DevRender::draw);
    }

    public static Hit hit(ResourceLocation id)
    {
        return HITS.getOrDefault(id, DevRender::hit);
    }

    public static Highlight highlight(ResourceLocation id)
    {
        return HIGHLIGHTS.getOrDefault(id, DevRender::outline);
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
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

            drawable.add(new Drawable(whim, whim.type().params(), at));
        }

        drawable.sort(Comparator.comparingDouble(entry -> -entry.at().distanceToSqr(eye)));

        for (Drawable entry : drawable)
        {
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

        WhimTrace.render(pose, level, eye, look, camera, partialTick);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
