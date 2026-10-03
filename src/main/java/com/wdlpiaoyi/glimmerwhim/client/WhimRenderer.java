package com.wdlpiaoyi.glimmerwhim.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 开发者占位。紫黑面片，能看见就算完。 */
public final class WhimRenderer
{
    /** 摆多远。dev 用。 */
    private static final double DEV_DISTANCE = 8.0D;

    /** 半个边长。 */
    private static final double SIZE = 0.5D;

    private static final float[] PURPLE = { 1.0F, 0.0F, 1.0F, 1.0F };
    private static final float[] BLACK = { 0.0F, 0.0F, 0.0F, 1.0F };

    private WhimRenderer()
    {
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

        if (level == null || minecraft.player == null || ClientWhimCache.all().isEmpty())
        {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            WhimAnchor anchor = whim.anchor();
            Vec3 direction = anchor.direction(level, eye, partialTick).orElse(null);

            if (direction == null)
            {
                continue;
            }

            Vec3 dir = direction.normalize();
            Vec3 position = eye.add(dir.scale(DEV_DISTANCE)).subtract(camera);

            pose.pushPose();
            pose.translate(position.x, position.y, position.z);
            drawQuad(pose, dir);
            pose.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawQuad(PoseStack pose, Vec3 dir)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (right.lengthSqr() < 1.0E-6D)
        {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }

        right = right.normalize();

        Vec3 up = right.cross(dir).normalize();
        Vec3 origin = right.scale(-SIZE).add(up.scale(-SIZE));

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 切成 2x2 的紫黑棋盘 —— 就是原版贴图丢了那副样子。
        face(builder, matrix, origin, right.scale(2.0D * SIZE), up.scale(2.0D * SIZE));

        BufferUploader.drawWithShader(builder.end());
    }

    private static void face(BufferBuilder builder, Matrix4f matrix, Vec3 origin, Vec3 u, Vec3 v)
    {
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                double i0 = i / 2.0D;
                double i1 = (i + 1) / 2.0D;
                double j0 = j / 2.0D;
                double j1 = (j + 1) / 2.0D;

                float[] color = ((i + j) & 1) == 0 ? PURPLE : BLACK;

                corner(builder, matrix, origin.add(u.scale(i0)).add(v.scale(j0)), color);
                corner(builder, matrix, origin.add(u.scale(i1)).add(v.scale(j0)), color);
                corner(builder, matrix, origin.add(u.scale(i1)).add(v.scale(j1)), color);
                corner(builder, matrix, origin.add(u.scale(i0)).add(v.scale(j1)), color);
            }
        }
    }

    private static void corner(BufferBuilder builder, Matrix4f matrix, Vec3 position, float[] color)
    {
        builder.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
    }
}
