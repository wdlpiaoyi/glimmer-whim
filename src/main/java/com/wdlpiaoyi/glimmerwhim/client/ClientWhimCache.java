package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSpawnPacket;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端这边的镜像：服务端发过来什么，这里就是什么，不做判断也不做过滤。
 */
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

    public static void accept(WhimSpawnPacket packet)
    {
        switchDimension(packet.dimension());
        WHIMES.put(packet.id(), new WhimView(packet.id(), packet.anchor(), packet.element(), packet.data(), packet.lifetime()));
    }

    public static void remove(WhimRemovePacket packet)
    {
        switchDimension(packet.dimension());
        WHIMES.remove(packet.id());
    }

    public static Collection<WhimView> all()
    {
        return WHIMES.values();
    }

    public static void clear()
    {
        WHIMES.clear();
        dimension = null;
    }

    /** 换了世界就把上一个世界的东西全清了 —— 那边的东西不会跟过来。 */
    private static void switchDimension(ResourceKey<Level> incoming)
    {
        if (!incoming.equals(dimension))
        {
            WHIMES.clear();
            dimension = incoming;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }
}
