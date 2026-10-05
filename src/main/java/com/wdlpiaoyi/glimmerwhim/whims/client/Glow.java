package com.wdlpiaoyi.glimmerwhim.whims.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.world.phys.Vec3;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

// 光效图元：加色混合 + 纯顶点色几何，不用贴图；亮度靠顶点 alpha 衰减
// 这里也放光效几何的尺寸约定：本体基准半径与「锚越远越大」的距离补偿
public final class Glow
{
    private static final int SEGMENTS = 48;

    // 距离补偿的基准：64 格处不放大不缩小
    private static final double DISTANCE_REFERENCE = 64.0D;

    private static final double DISTANCE_SCALE_MIN = 0.5D;

    private static final double DISTANCE_SCALE_MAX = 2.0D;

    // 由中心向外的半径比例与对应 alpha，多段衰减比线性更像光晕
    private static final double[] BANDS = { 0.0D, 0.28D, 0.55D, 0.78D, 1.0D };

    private static final float[] ALPHAS = { 1.0F, 0.62F, 0.3F, 0.12F, 0.0F };

    private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);

    private static final Vec3 X_AXIS = new Vec3(1.0D, 0.0D, 0.0D);

    private Glow()
    {
    }

    // 加色混合：让几何发光而不是糊一层色；调用方画完必须 restore()
    public static void additive()
    {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    public static void restore()
    {
        RenderSystem.defaultBlendFunc();
    }

    // 面向相机的辉光圆盘；dir 是相机到本体的方向，也就是圆盘法线
    public static void disc(PoseStack pose, Vec3 dir, double radius, float[] color, double brightness)
    {
        if (radius <= 1.0E-4D)
        {
            return;
        }

        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Vec3 right = right(dir);
        Vec3 up = right.cross(dir).normalize();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < SEGMENTS; i++)
        {
            double first = Math.PI * 2.0D * i / SEGMENTS;
            double second = Math.PI * 2.0D * (i + 1) / SEGMENTS;

            // 每段按半径分带，带与带之间 alpha 递减形成径向光晕
            for (int band = 0; band < BANDS.length - 1; band++)
            {
                double near = BANDS[band] * radius;
                double far = BANDS[band + 1] * radius;
                float nearAlpha = ALPHAS[band] * (float) brightness;
                float farAlpha = ALPHAS[band + 1] * (float) brightness;

                point(builder, matrix, ringPoint(right, up, near, first), color, nearAlpha);
                point(builder, matrix, ringPoint(right, up, far, first), color, farAlpha);
                point(builder, matrix, ringPoint(right, up, far, second), color, farAlpha);
                point(builder, matrix, ringPoint(right, up, near, second), color, nearAlpha);
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    public static void ring(PoseStack pose, Vec3 normal, double radius, double width, double phase, float[] color,
            double brightness)
    {
        ring(pose, normal, radius, width, phase, SEGMENTS, color, brightness);
    }

    // 绕 normal 的圆环带；segments 给小值可当正多边形（例如 4 就是方框）
    public static void ring(PoseStack pose, Vec3 normal, double radius, double width, double phase, int segments,
            float[] color, double brightness)
    {
        if (radius <= 1.0E-4D || width <= 1.0E-4D)
        {
            return;
        }

        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Vec3 first = perpendicular(normal);
        Vec3 second = normal.cross(first).normalize();
        double half = width / 2.0D;

        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++)
        {
            double angle = phase + Math.PI * 2.0D * i / segments;
            Vec3 radial = first.scale(Math.cos(angle)).add(second.scale(Math.sin(angle)));
            point(builder, matrix, radial.scale(radius + half), color, (float) brightness);
            point(builder, matrix, radial.scale(radius - half), color, (float) brightness);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    // 面向相机的方形光片：绕视线自转，用作碎片
    public static void shard(PoseStack pose, Vec3 dir, double size, double spin, float[] color, double brightness)
    {
        if (size <= 1.0E-4D)
        {
            return;
        }

        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Vec3 right = right(dir);
        Vec3 up = right.cross(dir).normalize();
        Vec3 half = right.scale(Math.cos(spin) * size / 2.0D).add(up.scale(Math.sin(spin) * size / 2.0D));
        Vec3 other = right.scale(-Math.sin(spin) * size / 2.0D).add(up.scale(Math.cos(spin) * size / 2.0D));

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        point(builder, matrix, half.add(other), color, (float) brightness);
        point(builder, matrix, other.subtract(half), color, (float) brightness);
        point(builder, matrix, half.add(other).scale(-1.0D), color, (float) brightness);
        point(builder, matrix, half.subtract(other), color, (float) brightness);
        BufferUploader.drawWithShader(builder.end());
    }

    // 该 pose 已平移到「本体相对相机」的位置，平移列的长度就是到相机的距离
    public static double distance(PoseStack pose)
    {
        Matrix4f matrix = pose.last().pose();

        return Math.sqrt(matrix.m30() * matrix.m30() + matrix.m31() * matrix.m31() + matrix.m32() * matrix.m32());
    }

    // 距离补偿：锚越远几何越大，屏幕上看起来差不多（64 格为基准）
    public static double distanceScale(PoseStack pose)
    {
        double scale = 0.5D + 0.5D * distance(pose) / DISTANCE_REFERENCE;

        return Math.max(DISTANCE_SCALE_MIN, Math.min(DISTANCE_SCALE_MAX, scale));
    }

    // 本体几何基准半径：{size} 的一半再乘距离补偿
    public static double coreRadius(PoseStack pose, WhimParams params, WhimData data)
    {
        return 0.5D * params.number(data, "size", 1.0D) * distanceScale(pose);
    }

    // 绕任意轴旋转（Rodrigues），axis 须为单位向量
    public static Vec3 rotate(Vec3 vector, Vec3 axis, double angle)
    {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return vector.scale(cos).add(axis.cross(vector).scale(sin)).add(axis.scale(axis.dot(vector) * (1.0D - cos)));
    }

    // 与给定方向垂直的任意单位向量
    public static Vec3 perpendicular(Vec3 normal)
    {
        Vec3 reference = Math.abs(normal.y) < 0.9D ? UP : X_AXIS;
        return reference.cross(normal).normalize();
    }

    // 视线与世界上方向叉乘得右向；视线竖直时兜底 +X
    private static Vec3 right(Vec3 dir)
    {
        Vec3 right = dir.cross(UP);

        if (right.lengthSqr() < 1.0E-6D)
        {
            return X_AXIS;
        }

        return right.normalize();
    }

    private static Vec3 ringPoint(Vec3 right, Vec3 up, double radius, double angle)
    {
        return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
    }

    private static void point(BufferBuilder builder, Matrix4f matrix, Vec3 position, float[] color, float alpha)
    {
        float shade = (float) Math.max(0.0D, Math.min(1.0D, alpha));

        builder.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(color[0], color[1], color[2], color[3] * shade)
                .endVertex();
    }
}
