package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

// 消散：本体消失后还在客户端演一段。时长由 WhimType.vanishMillis 声明，画法由渲染登记里的消散样式决定
// 位置在收到移除包时就定下，之后不跟随相机；只有移除包触发，维度切换与登出不演
public final class WhimVanish
{
    private record Fading(ClientWhimCache.WhimView whim, Vec3 at, long startMillis, int millis)
    {
    }

    private static final List<Fading> FADING = new ArrayList<>();

    // 视线与本体重合时的兜底方向
    private static final Vec3 FALLBACK_DIR = new Vec3(0.0D, 0.0D, 1.0D);

    private WhimVanish()
    {
    }

    public static void begin(ClientWhimCache.WhimView whim)
    {
        Minecraft minecraft = Minecraft.getInstance();
        WhimType type = whim.type();
        int millis = type.vanishMillis(whim.data());

        if (millis <= 0 || minecraft.level == null || minecraft.player == null || WhimRenderer.vanish(type.id()) == null)
        {
            return;
        }

        Vec3 at = whim.anchor().position(minecraft.level, minecraft.player.getEyePosition(), 1.0F).orElse(null);

        if (at == null)
        {
            return;
        }

        FADING.add(new Fading(whim, at, Util.getMillis(), millis));
    }

    public static boolean active()
    {
        return !FADING.isEmpty();
    }

    public static void render(PoseStack pose, Vec3 eye, Vec3 camera)
    {
        if (FADING.isEmpty())
        {
            return;
        }

        long now = Util.getMillis();
        Iterator<Fading> iterator = FADING.iterator();

        while (iterator.hasNext())
        {
            Fading fading = iterator.next();

            if (now - fading.startMillis() >= fading.millis())
            {
                iterator.remove();
                continue;
            }

            WhimType type = fading.whim().type();
            WhimRenderer.Vanish vanish = WhimRenderer.vanish(type.id());

            if (vanish == null)
            {
                iterator.remove();
                continue;
            }

            // 与本体一致：声明深度遮挡的仍被方块与实体挡住
            if (type.depthOcclusion(fading.whim().data()))
            {
                RenderSystem.enableDepthTest();
            }
            else
            {
                RenderSystem.disableDepthTest();
            }

            Vec3 offset = fading.at().subtract(eye);
            Vec3 dir = offset.lengthSqr() < 1.0E-8D ? FALLBACK_DIR : offset.normalize();
            Vec3 position = fading.at().subtract(camera);
            float progress = (now - fading.startMillis()) / (float) fading.millis();

            pose.pushPose();
            pose.translate(position.x, position.y, position.z);
            vanish.draw(pose, dir, progress, fading.whim().data(), type.params(), fading.whim().id());
            pose.popPose();
        }

        // 消散可能开过深度测试，交还给调用方原本的状态
        RenderSystem.disableDepthTest();
    }

    public static void clear()
    {
        FADING.clear();
    }
}
