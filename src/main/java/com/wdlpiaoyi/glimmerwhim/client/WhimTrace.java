package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhimTrace
{
    private enum Phase
    {
        IDLE,
        PLAY,
        FADE
    }

    private record Node(Vec3 at, ResourceLocation type, WhimData data, WhimParams params)
    {
    }

    private static Phase phase = Phase.IDLE;
    private static List<Node> frozen = List.of();
    private static List<Node> live = List.of();
    private static Vec3 origin;
    private static Vec3 target;
    private static float playMillis;
    private static float fadeMillis;
    private static long phaseStart;

    private WhimTrace()
    {
    }

    public static boolean active()
    {
        if (phase != Phase.IDLE)
        {
            return true;
        }

        List<UUID> chain = WhimInteractHandler.chain();

        return chain != null && !chain.isEmpty();
    }

    public static void release(ClientLevel level, Vec3 eye, List<UUID> chain, WhimTarget target)
    {
        List<Node> nodes = resolve(level, eye, 1.0F, chain);

        if (nodes.isEmpty())
        {
            phase = Phase.IDLE;
            return;
        }

        Node root = nodes.get(0);
        double play = root.params().number(root.data(), Whim.PLAYTIME, 0.0D);

        frozen = nodes;
        origin = eye;
        WhimTrace.target = target == null ? null : target.point();
        playMillis = (float) Math.max(0.0D, play) * 50.0F;
        fadeMillis = WhimRenderer.elementTrace(root.type()).fadeMillis();
        phase = playMillis > 0.0F && WhimTrace.target != null ? Phase.PLAY : Phase.FADE;
        phaseStart = System.currentTimeMillis();
    }

    public static void dissolve()
    {
        if (live.isEmpty())
        {
            phase = Phase.IDLE;
            return;
        }

        frozen = live;
        target = null;
        playMillis = 0.0F;
        fadeMillis = WhimRenderer.elementTrace(frozen.get(0).type()).fadeMillis();
        phase = Phase.FADE;
        phaseStart = System.currentTimeMillis();
    }

    public static void render(PoseStack pose, ClientLevel level, Vec3 eye, Vec3 look, Vec3 camera, float partialTick)
    {
        if (!WhimConfig.traceEnabled())
        {
            return;
        }

        List<UUID> chain = WhimInteractHandler.chain();

        if (chain != null && !chain.isEmpty())
        {
            List<Node> nodes = resolve(level, eye, partialTick, chain);

            if (!nodes.isEmpty())
            {
                Vec3 crosshair = crosshair(level, eye, look);
                live = nodes;
                origin = eye;
                draw(pose, camera, nodes, crosshair, 0.0F);
            }

            return;
        }

        if (phase == Phase.IDLE || frozen.isEmpty())
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (phase == Phase.PLAY)
        {
            float progress = playMillis <= 0.0F ? 1.0F : Mth.clamp((now - phaseStart) / playMillis, 0.0F, 1.0F);
            draw(pose, camera, frozen, lerp(origin, target, smooth(progress)), 0.0F);

            if (progress >= 1.0F)
            {
                phase = Phase.FADE;
                phaseStart = now;
            }

            return;
        }

        float fade = fadeMillis <= 0.0F ? 1.0F : Mth.clamp((now - phaseStart) / fadeMillis, 0.0F, 1.0F);
        draw(pose, camera, frozen, target == null ? origin : target, fade);

        if (fade >= 1.0F)
        {
            phase = Phase.IDLE;
            frozen = List.of();
        }
    }

    private static List<Node> resolve(ClientLevel level, Vec3 eye, float partialTick, List<UUID> chain)
    {
        List<Node> nodes = new ArrayList<>(chain.size());

        for (UUID id : chain)
        {
            ClientWhimCache.WhimView view = ClientWhimCache.view(id);

            if (view == null)
            {
                continue;
            }

            Vec3 at = view.anchor().position(level, eye, partialTick).orElse(null);

            if (at == null)
            {
                continue;
            }

            nodes.add(new Node(at, view.type().id(), view.data(), view.type().params()));
        }

        return nodes;
    }

    private static void draw(PoseStack pose, Vec3 camera, List<Node> nodes, Vec3 endpoint, float fade)
    {
        if (nodes.isEmpty())
        {
            return;
        }

        List<Vec3> points = new ArrayList<>(nodes.size());

        for (Node node : nodes)
        {
            points.add(node.at().subtract(camera));
        }

        Vec3 end = endpoint == null ? null : endpoint.subtract(camera);
        Node root = nodes.get(0);
        WhimRenderer.elementTrace(root.type()).draw(pose, points, end, fade, root.data(), root.params());

        for (int i = 1; i < nodes.size(); i++)
        {
            Node node = nodes.get(i);
            WhimRenderer.Trace overlay = WhimRenderer.modifierTrace(node.type());

            if (overlay != null)
            {
                overlay.draw(pose, points, end, fade, node.data(), node.params());
            }
        }
    }

    private static Vec3 crosshair(ClientLevel level, Vec3 eye, Vec3 look)
    {
        Vec3 end = eye.add(look.scale(WhimReach.blocks()));
        BlockHitResult hit = level.clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float t)
    {
        if (from == null || to == null)
        {
            return from == null ? to : from;
        }

        return from.add(to.subtract(from).scale(t));
    }

    private static float smooth(float t)
    {
        return t * t * (3.0F - 2.0F * t);
    }
}
