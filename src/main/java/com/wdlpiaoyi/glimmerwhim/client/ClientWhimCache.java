package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientWhimCache
{
    public record WhimView(UUID id, WhimAnchor anchor, ResourceLocation element, WhimData data, int lifetime)
    {
    }

    private static final Map<UUID, WhimView> WHIMES = new LinkedHashMap<>();
    private static ResourceKey<Level> dimension;

    private ClientWhimCache()
    {
    }

    public static void accept(WhimSummonPacket packet)
    {
        if (!sameWorld(packet.dimension()))
        {
            return;
        }

        WHIMES.put(packet.id(), new WhimView(packet.id(), packet.anchor(), packet.element(), packet.data(), packet.lifetime()));
    }

    public static void remove(WhimRemovePacket packet)
    {
        if (!sameWorld(packet.dimension()))
        {
            return;
        }

        WHIMES.remove(packet.id());
    }

    public static Collection<WhimView> all()
    {
        return WHIMES.values();
    }

    public static boolean contains(UUID id)
    {
        return WHIMES.containsKey(id);
    }

    public static void clear()
    {
        WHIMES.clear();
        dimension = null;
    }

    private static boolean sameWorld(ResourceKey<Level> incoming)
    {
        if (incoming.equals(dimension))
        {
            return true;
        }

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null || !incoming.equals(level.dimension()))
        {
            return false;
        }

        WHIMES.clear();
        dimension = incoming;
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null)
        {
            clear();
            return;
        }

        ResourceKey<Level> current = level.dimension();

        if (!current.equals(dimension))
        {
            WHIMES.clear();
            dimension = current;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }
}
