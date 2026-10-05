package com.wdlpiaoyi.glimmerwhim.whims.content.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
import com.wdlpiaoyi.glimmerwhim.whims.content.StrikeWhim;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

// 落雷本体：平时默认外观，蓄力态绕视线轴自转；被瞄准时放大后描边
public final class StrikeRender
{
    private static final float HIGHLIGHT_SCALE = 1.25F;
    private static final float SPIN_SPEED = 0.35F;
    // 本体停在最远 128 格的空中，按原尺寸瞄太苛刻：按几何中心把几何体放大到 2 倍
    private static final double HIT_SCALE = 2.0D;

    private StrikeRender()
    {
    }

    public static void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        if (data.get(StrikeWhim.CHARGING).isEmpty())
        {
            DefaultRender.draw(pose, dir, data, params);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // time 用游戏刻加帧插值，自转与帧率无关
        float time = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + minecraft.getFrameTime();
        Quaternionf spin = new Quaternionf().rotationAxis(time * SPIN_SPEED, (float) dir.x, (float) dir.y, (float) dir.z);

        pose.pushPose();
        pose.mulPose(spin);
        DefaultRender.draw(pose, dir, data, params);
        pose.popPose();
    }

    public static void highlight(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        pose.pushPose();
        pose.scale(HIGHLIGHT_SCALE, HIGHLIGHT_SCALE, HIGHLIGHT_SCALE);
        DefaultRender.outline(pose, dir, data, params);
        pose.popPose();
    }

    // 沿用绘制几何体，只是放大命中体积（远处也能选中）
    public static double hit(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        return DefaultRender.hit(eye, look, at, data, params, HIT_SCALE);
    }
}
