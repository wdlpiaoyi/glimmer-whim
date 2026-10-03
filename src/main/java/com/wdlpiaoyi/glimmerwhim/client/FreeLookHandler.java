package com.wdlpiaoyi.glimmerwhim.client;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 自由视角的按键和两个事件：按住还是切换看配置（默认按住）。
 * <p>
 * 键位在 {@code 选项 → 控制 → 微光奇想} 的"自由视角"里，默认左 Alt；按法在
 * {@code config/glimmerwhim-client.toml} 的 {@code [freelook] mode} 里（HOLD / TOGGLE）。
 */
public final class FreeLookHandler
{
    private static final KeyMapping KEY = new KeyMapping("key.glimmerwhim.freelook",
            GLFW.GLFW_KEY_LEFT_ALT, "key.categories.glimmerwhim");

    /** 上一 tick 自由视角键按着没有：切换模式只看"按下去那一下"，不看按住了多久。 */
    private static boolean wasDown;

    /** 上一 tick 镜头有没有盖住人的朝向：松开那一下要拿它决定补不补一条朝向给服务端。 */
    private static boolean wasOverriding;

    private FreeLookHandler()
    {
    }

    /** 把键登记进去，玩家才能在设置里看到它。 */
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event)
    {
        event.register(KEY);
    }

    /** 每 tick 看一次：按住模式看它按没按住，切换模式看这一 tick 里点没点过。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        // 松开以后往回退那一下的收尾。
        FreeLook.tick();

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
        {
            FreeLook.set(false, null);

            // 世界还在加载时按的键，账要在这儿清掉 —— 不然等会儿一次性放出来，视角会自己连开连关。
            while (KEY.consumeClick())
            {
            }

            wasDown = KEY.isDown();
            wasOverriding = false;
            return;
        }

        // 界面开着的时候按键不算数 —— 不然在键位界面里按一下，视角就跟着切了。
        boolean ui = Minecraft.getInstance().screen != null;

        if (WhimConfig.freeLookMode() == WhimConfig.FreeLookMode.HOLD)
        {
            FreeLook.set(KEY.isDown(), player);

            while (KEY.consumeClick())
            {
                // 按住模式不看"点了几下"，清掉这些账，免得哪天切成切换模式时一次冒出好几下。
            }

            wasDown = KEY.isDown();
        }
        else
        {
            boolean down = KEY.isDown();
            boolean clicked = false;

            while (KEY.consumeClick())
            {
                clicked = true;
            }

            // 只看"按下去那一下"：按住不放时的重复、加载时攒下的点击，都不该让视角连开连关。
            if (!wasDown && (down || clicked) && !ui)
            {
                FreeLook.set(!FreeLook.active(), player);
            }

            wasDown = down;
        }

        // 服务端也得知道"我在看哪儿"：自由视角开着（以及正往回退）的时候，每 tick 补一条朝向包。
        // 拉弓、扔珍珠、放楼梯的朝向都是服务端拿玩家的 yRot / xRot 现算的
        // （BowItem 里就是 player.getXRot() / player.getYRot()），不补的话画面跟着镜头、
        // 箭却照着身体的朝向飞，两边对不上。松开时再补一条人的朝向，把服务端换回来。
        boolean overriding = FreeLook.overriding();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();

        if (connection != null)
        {
            if (overriding)
            {
                connection.send(new ServerboundMovePlayerPacket.Rot(
                        FreeLook.viewYaw(player, 1.0F), FreeLook.viewPitch(player, 1.0F), player.onGround()));
            }
            else if (wasOverriding)
            {
                connection.send(new ServerboundMovePlayerPacket.Rot(
                        player.getYRot(), player.getXRot(), player.onGround()));
            }
        }

        wasOverriding = overriding;
    }

    /** 镜头的角度：开着（以及刚松开正往回退）的时候交给我们自己算，人转不转是另一回事。 */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null || !FreeLook.overriding())
        {
            return;
        }

        float partialTick = (float) event.getPartialTick();
        event.setYaw(FreeLook.viewYaw(player, partialTick));
        event.setPitch(FreeLook.viewPitch(player, partialTick));
    }
}
