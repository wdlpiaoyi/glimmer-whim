package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimUpdatePacket;
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

    // 客户端镜像，仅用于渲染与瞄准，服务端才是权威
    private static final Map<UUID, WhimView> WHIMES = new LinkedHashMap<>();
    // 本地倒计时（tick），只用于表现（例如脉动频率）；寿命是否结束仍以服务端为准
    private static final Map<UUID, Integer> REMAINING = new LinkedHashMap<>();
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
        int lifetime = lifetime(packet.data());

        if (lifetime >= 0)
        {
            REMAINING.put(packet.id(), lifetime);
        }
        else
        {
            REMAINING.remove(packet.id());
        }
    }

    public static void remove(WhimRemovePacket packet)
    {
        if (!sameWorld(packet.dimension()))
        {
            return;
        }

        WhimView view = WHIMES.remove(packet.id());
        REMAINING.remove(packet.id());

        // 本体消失后可以继续演一段消散：时长与画法由类型和渲染登记决定
        if (view != null)
        {
            WhimVanish.begin(view);
        }
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
        REMAINING.clear();
        WhimVanish.clear();
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

        clear();
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
            clear();
            dimension = current;
        }

        countdown();
    }

    // 可被任何表现读取的剩余寿命比例（0..1）；永久或未知返回 1
    public static float remainingFraction(UUID id, WhimData data)
    {
        Integer left = REMAINING.get(id);
        int total = lifetime(data);

        if (left == null || total <= 0)
        {
            return 1.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, left / (float) total));
    }

    // 每 tick 递减；按住期间服务端会冻结寿命，本地倒计时跟着冻结
    private static void countdown()
    {
        List<UUID> held = WhimInteractHandler.chain();

        for (Map.Entry<UUID, Integer> entry : REMAINING.entrySet())
        {
            if (entry.getValue() > 0 && (held == null || !held.contains(entry.getKey())))
            {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    private static int lifetime(WhimData data)
    {
        return data.get(Whim.LIFETIME).map(value ->
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException exception)
            {
                return -1;
            }
        }).orElse(-1);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        clear();
    }
}
