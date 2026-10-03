package com.wdlpiaoyi.glimmerwhim.client;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 自由视角的按键和两个事件：按住还是切换看配置（默认按住）。
 * <p>
 * 键位是 {@code 选项 → 控制} 里那条"自由视角"，默认左 Alt，随时能改。
 */
public final class FreeLookHandler
{
    private static final KeyMapping KEY = new KeyMapping("key.glimmerwhim.freelook",
            GLFW.GLFW_KEY_LEFT_ALT, "key.categories.glimmerwhim");

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

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
        {
            FreeLook.set(false, null);
            return;
        }

        if (WhimConfig.freeLookMode() == WhimConfig.FreeLookMode.HOLD)
        {
            FreeLook.set(KEY.isDown(), player);

            while (KEY.consumeClick())
            {
                // 按住模式不看"点了几下"，清掉这些账，免得哪天切成切换模式时一次冒出好几下。
            }
        }
        else
        {
            while (KEY.consumeClick())
            {
                FreeLook.set(!FreeLook.active(), player);
            }
        }
    }

    /** 镜头的角度：开着就交给我们自己算的，人转不转是另一回事。 */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        if (FreeLook.active())
        {
            event.setYaw(FreeLook.yaw());
            event.setPitch(FreeLook.pitch());
        }
    }
}
