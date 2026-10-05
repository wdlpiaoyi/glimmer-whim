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
import com.wdlpiaoyi.glimmerwhim.whims.client.Tint;
import com.wdlpiaoyi.glimmerwhim.whims.client.Trace;
import com.wdlpiaoyi.glimmerwhim.whims.client.Traces;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhimTrace
{
    // IDLE 无轨迹；PLAY 从起点飞向目标；FADE 淡出
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

    public static void clear()
    {
        phase = Phase.IDLE;
        frozen = List.of();
        live = List.of();
        origin = null;
        target = null;
        playMillis = 0.0F;
        fadeMillis = 0.0F;
        phaseStart = 0L;
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
        // 根节点的 PLAYTIME 单位是 tick
        double play = root.params().number(root.data(), Whim.PLAYTIME, 0.0D);

        frozen = nodes;
        origin = eye;
        WhimTrace.target = target == null ? null : target.point();
        // PLAYTIME 单位是 tick，×50 换成毫秒（20tps）
        playMillis = (float) Math.max(0.0D, play) * 50.0F;
        fadeMillis = WhimRenderer.elementTrace(root.params(), root.data()).fadeMillis();
        phase = playMillis > 0.0F && WhimTrace.target != null ? Phase.PLAY : Phase.FADE;
        phaseStart = System.currentTimeMillis();
    }

    // 链被中断时，把当前实时轨迹转为淡出
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
        fadeMillis = WhimRenderer.elementTrace(frozen.get(0).params(), frozen.get(0).data()).fadeMillis();
        phase = Phase.FADE;
        phaseStart = System.currentTimeMillis();
    }

    // 按住期间画实时链；否则播放已冻结的轨迹
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

            nodes.add(new Node(at, view.type().id(), view.data(), view.type().effectiveParams()));
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
        List<Stroke> strokes = strokes(nodes);
        Trace element = WhimRenderer.elementTrace(root.params(), root.data());
        int count = nodes.size();

        // 元素基础线按染色变化切段，每段套用当时的染色
        int start = 0;
        boolean merged = false;

        while (start <= count - 2)
        {
            Stroke stroke = strokes.get(start);
            int stop = start;

            while (stop + 1 <= count - 2 && strokes.get(stop + 1).equals(stroke))
            {
                stop++;
            }

            // 末段与末节点染色一致时才把终点并进来，否则终点属于末节点的染色
            boolean toEnd = stop == count - 2 && strokes.get(count - 1).equals(stroke);
            merged = toEnd;
            apply(stroke, root);
            element.draw(pose, new ArrayList<>(points.subList(start, stop + 2)), toEnd ? end : null, fade,
                    root.data(), root.params());
            Traces.clearTint();
            start = stop + 1;
        }

        if (!merged)
        {
            // 末节点到终点这一段用末节点的染色
            apply(strokes.get(count - 1), nodes.get(count - 1));
            element.draw(pose, new ArrayList<>(points.subList(count - 1, count)), end, fade, root.data(),
                    root.params());
            Traces.clearTint();
        }

        // 每个 modifier 从它自己出发画到终点，即只影响它之后的段；后面的 modifier 会叠在同一段上
        for (int i = 1; i < count; i++)
        {
            Node node = nodes.get(i);
            Trace overlay = WhimRenderer.modifierTrace(node.params(), node.data());

            if (overlay == null)
            {
                continue;
            }

            apply(strokes.get(i), node);
            overlay.draw(pose, new ArrayList<>(points.subList(i, count)), end, fade, node.data(), node.params());
            Traces.clearTint();
        }
    }

    // 每个节点处的染色：元素样式打底，之后的 modifier 合成样式从它自己往后覆盖
    // 同一种合成样式连续叠加时累加层数，交给样式自己决定叠加效果
    private static List<Stroke> strokes(List<Node> nodes)
    {
        List<Stroke> strokes = new ArrayList<>(nodes.size());
        Tint running = WhimRenderer.elementTint(nodes.get(0).params(), nodes.get(0).data());
        int stack = running == null ? 0 : 1;

        for (int i = 0; i < nodes.size(); i++)
        {
            if (i > 0)
            {
                Tint own = WhimRenderer.modifierTint(nodes.get(i).params(), nodes.get(i).data());

                if (own != null)
                {
                    stack = own == running ? stack + 1 : 1;
                    running = own;
                }
            }

            strokes.add(new Stroke(running, stack));
        }

        return strokes;
    }

    private static void apply(Stroke stroke, Node node)
    {
        Traces.setTint(stroke.tint() == null ? null : stroke.tint().color(node.data(), node.params(), stroke.stack()));
    }

    // tint 为 null 表示不染色；stack 是同一种合成样式叠了几层
    private record Stroke(Tint tint, int stack)
    {
    }

    private static Vec3 crosshair(ClientLevel level, Vec3 eye, Vec3 look)
    {
        // 实时链终点取视线与方块的最近交点
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
