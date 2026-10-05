package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.client.Traces;

import net.minecraft.world.phys.Vec3;

// 电流：每段细分后沿垂向抖动，抖动符号按帧种子闪烁
public final class DevStrikeTrace
{
    // 细分步长与抖幅（格）
    private static final double STEP = 0.5D;
    private static final double JITTER = 0.18D;
    // 远处每段的屏幕步长下限（像素），低于它就把步长与抖幅一起放大，形状随距离保持一致
    private static final double MIN_STEP_PIXELS = 24.0D;
    private static final double JITTER_RATIO = JITTER / STEP;
    // 闪烁周期（毫秒）
    private static final long FLICKER_MILLIS = 45L;
    private static final float RED = 0.70F;
    private static final float GREEN = 0.95F;
    private static final float BLUE = 1.0F;

    private DevStrikeTrace()
    {
    }

    public static void draw(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        List<Vec3> all = new ArrayList<>(points.size() + 1);
        all.addAll(points);

        if (endpoint != null)
        {
            all.add(endpoint);
        }

        if (all.size() < 2)
        {
            return;
        }

        long frame = System.currentTimeMillis() / FLICKER_MILLIS;
        // 以本段折线的点数为盐：整链与各段尾巴的长度不同，抖动互不相同，叠在同一段上也能分辨
        int salt = points.size();
        List<Vec3> jittered = new ArrayList<>();
        jittered.add(all.get(0));

        for (int i = 0; i + 1 < all.size(); i++)
        {
            Vec3 from = all.get(i);
            Vec3 to = all.get(i + 1);
            Vec3 delta = to.subtract(from);
            Vec3 middle = from.add(delta.scale(0.5D));
            // 近处保持 0.5 格步长；远处按屏幕尺寸放大步长与抖幅
            double step = Math.max(STEP, Traces.pixelsToWorld(MIN_STEP_PIXELS, middle));
            int steps = Math.max(1, (int) Math.round(delta.length() / step));
            Vec3 side = steps > 1 ? perpendicular(delta) : Vec3.ZERO;
            double jitter = step * JITTER_RATIO;

            for (int s = 1; s < steps; s++)
            {
                double offset = noise(frame, salt, i, s) * jitter;
                jittered.add(from.add(delta.scale((double) s / steps)).add(side.scale(offset)));
            }

            // 段末保留原始节点/终点
            jittered.add(to);
        }

        Traces.polyline(pose, jittered, RED, GREEN, BLUE, 1.0F - fade, (float) WhimConfig.traceWidth());
    }

    // 与线段垂直的单位向量；线段接近竖直时换参考轴
    private static Vec3 perpendicular(Vec3 delta)
    {
        Vec3 direction = delta.normalize();
        Vec3 reference = Math.abs(direction.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);

        return direction.cross(reference).normalize();
    }

    // 由帧种子、折线点数与段/点序号确定的 [-1,1] 抖动
    private static double noise(long frame, int salt, int segment, int step)
    {
        long value = frame * 0x9E3779B97F4A7C15L + salt * 0x100000001B3L + segment * 0xC2B2AE3D27D4EB4FL
                + step * 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;

        return ((value >>> 11) / (double) (1L << 53)) * 2.0D - 1.0D;
    }
}
