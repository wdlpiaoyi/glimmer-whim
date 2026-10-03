package com.wdlpiaoyi.glimmerwhim.client;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 自由视角：镜头和人的朝向分家。人站哪儿镜头就在哪儿，只有"往哪儿看"归镜头自己管。
 * <p>
 * 这里只管几个数：开着没、镜头朝哪儿、松手以后往人的朝向上退的那一下。
 * 按键和事件在 {@link FreeLookHandler}；把"鼠标转人"那一刀掐掉、改成转镜头的是
 * {@code mixin.client.EntityTurnMixin}；让准星、手、我们自己的瞄准也听镜头的是
 * {@code mixin.client.LocalPlayerLookMixin}。
 * <p>
 * 那些 mixin 会早早碰到这个类，所以除了这几个数，什么也别往里放。
 */
public final class FreeLook
{
    private enum State
    {
        /** 没开：一切都还是人的朝向，跟原版一样。 */
        IDLE,
        /** 开着：鼠标转的是镜头。 */
        ACTIVE,
        /** 刚松手：镜头正从松开的地方往人的朝向上退。 */
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

    /** 退的那一趟用多久（毫秒），0 就是不退、松手就把镜头还给人。 */
    private static long fadeMillis()
    {
        return WhimConfig.freeLookFadeMillis();
    }

    /** 鼠标现在转的是不是镜头（退的那一下不算，那会儿人已经能自己转了）。 */
    public static boolean active()
    {
        return state == State.ACTIVE;
    }

    /** 准星、手、镜头现在该不该听我们的 —— 退的那一下也算。 */
    public static boolean overriding()
    {
        return state != State.IDLE;
    }

    /**
     * 镜头（连带准星和手）现在该朝哪个 yaw。
     * <p>
     * 没开的时候返回的就是人自己的朝向，原版怎么样还怎么样。
     */
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

    /** 跟 {@link #viewYaw} 一个意思，俯仰那一路。 */
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

    /** 开的时候照着人当下的朝向起手，免得镜头跳一下；关的时候开始往人的朝向上退。 */
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
                // 正退到一半又按下去：从现在镜头待的地方接着看，别弹回人的朝向。
                yaw = viewYaw(player, 1.0F);
                pitch = viewPitch(player, 1.0F);
            }
            else
            {
                yaw = player.getYRot();
                pitch = player.getXRot();
            }

            state = State.ACTIVE;
            GlimmerWhim.LOGGER.info("[Whim] freelook on");
            return;
        }

        if (state != State.ACTIVE)
        {
            // 没开就不用关；正在退的那段让它退完，别重新起头。
            return;
        }

        fadeFromYaw = yaw;
        fadeFromPitch = pitch;
        fadeStart = System.currentTimeMillis();
        state = State.FADING;
        GlimmerWhim.LOGGER.info("[Whim] freelook off");
    }

    /** 每 tick 收个尾：退完了就彻底把镜头交还给人。 */
    public static void tick()
    {
        if (state == State.FADING && System.currentTimeMillis() - fadeStart >= fadeMillis())
        {
            state = State.IDLE;
        }
    }

    /**
     * 鼠标要把人转这么多。收到的就是原版 {@code MouseHandler} 那份（灵敏度、平滑都在里面了）。
     * <p>
     * 原版的下一步是 {@code Entity.turn} 里的 {@code * 0.15F}，那一刀被我们掐了，所以在这儿补上 ——
     * 不补的话镜头会比原版快上一大截。
     * <p>
     * 返回 true 表示这一刀别落到人身上，转镜头就行了。
     */
    public static boolean turn(double yRot, double xRot)
    {
        if (state != State.ACTIVE)
        {
            return false;
        }

        yaw += (float) yRot * 0.15F;
        pitch = Mth.clamp(pitch + (float) xRot * 0.15F, -90.0F, 90.0F);

        return true;
    }

    /** 退回人朝向的进度：0 到 1，两头慢中间快。 */
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
