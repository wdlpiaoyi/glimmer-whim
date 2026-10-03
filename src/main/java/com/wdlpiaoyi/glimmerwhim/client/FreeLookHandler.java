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
 * 键位在 {@code 选项 → 控制 → 微光奇想} 的"自由视角"里，默认左 Alt；按法在
 * {@code config/glimmerwhim-client.toml} 的 {@code [freelook] mode} 里（HOLD / TOGGLE）。
 * <p>
 * 这里只管"看"：按住以后鼠标转的是镜头，人的朝向冻在原地 —— 挖掘、放置、攻击、拉弓这些动手的事
 * 照旧全按人的朝向算，一点没动；只有"瞄"（高亮）跟着镜头走，因为准星在屏幕正中间；松手镜头淡回人的朝向。
 */
public final class FreeLookHandler
{
    private static final KeyMapping KEY = new KeyMapping("key.glimmerwhim.freelook",
            GLFW.GLFW_KEY_LEFT_ALT, "key.categories.glimmerwhim");

    /** 上一 tick 自由视角键按着没有：切换模式只看"按下去那一下"，不看按住了多久。 */
    private static boolean wasDown;

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
