package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTraces;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// 轨迹样式的具名注册表；绘制样式 line/glow/none 用 register，合成样式 hue 用 tint（不画几何，只给之后的绘制染色）
public final class Traces
{
    // 默认白；glow 为半透明白
    private static final float[] DEFAULT_COLOR = { 1.0F, 1.0F, 1.0F, 1.0F };
    private static final float[] GLOW_COLOR = { 1.0F, 1.0F, 1.0F, 0.5F };

    // 曲线：每段朝屏幕侧向鼓出，最大的横向偏移是段长的这个比例
    private static final double CURVE_BULGE = 0.25D;
    // 细分步长（格）与远处的屏幕步长下限（像素）
    private static final double CURVE_STEP = 0.5D;
    private static final double CURVE_MIN_STEP_PIXELS = 24.0D;
    // 短于它的段视为起终点重合，改画圆环
    private static final double CURVE_MIN_LENGTH = 0.05D;
    private static final double CURVE_RING_RADIUS = 0.5D;
    private static final int CURVE_RING_SEGMENTS = 24;

    // 显式声明「不画轨迹」
    public static final Trace NONE = (pose, points, endpoint, fade, data, params) -> { };

    private static final Map<ResourceLocation, Trace> STYLES = new LinkedHashMap<>();
    // 合成样式：提供染色，不自己画几何
    private static final Map<ResourceLocation, Tint> TINTS = new LinkedHashMap<>();
    // 当前生效的染色；为 null 时各样式用自己的颜色
    private static float[] tint;

    static
    {
        register(WhimTraces.LINE, Traces::line);
        register(WhimTraces.GLOW, Traces::glow);
        register(WhimTraces.CURVE, Traces::curve);
        register(WhimTraces.NONE, NONE);
        tint(WhimTraces.HUE, Traces::hueColor);
    }

    private Traces()
    {
    }

    public static void register(ResourceLocation id, Trace trace)
    {
        STYLES.put(id, trace);
    }

    public static Trace get(ResourceLocation id)
    {
        return STYLES.get(id);
    }

    public static Set<ResourceLocation> ids()
    {
        return Collections.unmodifiableSet(STYLES.keySet());
    }

    // id 是否是已登记的样式（绘制或合成）
    public static boolean known(ResourceLocation id)
    {
        return STYLES.containsKey(id) || TINTS.containsKey(id);
    }

    // 登记合成样式：它不画几何，只给之后的绘制染色
    public static void tint(ResourceLocation id, Tint impl)
    {
        TINTS.put(id, impl);
    }

    public static Tint tint(ResourceLocation id)
    {
        return TINTS.get(id);
    }

    public static void setTint(float[] color)
    {
        tint = color;
    }

    public static void clearTint()
    {
        tint = null;
    }

    public static void line(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        // alpha 随 fade 线性衰减，fade=0 时完全不透明
        float[] color = DEFAULT_COLOR;
        stroke(pose, points, endpoint, color[0], color[1], color[2], (1.0F - fade) * color[3],
                (float) WhimConfig.traceWidth());
    }

    public static float[] hueColor(WhimData data, WhimParams params, int stack)
    {
        // 色相按 5 秒周期循环，基色取白色；叠加时周期按层数缩短，即循环加快
        long period = 5000L / Math.max(1, stack);
        float shift = (float) ((System.currentTimeMillis() % period) / (double) period);
        return DefaultRender.rotateHue(DEFAULT_COLOR, shift);
    }

    public static void glow(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        // 双倍线宽半透明，叠出辉光
        stroke(pose, points, endpoint, GLOW_COLOR[0], GLOW_COLOR[1], GLOW_COLOR[2], (1.0F - fade) * GLOW_COLOR[3],
                (float) WhimConfig.traceWidth() * 2.0F);
    }

    public static void curve(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
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

        List<Vec3> path = new ArrayList<>();
        path.add(all.get(0));

        for (int i = 0; i + 1 < all.size(); i++)
        {
            Vec3 from = all.get(i);
            Vec3 to = all.get(i + 1);
            Vec3 delta = to.subtract(from);
            double length = delta.length();

            if (length < CURVE_MIN_LENGTH)
            {
                // 起终点重合：折线等于没有，改画一个正对相机的小圆环
                ring(path, from);
                path.add(to);
                continue;
            }

            Vec3 middle = from.add(delta.scale(0.5D));
            // 远处按屏幕尺寸放大步长，弯的形状随距离保持一致
            double step = Math.max(CURVE_STEP, pixelsToWorld(CURVE_MIN_STEP_PIXELS, middle));
            int steps = Math.max(2, (int) Math.round(length / step));
            Vec3 lateral = lateral(delta, middle);

            for (int s = 1; s <= steps; s++)
            {
                double t = (double) s / steps;
                // 横向偏移在段中点最大、两端归零
                path.add(from.add(delta.scale(t))
                        .add(lateral.scale(length * CURVE_BULGE * 4.0D * t * (1.0D - t))));
            }
        }

        polyline(pose, path, DEFAULT_COLOR[0], DEFAULT_COLOR[1], DEFAULT_COLOR[2], (1.0F - fade) * DEFAULT_COLOR[3],
                (float) WhimConfig.traceWidth());
    }

    // 屏幕平面内、与线段垂直的方向：从任何视角看都是横向的弯，不会被压平
    private static Vec3 lateral(Vec3 delta, Vec3 at)
    {
        Vec3 axis = at.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : at.normalize();
        Vec3 lateral = axis.cross(delta);

        if (lateral.lengthSqr() < 1.0E-6D)
        {
            // 视线与线段同向，侧向无从谈起，取任一垂直轴兜底
            Vec3 reference = Math.abs(axis.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            lateral = axis.cross(reference);
        }

        return lateral.lengthSqr() < 1.0E-8D ? Vec3.ZERO : lateral.normalize();
    }

    // 在点上追加一个正对相机的圆环（相机在 pose 原点）
    private static void ring(List<Vec3> path, Vec3 center)
    {
        Vec3 axis = center.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : center.normalize();
        Vec3 reference = Math.abs(axis.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 u = axis.cross(reference).normalize();
        Vec3 v = axis.cross(u).normalize();

        for (int i = 0; i <= CURVE_RING_SEGMENTS; i++)
        {
            double angle = Math.PI * 2.0D * i / CURVE_RING_SEGMENTS;
            path.add(center.add(u.scale(Math.cos(angle) * CURVE_RING_RADIUS))
                    .add(v.scale(Math.sin(angle) * CURVE_RING_RADIUS)));
        }
    }

    // 供自定义样式复用：points 须是完整折线（含末点）；width 是屏幕像素宽度
    public static void polyline(PoseStack pose, List<Vec3> points, float r, float g, float b, float a, float width)
    {
        if (points.size() < 2 || width <= 0.0F)
        {
            return;
        }

        // 合成样式生效时，颜色由它接管
        if (tint != null)
        {
            r = tint[0];
            g = tint[1];
            b = tint[2];
        }

        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        double perPixel = worldPerPixel();

        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < points.size(); i++)
        {
            Vec3 point = points.get(i);
            // 每个点按它到相机的距离换算横向偏移，使带子在屏幕上保持恒定像素宽度
            Vec3 offset = side(points, i).scale(point.length() * perPixel * width * 0.5D);
            Vec3 left = point.add(offset);
            Vec3 right = point.subtract(offset);

            builder.vertex(matrix, (float) left.x, (float) left.y, (float) left.z)
                    .color(r, g, b, a)
                    .endVertex();
            builder.vertex(matrix, (float) right.x, (float) right.y, (float) right.z)
                    .color(r, g, b, a)
                    .endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
    }

    // 相机距离 1 处 1 像素对应的世界长度，由投影矩阵与窗口高度得出
    private static double worldPerPixel()
    {
        float m11 = RenderSystem.getProjectionMatrix().m11();
        int height = Minecraft.getInstance().getWindow().getHeight();

        if (m11 <= 0.0F || height <= 0)
        {
            return 0.002D;
        }

        return 2.0D / ((double) m11 * height);
    }

    // 相机相对坐标 point 处，屏幕上 pixels 像素对应的世界长度
    public static double pixelsToWorld(double pixels, Vec3 point)
    {
        return pixels * worldPerPixel() * point.length();
    }

    // 折线在该点的横向：垂直于走向、且面向相机（相机在 pose 原点），使带子始终朝向观察者
    private static Vec3 side(List<Vec3> points, int index)
    {
        Vec3 previous = points.get(Math.max(index - 1, 0));
        Vec3 next = points.get(Math.min(index + 1, points.size() - 1));
        Vec3 direction = next.subtract(previous);

        if (direction.lengthSqr() < 1.0E-8D)
        {
            return Vec3.ZERO;
        }

        direction = direction.normalize();
        Vec3 at = points.get(index);
        Vec3 side = at.lengthSqr() < 1.0E-8D ? Vec3.ZERO : direction.cross(at.normalize());

        if (side.lengthSqr() < 1.0E-6D)
        {
            Vec3 reference = Math.abs(direction.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D)
                    : new Vec3(0.0D, 1.0D, 0.0D);
            side = direction.cross(reference);
        }

        return side.lengthSqr() < 1.0E-8D ? Vec3.ZERO : side.normalize();
    }

    private static void stroke(PoseStack pose, List<Vec3> points, Vec3 endpoint, float r, float g, float b, float a,
            float width)
    {
        // 把末端点补到折线尾部，交给 polyline 一次成型
        List<Vec3> all = new ArrayList<>(points.size() + 1);
        all.addAll(points);

        if (endpoint != null)
        {
            all.add(endpoint);
        }

        polyline(pose, all, r, g, b, a, width);
    }
}
