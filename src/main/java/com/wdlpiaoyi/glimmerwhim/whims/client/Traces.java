package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.ArrayList;
import java.util.List;

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

import net.minecraft.world.phys.Vec3;

public final class Traces
{
    private static final float[] DEFAULT_COLOR = { 1.0F, 1.0F, 1.0F, 1.0F };
    private static final float[] GLOW_COLOR = { 1.0F, 1.0F, 1.0F, 0.5F };

    private Traces()
    {
    }

    public static void line(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        float[] color = DEFAULT_COLOR;
        stroke(pose, points, endpoint, color[0], color[1], color[2], (1.0F - fade) * color[3],
                (float) WhimConfig.traceWidth());
    }

    public static void hue(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        float shift = (float) ((System.currentTimeMillis() % 5000L) / 5000.0D);
        float[] color = DevRender.rotateHue(WhimConfig.renderDevColorElement(), shift);
        stroke(pose, points, endpoint, color[0], color[1], color[2], (1.0F - fade) * color[3],
                (float) WhimConfig.traceWidth());
    }

    public static void glow(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data,
            WhimParams params)
    {
        stroke(pose, points, endpoint, GLOW_COLOR[0], GLOW_COLOR[1], GLOW_COLOR[2], (1.0F - fade) * GLOW_COLOR[3],
                (float) WhimConfig.traceWidth() * 2.0F);
    }

    private static void stroke(PoseStack pose, List<Vec3> points, Vec3 endpoint, float r, float g, float b, float a,
            float width)
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

        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        RenderSystem.lineWidth(width);
        builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (Vec3 point : all)
        {
            builder.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(r, g, b, a)
                    .endVertex();
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.lineWidth(1.0F);
    }
}
