package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;

import net.minecraft.world.phys.Vec3;

public final class DevStrikeRender
{
    private static final float HIGHLIGHT_SCALE = 1.25F;

    private DevStrikeRender()
    {
    }

    public static void highlight(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        pose.pushPose();
        pose.scale(HIGHLIGHT_SCALE, HIGHLIGHT_SCALE, HIGHLIGHT_SCALE);
        DefaultRender.outline(pose, dir, data, params);
        pose.popPose();
    }
}
