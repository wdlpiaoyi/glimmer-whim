package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.net.WhimHoldPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimUsePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimVoidPacket;
import com.wdlpiaoyi.glimmerwhim.whims.WhimRole;

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

    private static List<UUID> chain;
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
            chain = null;
            wasDown = INTERACT.isDown();
            return;
        }

        boolean down = INTERACT.isDown();

        if (down)
        {
            if (!wasDown)
            {
                chain = null;
                UUID root = WhimAim.aimed();
                ClientWhimCache.WhimView view = root == null ? null : ClientWhimCache.view(root);

                if (view != null && view.type().roles().contains(WhimRole.ELEMENT))
                {
                    chain = new ArrayList<>();
                    chain.add(root);
                    WhimNetwork.CHANNEL.sendToServer(new WhimHoldPacket(root));
                }
            }

            if (chain != null)
            {
                if (!ClientWhimCache.contains(chain.get(0)))
                {
                    if (minecraft.getConnection() != null)
                    {
                        WhimNetwork.CHANNEL.sendToServer(new WhimVoidPacket(List.copyOf(chain)));
                    }

                    chain = null;
                }
                else
                {
                    chain.removeIf(id -> !ClientWhimCache.contains(id));

                    if (chain.isEmpty())
                    {
                        chain = null;
                    }
                    else
                    {
                        append(WhimAim.aimed());
                    }
                }
            }
        }
        else if (chain != null)
        {
            if (minecraft.getConnection() != null && minecraft.level != null)
            {
                WhimTarget target = WhimTargeting.pick(minecraft.level, player, player.getEyePosition(),
                        FreeLook.viewVector(player, 1.0F));

                WhimNetwork.CHANNEL.sendToServer(new WhimUsePacket(List.copyOf(chain), target));
            }

            chain = null;
        }

        wasDown = down;
    }

    private static void append(UUID id)
    {
        if (id == null || chain.contains(id))
        {
            return;
        }

        ClientWhimCache.WhimView view = ClientWhimCache.view(id);

        if (view != null && view.type().roles().contains(WhimRole.MODIFIER))
        {
            chain.add(id);
        }
    }
}
