package com.wdlpiaoyi.glimmerwhim.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.world.entity.player.Player;

/**
 * 自由视角：镜头和人的朝向分家。人站哪儿镜头就在哪儿，只有"往哪儿看"归镜头自己管。
 * <p>
 * 这里只管几个数（开着没、往哪儿看），按键和事件在 {@link FreeLookHandler}，
 * 真正把"鼠标转人"那一刀掐掉的是 {@code mixin.client.EntityTurnMixin}。
 * <p>
 * 那个 mixin 会早早碰到这个类，所以除了这几个数，什么也别往里放。
 */
public final class FreeLook
{
    private static boolean active;
    private static float yaw;
    private static float pitch;

    private FreeLook()
    {
    }

    public static boolean active()
    {
        return active;
    }

    public static float yaw()
    {
        return yaw;
    }

    public static float pitch()
    {
        return pitch;
    }

    /** 开的时候照着人当下的朝向起手，免得镜头跳一下；关的时候不用收拾，下一帧镜头自己就回到人脸上。 */
    public static void set(boolean on, Player player)
    {
        if (on == active)
        {
            return;
        }

        active = on;

        if (on && player != null)
        {
            yaw = player.getYRot();
            pitch = player.getXRot();
        }

        GlimmerWhim.LOGGER.info("[Whim] freelook {}", on ? "on" : "off");
    }

    /**
     * 鼠标要把人转这么多（收到的就是原版乘过灵敏度和 0.15 的那份，手感天然一致）。
     * <p>
     * 返回 true 表示这一刀别落到人身上，转镜头就行了。
     */
    public static boolean turn(double yawDelta, double pitchDelta)
    {
        if (!active)
        {
            return false;
        }

        yaw += (float) yawDelta;
        pitch = (float) Math.max(-90.0D, Math.min(90.0D, pitch + pitchDelta));

        return true;
    }
}
