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
        register(WhimTraces.NONE, NONE);
        TINTS.put(WhimTraces.HUE, Traces::hueColor);
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
        // 色相按 5 秒周期循环，基色取 dev 元素主色；叠加时周期按层数缩短，即循环加快
        long period = 5000L / Math.max(1, stack);
        float shift = (float) ((System.currentTimeMillis() % period) / (double) period);
        return DefaultRender.rotateHue(WhimConfig.renderDevColorElement(), shift);
    }

    public static void glow(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        // 双倍线宽半透明，叠出辉光
        stroke(pose, points, endpoint, GLOW_COLOR[0], GLOW_COLOR[1], GLOW_COLOR[2], (1.0F - fade) * GLOW_COLOR[3],
                (float) WhimConfig.traceWidth() * 2.0F);
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
