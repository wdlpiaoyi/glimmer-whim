package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
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

    public static void clear()
    {
        WHIMES.clear();
        dimension = null;
    }

    /**
     * 这个包是不是我这个世界的。
     * <p>
     * 是——顺手把上一个世界的清了；不是——丢掉，别让旧包的维度把现在的表带歪。
     */
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

    /**
     * 玩家自己走的地方（换维度、回主菜单）不一定有包过来 —— 所以每 tick 对一次自己的维度。
     * 新维度里一条灵感都没有的时候，就靠这一下把上一个维度的清掉。
     */
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
