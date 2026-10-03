package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** 瞄准：锥里碰到的、离准星最近的那条。纯客户端，不发包。 */
public final class WhimAim
{
    private static UUID aimed;

    private WhimAim()
    {
    }

    /** 每帧算一次，返回被瞄上的那条。 */
    public static UUID update(ClientLevel level, Vec3 eye, Vec3 look, float partialTick)
    {
        ClientWhimCache.WhimView best = null;
        double bestDot = -1.0D;

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            Vec3 direction = whim.anchor().direction(level, eye, partialTick).orElse(null);

            if (direction == null || direction.lengthSqr() < 1.0E-12D)
            {
                continue;
            }

            double dot = direction.normalize().dot(look);

            if (dot < Math.cos(Math.toRadians(WhimConfig.aimConeDegrees(whim.element()))))
            {
                continue;
            }

            if (dot > bestDot)
            {
                best = whim;
                bestDot = dot;
            }
        }

        UUID id = best == null ? null : best.id();

        if (!Objects.equals(id, aimed))
        {
            aimed = id;

            if (best == null)
            {
                GlimmerWhim.LOGGER.info("[Whim] aim id=-");
            }
            else
            {
                double angle = Math.toDegrees(Math.acos(Math.min(1.0D, bestDot)));
                GlimmerWhim.LOGGER.info("[Whim] aim id={} element={} angle={}",
                        best.id(), best.element(), String.format(Locale.ROOT, "%.1f", angle));
            }
        }

        return aimed;
    }
}
