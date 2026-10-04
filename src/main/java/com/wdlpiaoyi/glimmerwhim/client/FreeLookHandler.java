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

public final class FreeLookHandler
{
    // 默认左 Alt；HOLD 模式长按生效，否则单击切换
    private static final KeyMapping KEY = new KeyMapping("key.glimmerwhim.freelook",
            GLFW.GLFW_KEY_LEFT_ALT, "key.categories.glimmerwhim");

    private static boolean wasDown;

    private FreeLookHandler()
    {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event)
    {
        event.register(KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        FreeLook.tick();

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
        {
            FreeLook.set(false, null);

            while (KEY.consumeClick())
            {
            }

            wasDown = KEY.isDown();
            return;
        }

        boolean ui = Minecraft.getInstance().screen != null;

        if (WhimConfig.freeLookMode() == WhimConfig.FreeLookMode.HOLD)
        {
            FreeLook.set(KEY.isDown(), player);

            while (KEY.consumeClick())
            {
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

            // 切换模式：仅在上升沿且无界面时切
            if (!wasDown && (down || clicked) && !ui)
            {
                FreeLook.set(!FreeLook.active(), player);
            }

            wasDown = down;
        }
    }

    @SubscribeEvent
    // 在相机角度事件里替换偏航/俯仰，仅覆盖渲染视角，不改实体朝向
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
