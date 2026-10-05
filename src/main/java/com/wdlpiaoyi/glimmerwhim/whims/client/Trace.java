package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface Trace
{
    void draw(PoseStack pose, List<Vec3> points, Vec3 endpoint, float fade, WhimData data, WhimParams params);

    // 默认淡出时长，单位毫秒
    default float fadeMillis()
    {
        return 300.0F;
    }
}
