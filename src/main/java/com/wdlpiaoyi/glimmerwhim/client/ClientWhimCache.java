package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimUpdatePacket;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
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

    // 客户端镜像，仅用于渲染与瞄准，服务端才是权威
    private static final Map<UUID, WhimView> WHIMES = new LinkedHashMap<>();
    // 缓存所属维度；跨维度或重连时整体清空
    private static ResourceKey<Level> dimension;

    private ClientWhimCache()
    {
    }

    public static void accept(WhimSummonPacket packet)
    {
        // 维度不符、锚点为空或元素未知时直接丢弃
        if (!sameWorld(packet.dimension()) || packet.anchor() == null)
        {
            return;
        }

        WhimType type = Whims.get(packet.element());

        if (type == null)
        {
            return;
        }

        WHIMES.put(packet.id(), new WhimView(packet.id(), packet.anchor(), type, packet.data()));
    }

    public static void remove(WhimRemovePacket packet)
    {
        if (!sameWorld(packet.dimension()))
        {
            return;
        }

        WHIMES.remove(packet.id());
    }

    // 只换数据；未持有该灵感时忽略（可见性变更会重新走召唤/移除）
    public static void update(WhimUpdatePacket packet)
    {
        if (!sameWorld(packet.dimension()))
        {
            return;
        }

        WHIMES.computeIfPresent(packet.id(),
                (id, view) -> new WhimView(view.id(), view.anchor(), view.type(), packet.data()));
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
        dimension = null;
    }

    // 维度不符返回 false；首次匹配到当前维度时清掉旧维度缓存
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
