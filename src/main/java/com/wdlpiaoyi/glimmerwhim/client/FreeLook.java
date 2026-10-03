package com.wdlpiaoyi.glimmerwhim.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FreeLook
{
    private enum State
    {
        IDLE,
        ACTIVE,
        FADING
    }

    private static State state = State.IDLE;
    private static float yaw;
    private static float pitch;
    private static float fadeFromYaw;
    private static float fadeFromPitch;
    private static long fadeStart;

    private FreeLook()
    {
    }

    private static long fadeMillis()
    {
        return WhimConfig.freeLookFadeMillis();
    }

    public static boolean active()
    {
        return state == State.ACTIVE;
    }

    public static boolean overriding()
    {
        return state != State.IDLE;
    }

    public static float viewYaw(Player player, float partialTick)
    {
        float body = Mth.lerp(partialTick, player.yRotO, player.getYRot());

        switch (state)
        {
            case ACTIVE:
                return yaw;
            case FADING:
                return Mth.rotLerp(fade(), fadeFromYaw, body);
            default:
                return body;
        }
    }

    public static float viewPitch(Player player, float partialTick)
    {
        float body = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        switch (state)
        {
            case ACTIVE:
                return pitch;
            case FADING:
                return Mth.lerp(fade(), fadeFromPitch, body);
            default:
                return body;
        }
    }

    public static Vec3 viewVector(Player player, float partialTick)
    {
        return Vec3.directionFromRotation(viewPitch(player, partialTick), viewYaw(player, partialTick));
    }

    public static void set(boolean on, Player player)
    {
        if (on && player != null)
        {
            if (state == State.ACTIVE)
            {
                return;
            }

            if (state == State.FADING)
            {
                yaw = viewYaw(player, 1.0F);
                pitch = viewPitch(player, 1.0F);
            }
            else
            {
                yaw = player.getYRot();
                pitch = player.getXRot();
            }

            state = State.ACTIVE;
            GlimmerWhim.log("[Whim] freelook on");
            return;
        }

        if (state != State.ACTIVE)
        {
            return;
        }

        fadeFromYaw = yaw;
        fadeFromPitch = pitch;
        fadeStart = System.currentTimeMillis();
        state = State.FADING;
        GlimmerWhim.log("[Whim] freelook off");
    }

    public static void tick()
    {
        if (state == State.FADING && System.currentTimeMillis() - fadeStart >= fadeMillis())
        {
            state = State.IDLE;
        }
    }

    public static boolean turn(Player player, double yRot, double xRot)
    {
        if (state != State.ACTIVE)
        {
            return false;
        }

        float step = (float) (0.15D * WhimConfig.freeLookSensitivity());
        float limit = (float) WhimConfig.freeLookPitchLimitDegrees();
        float yawLimit = (float) WhimConfig.freeLookYawLimitDegrees();

        yaw += (float) yRot * step;
        pitch = Mth.clamp(pitch + (float) xRot * step, -limit, limit);

        float body = player.getYRot();
        yaw = body + Mth.clamp(Mth.wrapDegrees(yaw - body), -yawLimit, yawLimit);

        return true;
    }

    private static float fade()
    {
        float span = (float) fadeMillis();

        if (span <= 0.0F)
        {
            return 1.0F;
        }

        float t = Mth.clamp((System.currentTimeMillis() - fadeStart) / span, 0.0F, 1.0F);

        return t * t * (3.0F - 2.0F * t);
    }
}
