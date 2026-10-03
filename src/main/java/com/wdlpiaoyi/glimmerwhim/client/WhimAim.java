package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.net.WhimAimPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhimAim
{
    private static UUID aimed;

    private WhimAim()
    {
    }

    public static UUID aimed()
    {
        return aimed;
    }

    public static UUID update(ClientLevel level, Vec3 eye, Vec3 look, float partialTick)
    {
        ClientWhimCache.WhimView best = null;
        double bestDot = 0.0D;
        double bestHit = Double.POSITIVE_INFINITY;
        double reach = WhimReach.blocks();
        Vec3 direction = look.normalize();

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            WhimParams params = WhimAnchors.params(whim.anchor().type());
            Vec3 at = whim.anchor().position(level, eye, partialTick, whim.data(), params).orElse(null);

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

            if (!WhimConfig.aimThroughWalls() && occluded(level, eye, at))
            {
                continue;
            }

            double hit = WhimRenderer.hit(whim.element()).test(eye, direction, at, whim.data(), params);

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
                        best.id(), best.element(), String.format(Locale.ROOT, "%.1f", angle));
            }

            if (Minecraft.getInstance().getConnection() != null)
            {
                WhimNetwork.CHANNEL.sendToServer(new WhimAimPacket(id));
            }
        }

        return aimed;
    }

    private static boolean occluded(ClientLevel level, Vec3 eye, Vec3 at)
    {
        BlockHitResult hit = level.clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                Minecraft.getInstance().player));

        return hit.getType() != HitResult.Type.MISS;
    }
}
