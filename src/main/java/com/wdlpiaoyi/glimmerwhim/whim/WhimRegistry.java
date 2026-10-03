package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSpawnPacket;

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
 * {@link #removeWhim} 是唯一的出口。调用它的只有四个地方：tick 到点、施法结算、松手、维度卸载。
 */
public final class WhimRegistry
{
    private static final Map<ResourceKey<Level>, WhimRegistry> REGISTRIES = new HashMap<>();

    private record Tracked(Whim whim, WhimVisibility visibility)
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
    public void spawn(Whim whim)
    {
        this.spawn(whim, WhimVisibility.ALL);
    }

    /** 出现，只发给能用它的人。 */
    public void spawn(Whim whim, WhimVisibility visibility)
    {
        this.tracked.put(whim.id(), new Tracked(whim, visibility));

        GlimmerWhim.LOGGER.info("[Whim] spawn id={} dim={} anchor={} element={} lifetime={} visibility={}",
                whim.id(), this.level.dimension().location(), whim.anchor().type(), whim.element(), whim.lifetime(),
                visibility == WhimVisibility.ALL ? "all" : "filtered");

        WhimSpawnPacket packet = WhimSpawnPacket.of(this.level.dimension(), whim);

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

        WhimRemovePacket packet = new WhimRemovePacket(this.level.dimension(), id, reason);

        for (ServerPlayer player : this.level.players())
        {
            WhimNetwork.sendTo(player, packet);
        }
    }

    /** 过一 tick：谁的 lifetime 见底了，谁走。 */
    public void tick()
    {
        List<UUID> expired = new ArrayList<>();

        for (Tracked entry : this.tracked.values())
        {
            if (entry.whim().tick() <= 0)
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
                WhimNetwork.sendTo(player, WhimSpawnPacket.of(this.level.dimension(), entry.whim()));
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
            of(player.serverLevel()).sendSnapshot(player);
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
