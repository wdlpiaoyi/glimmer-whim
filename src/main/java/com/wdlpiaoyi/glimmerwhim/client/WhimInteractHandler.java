package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.net.WhimHoldPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimUsePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimVoidPacket;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class WhimInteractHandler
{
    // 鼠标右键；不 consume 原版 use，两者照常触发
    private static final KeyMapping INTERACT = new KeyMapping("key.glimmerwhim.interact", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.glimmerwhim");

    // 按住期间维护的链快照；null 表示未在交互
    private static List<UUID> chain;
    // 最后一次发给服务端的链快照，避免重复发包
    private static List<UUID> sent;
    private static boolean wasDown;
    // 按住期间是否离开过链根；离开再回到根才算以根为目标
    private static boolean left;

    private WhimInteractHandler()
    {
    }

    public static List<UUID> chain()
    {
        return chain;
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
        WhimAim.flush();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen != null)
        {
            // 掉线或打开界面时作废链，避免残留
            if (chain != null)
            {
                if (minecraft.getConnection() != null)
                {
                    WhimNetwork.CHANNEL.sendToServer(new WhimVoidPacket(List.copyOf(chain)));
                }

                WhimTrace.dissolve();
            }

            chain = null;
            sent = null;
            left = false;
            wasDown = INTERACT.isDown();
            return;
        }

        boolean down = INTERACT.isDown();

        if (down)
        {
            // 按下沿：以当前瞄准为根开链并即时通知服务端
            if (!wasDown)
            {
                chain = null;
                UUID root = WhimAim.aimed();
                ClientWhimCache.WhimView view = root == null ? null : ClientWhimCache.view(root);

                if (view != null && view.type().canChain(view.data()) && view.type().canRoot())
                {
                    chain = new ArrayList<>();
                    chain.add(root);
                    left = false;
                    sent = null;
                    syncHold(minecraft);
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

                    WhimTrace.dissolve();
                    chain = null;
                    sent = null;
                }
                else
                {
                    chain.removeIf(id -> !ClientWhimCache.contains(id));
                    syncHold(minecraft);

                    if (chain.isEmpty())
                    {
                        WhimTrace.dissolve();
                        chain = null;
                    }
                    else
                    {
                        if (!Objects.equals(WhimAim.aimed(), chain.get(0)))
                        {
                            left = true;
                        }

                        append(WhimAim.aimed());
                        syncHold(minecraft);
                    }
                }
            }
        }
        else if (chain != null)
        {
            if (minecraft.getConnection() != null && minecraft.level != null)
            {
                // 松开沿：定稿链，并把准星目标交给服务端结算
                WhimTargeters.Context context = new WhimTargeters.Context(minecraft.level, player,
                        player.getEyePosition(), FreeLook.viewVector(player, 1.0F), chain.get(0), left);
                WhimTarget target = WhimTargeters.pick(context).orElse(null);

                WhimTrace.release(minecraft.level, player.getEyePosition(), chain, target);
                WhimNetwork.CHANNEL.sendToServer(new WhimUsePacket(List.copyOf(chain), target));
            }

            chain = null;
            sent = null;
            left = false;
        }

        wasDown = down;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        chain = null;
        sent = null;
        wasDown = false;
        left = false;
        WhimTrace.clear();
        WhimAim.clear();
    }

    // 链有变化时把整条链发给服务端，用于按链暂停寿命
    private static void syncHold(Minecraft minecraft)
    {
        if (chain == null || chain.equals(sent) || minecraft.getConnection() == null)
        {
            return;
        }

        sent = List.copyOf(chain);
        WhimNetwork.CHANNEL.sendToServer(new WhimHoldPacket(sent));
    }

    private static void append(UUID id)
    {
        if (id == null || chain.contains(id))
        {
            return;
        }

        ClientWhimCache.WhimView view = ClientWhimCache.view(id);

        if (view != null && view.type().canChain(view.data()))
        {
            chain.add(id);
        }
    }
}
