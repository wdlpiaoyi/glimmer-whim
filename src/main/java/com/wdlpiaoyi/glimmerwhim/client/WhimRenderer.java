package com.wdlpiaoyi.glimmerwhim.client;

import java.util.UUID;

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
    private static final double SIZE = 0.25D;

    /** 高亮比面片宽出去多少。 */
    private static final double OUTLINE = 0.08D;

    private static final float[] PURPLE = { 1.0F, 0.0F, 1.0F, 1.0F };
    private static final float[] BLACK = { 0.0F, 0.0F, 0.0F, 1.0F };
    private static final float[] WHITE = { 1.0F, 1.0F, 1.0F, 1.0F };

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
        Vec3 look = minecraft.player.getViewVector(partialTick);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        UUID aimed = WhimAim.update(level, eye, look, partialTick);

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

            if (whim.id().equals(aimed))
            {
                WhimTypes.highlight(whim.element()).draw(pose, dir);
            }
            else
            {
                checkerboard(pose, dir);
            }

            pose.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** dev 元素的高亮：外面套一圈白边。 */
    public static void whiteOutline(PoseStack pose, Vec3 dir)
    {
        quad(pose, dir, SIZE + OUTLINE, WHITE, WHITE);
        checkerboard(pose, dir);
    }

    private static void checkerboard(PoseStack pose, Vec3 dir)
    {
        quad(pose, dir, SIZE, PURPLE, BLACK);
    }

    private static void quad(PoseStack pose, Vec3 dir, double size, float[] first, float[] second)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Vec3 right = right(dir);
        Vec3 up = right.cross(dir).normalize();
        Vec3 origin = right.scale(-size).add(up.scale(-size));
        Vec3 u = right.scale(2.0D * size);
        Vec3 v = up.scale(2.0D * size);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 切成 2x2 —— 就是原版贴图丢了那副样子。
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                double i0 = i / 2.0D;
                double i1 = (i + 1) / 2.0D;
                double j0 = j / 2.0D;
                double j1 = (j + 1) / 2.0D;
                float[] color = ((i + j) & 1) == 0 ? first : second;

                corner(builder, matrix, origin.add(u.scale(i0)).add(v.scale(j0)), color);
                corner(builder, matrix, origin.add(u.scale(i1)).add(v.scale(j0)), color);
                corner(builder, matrix, origin.add(u.scale(i1)).add(v.scale(j1)), color);
                corner(builder, matrix, origin.add(u.scale(i0)).add(v.scale(j1)), color);
            }
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static Vec3 right(Vec3 dir)
    {
        Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (right.lengthSqr() < 1.0E-6D)
        {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }

        return right.normalize();
    }

    private static void corner(BufferBuilder builder, Matrix4f matrix, Vec3 position, float[] color)
    {
        builder.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
    }
}
