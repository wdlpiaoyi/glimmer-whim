package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientWhimCache
{
    public record WhimView(UUID id, WhimAnchor anchor, WhimType type, WhimData data)
    {
    }

    private static final Map<UUID, WhimView> WHIMES = new LinkedHashMap<>();
    private static final Map<UUID, Integer> REMAINING = new LinkedHashMap<>();
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

        WhimType type = Whims.get(packet.element());

        if (type == null)
        {
            return;
        }

        WHIMES.put(packet.id(), new WhimView(packet.id(), packet.anchor(), type, packet.data()));
        REMAINING.put(packet.id(), lifetime(packet.data()));
    }

    public static void remove(WhimRemovePacket packet)
    {
        if (!sameWorld(packet.dimension()))
        {
            return;
        }

        WHIMES.remove(packet.id());
        REMAINING.remove(packet.id());
    }

    public static Collection<WhimView> all()
    {
        return WHIMES.values();
    }

    public static boolean contains(UUID id)
    {
        return WHIMES.containsKey(id);
    }

    public static WhimView view(UUID id)
    {
        return WHIMES.get(id);
    }

    public static void clear()
    {
        WHIMES.clear();
        REMAINING.clear();
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
        REMAINING.clear();
        dimension = incoming;
        return true;
    }

    private static int lifetime(WhimData data)
    {
        try
        {
            return Integer.parseInt(data.get(Whim.LIFETIME).orElse("-1"));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    public static void tick()
    {
        if (REMAINING.isEmpty())
        {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = REMAINING.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue();

            if (remaining < 0)
            {
                continue;
            }

            remaining--;

            if (remaining <= 0)
            {
                iterator.remove();
                WHIMES.remove(entry.getKey());
            }
            else
            {
                entry.setValue(remaining);
            }
        }
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
            REMAINING.clear();
            dimension = current;
        }

        tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }
}
