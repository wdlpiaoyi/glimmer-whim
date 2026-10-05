package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSight;
import com.wdlpiaoyi.glimmerwhim.net.WhimAimPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public final class WhimAim
{
    private static UUID aimed;
    // 上次已发给服务端的值，避免每 tick 重复发包
    private static UUID sent;

    private WhimAim()
    {
    }

    public static UUID aimed()
    {
        return aimed;
    }

    public static void flush()
    {
        if (Minecraft.getInstance().getConnection() == null)
        {
            sent = null;
            return;
        }

        if (Objects.equals(aimed, sent))
        {
            return;
        }

        sent = aimed;
        WhimNetwork.CHANNEL.sendToServer(new WhimAimPacket(aimed));
    }

    public static void clear()
    {
        aimed = null;
        sent = null;
    }

    // 仅当目标变化时才发包
    // 遍历 reach 内可交互灵感，取命中测试最近者，按遮挡配置与视线剔除
    public static UUID update(ClientLevel level, Vec3 eye, Vec3 look, float partialTick)
    {
        ClientWhimCache.WhimView best = null;
        double bestDot = 0.0D;
        double bestHit = Double.POSITIVE_INFINITY;
        double reach = WhimReach.blocks();
        Vec3 direction = look.normalize();

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            if (!whim.type().interactable(whim.data()))
            {
                continue;
            }

            Vec3 at = whim.anchor().position(level, eye, partialTick).orElse(null);

            if (at == null)
            {
                continue;
            }

            Vec3 toIt = at.subtract(eye);
            double distanceSqr = toIt.lengthSqr();

            if (distanceSqr < 1.0E-12D)
            {
                continue;
            }

            if (distanceSqr > reach * reach)
            {
                continue;
            }

            if ((!WhimConfig.aimThroughWalls() || whim.type().requiresLineOfSight())
                    && WhimSight.occluded(level, eye, at, Minecraft.getInstance().player,
                            whim.type().occludedByBlocks(whim.data()), whim.type().entityOccluders(whim.data()),
                            whim.type().entityOcclusionRenderBox(whim.data())))
            {
                continue;
            }

            WhimParams params = whim.type().effectiveParams();
            double hit = WhimRenderer.hitVolume(params, whim.data()).test(eye, direction, at, whim.data(), params);

            if (hit < 0.0D || hit >= bestHit)
            {
                continue;
            }

            best = whim;
            bestHit = hit;
            bestDot = toIt.normalize().dot(direction);
        }

        UUID id = best == null ? null : best.id();

        if (!Objects.equals(id, aimed))
        {
            aimed = id;

            if (best == null)
            {
                GlimmerWhim.log("[Whim] aim id=-");
            }
            else
            {
                double angle = Math.toDegrees(Math.acos(Math.min(1.0D, bestDot)));
                GlimmerWhim.log("[Whim] aim id={} element={} angle={}",
                        best.id(), best.type().id(), String.format(Locale.ROOT, "%.1f", angle));
            }
        }

        return aimed;
    }
}
