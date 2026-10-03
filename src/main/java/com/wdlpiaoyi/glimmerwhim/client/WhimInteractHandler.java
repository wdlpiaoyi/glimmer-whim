package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Objects;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import com.wdlpiaoyi.glimmerwhim.net.WhimHoldPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimUsePacket;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class WhimInteractHandler
{
    private static final KeyMapping INTERACT = new KeyMapping("key.glimmerwhim.interact", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.glimmerwhim");

    private static UUID held;
    private static boolean wasDown;

    private WhimInteractHandler()
    {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event)
    {
        event.register(INTERACT);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen != null)
        {
            held = null;
            wasDown = INTERACT.isDown();
            return;
        }

        boolean down = INTERACT.isDown();

        if (down)
        {
            if (!wasDown)
            {
                held = WhimAim.aimed();

                if (held != null && ClientWhimCache.contains(held))
                {
                    WhimNetwork.CHANNEL.sendToServer(new WhimHoldPacket(held));
                }
            }

            if (held != null && !ClientWhimCache.contains(held))
            {
                held = null;
            }
        }
        else if (held != null)
        {
            boolean success = Objects.equals(WhimAim.aimed(), held) && ClientWhimCache.contains(held);

            if (minecraft.getConnection() != null)
            {
                WhimNetwork.CHANNEL.sendToServer(new WhimUsePacket(held, success));
            }

            held = null;
        }

        wasDown = down;
    }
}
