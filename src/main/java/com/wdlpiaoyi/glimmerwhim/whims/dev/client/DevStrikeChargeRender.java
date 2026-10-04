package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class DevStrikeChargeRender
{
    private static final float SPIN_SPEED = 0.35F;

    private DevStrikeChargeRender()
    {
    }

    public static void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level == null ? 0.0F : minecraft.level.getGameTime() + minecraft.getFrameTime();
        Quaternionf spin = new Quaternionf().rotationAxis(time * SPIN_SPEED, (float) dir.x, (float) dir.y, (float) dir.z);

        pose.pushPose();
        pose.mulPose(spin);
        DefaultRender.draw(pose, dir, data, params);
        pose.popPose();
    }
}
