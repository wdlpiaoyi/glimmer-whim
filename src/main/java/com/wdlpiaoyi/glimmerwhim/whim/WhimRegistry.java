package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 一个维度一张表：这个维度里现在还活着的灵感。
 * <p>
 * {@link #removeWhim} 是唯一的出口。
 */
public final class WhimRegistry
{
    private static final Map<ResourceKey<Level>, WhimRegistry> REGISTRIES = new HashMap<>();

    /** 玩家 UUID -> 他现在瞄着的那条。客户端说了才算，只给命令补全用。 */
    private static final Map<UUID, UUID> AIMED = new HashMap<>();

    private record Tracked(Whim whim, WhimVisibility visibility)
    {
    }

    /** 找到的一条灵感落在哪个维度。 */
    public record Found(ResourceKey<Level> dimension, Whim whim)
    {
    }

    private final ServerLevel level;
    private final Map<UUID, Tracked> tracked = new LinkedHashMap<>();

    private WhimRegistry(ServerLevel level)
    {
        this.level = level;
    }

    public static WhimRegistry of(ServerLevel level)
    {
        return REGISTRIES.computeIfAbsent(level.dimension(), key -> new WhimRegistry(level));
    }

    public ServerLevel level()
    {
        return this.level;
    }

    /** 出现，所有人可见。 */
    public void summon(Whim whim)
    {
        this.summon(whim, WhimVisibility.ALL);
    }

    /** 出现，只发给能用它的人。 */
    public void summon(Whim whim, WhimVisibility visibility)
    {
        this.tracked.put(whim.id(), new Tracked(whim, visibility));

        GlimmerWhim.LOGGER.info("[Whim] summon id={} dim={} anchor={} element={} lifetime={} visibility={}",
                whim.id(), this.level.dimension().location(), whim.anchor().type(), whim.element(), whim.lifetime(),
                visibility == WhimVisibility.ALL ? "all" : "filtered");

        WhimSummonPacket packet = WhimSummonPacket.of(this.level.dimension(), whim);

        for (ServerPlayer player : this.level.players())
        {
            if (visibility.canUse(player, whim))
            {
                WhimNetwork.sendTo(player, packet);
            }
        }
    }

    /** 唯一出口。 */
    public void removeWhim(UUID id, WhimRemoveReason reason)
    {
        if (this.tracked.remove(id) == null)
        {
            return;
        }

        GlimmerWhim.LOGGER.info("[Whim] remove id={} dim={} reason={}", id, this.level.dimension().location(), reason);

        // 谁还瞄着它，就把 AIMED 里的旧值清掉 —— 不然补全会给一个已经没了的 uuid。
        AIMED.values().removeIf(id::equals);

        WhimRemovePacket packet = new WhimRemovePacket(this.level.dimension(), id, reason);

        for (ServerPlayer player : this.level.players())
        {
            WhimNetwork.sendTo(player, packet);
        }
    }

    /** 过一 tick：谁的 lifetime 见底了，谁走。永久的一直是负数，不会撞上 0。 */
    public void tick()
    {
        List<UUID> expired = new ArrayList<>();

        for (Tracked entry : this.tracked.values())
        {
            if (entry.whim().tick() == 0)
            {
                expired.add(entry.whim().id());
            }
        }

        for (UUID id : expired)
        {
            this.removeWhim(id, WhimRemoveReason.EXPIRED);
        }
    }

    /** 给某个玩家补一份快照 —— 上线、换维度都走这儿。 */
    public void sendSnapshot(ServerPlayer player)
    {
        for (Tracked entry : this.tracked.values())
        {
            if (entry.visibility().canUse(player, entry.whim()))
            {
                WhimNetwork.sendTo(player, WhimSummonPacket.of(this.level.dimension(), entry.whim()));
            }
        }
    }

    public Collection<Whim> all()
    {
        List<Whim> whimes = new ArrayList<>(this.tracked.size());

        for (Tracked entry : this.tracked.values())
        {
            whimes.add(entry.whim());
        }

        return Collections.unmodifiableList(whimes);
    }

    /** 所有维度加一块儿，空的不列。 */
    public static Map<ResourceKey<Level>, Collection<Whim>> allDimensions()
    {
        Map<ResourceKey<Level>, Collection<Whim>> dimensions = new LinkedHashMap<>();

        for (Map.Entry<ResourceKey<Level>, WhimRegistry> entry : REGISTRIES.entrySet())
        {
            Collection<Whim> whimes = entry.getValue().all();

            if (!whimes.isEmpty())
            {
                dimensions.put(entry.getKey(), whimes);
            }
        }

        return dimensions;
    }

    /** 按 id 找，跨维度。 */
    public static Optional<Found> find(UUID id)
    {
        for (Map.Entry<ResourceKey<Level>, WhimRegistry> entry : REGISTRIES.entrySet())
        {
            Tracked tracked = entry.getValue().tracked.get(id);

            if (tracked != null)
            {
                return Optional.of(new Found(entry.getKey(), tracked.whim()));
            }
        }

        return Optional.empty();
    }

    /** 按 id 移除，跨维度。 */
    public static boolean kill(UUID id)
    {
        for (WhimRegistry registry : REGISTRIES.values())
        {
            if (registry.tracked.containsKey(id))
            {
                registry.removeWhim(id, WhimRemoveReason.OTHERS);
                return true;
            }
        }

        return false;
    }

    /** 客户端说它现在瞄着哪条，null 表示没瞄。 */
    public static void setAimed(ServerPlayer player, UUID id)
    {
        if (id == null)
        {
            AIMED.remove(player.getUUID());
        }
        else
        {
            AIMED.put(player.getUUID(), id);
        }
    }

    public static UUID aimed(ServerPlayer player)
    {
        return AIMED.get(player.getUUID());
    }

    private void unload()
    {
        if (!this.tracked.isEmpty())
        {
            GlimmerWhim.LOGGER.info("[Whim] unload dim={} dropped={}", this.level.dimension().location(),
                    this.tracked.size());
        }

        this.tracked.clear();
    }

    // ---- Forge 事件 ----

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level)
        {
            of(level).tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            of(player.serverLevel()).sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            // 刚瞄的那条留在上一个维度了。
            AIMED.remove(player.getUUID());
            of(player.serverLevel()).sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            AIMED.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if (event.getLevel() instanceof ServerLevel level)
        {
            WhimRegistry registry = REGISTRIES.remove(level.dimension());

            if (registry != null)
            {
                registry.unload();
            }
        }
    }
}
