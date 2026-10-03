package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WhimRegistry
{
    private static final Map<ResourceKey<Level>, WhimRegistry> REGISTRIES = new HashMap<>();

    private static final Map<UUID, UUID> AIMED = new HashMap<>();

    private static final int SYNC_INTERVAL = 20;

    private record Tracked(Whim whim, WhimVisibility visibility)
    {
    }

    public record Found(ResourceKey<Level> dimension, Whim whim)
    {
    }

    private final ServerLevel level;
    private final Map<UUID, Tracked> tracked = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> sent = new HashMap<>();

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

    public void summon(Whim whim)
    {
        this.summon(whim, WhimVisibility.ALL);
    }

    public void summon(Whim whim, WhimVisibility visibility)
    {
        this.tracked.put(whim.id(), new Tracked(whim, visibility));

        GlimmerWhim.log("[Whim] summon id={} dim={} anchor={} element={} lifetime={} visibility={}",
                whim.id(), this.level.dimension().location(), whim.anchor().type(), whim.type().id(), whim.lifetime(),
                visibility == WhimVisibility.ALL ? "all" : "filtered");

        for (ServerPlayer player : this.level.players())
        {
            this.sync(player);
        }
    }

    public void removeWhim(UUID id, WhimRemoveReason reason)
    {
        if (this.tracked.remove(id) == null)
        {
            return;
        }

        GlimmerWhim.log("[Whim] remove id={} dim={} reason={}", id, this.level.dimension().location(), reason);

        AIMED.values().removeIf(id::equals);

        WhimRemovePacket packet = new WhimRemovePacket(this.level.dimension(), id, reason);

        for (Map.Entry<UUID, Set<UUID>> entry : this.sent.entrySet())
        {
            if (!entry.getValue().remove(id))
            {
                continue;
            }

            ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(entry.getKey());

            if (player != null)
            {
                WhimNetwork.sendTo(player, packet);
            }
        }
    }

    public void tick()
    {
        for (Whim whim : this.all())
        {
            if (whim.tick() == 0)
            {
                this.removeWhim(whim.id(), WhimRemoveReason.EXPIRED);
            }
            else
            {
                fire(WhimEvent.Kind.TICK, whim.id(), null);
            }
        }

        if (this.level.getGameTime() % SYNC_INTERVAL == 0)
        {
            for (ServerPlayer player : this.level.players())
            {
                this.sync(player);
            }
        }
    }

    private void sync(ServerPlayer player)
    {
        if (player.serverLevel() != this.level)
        {
            return;
        }

        Set<UUID> known = this.sent.computeIfAbsent(player.getUUID(), key -> new HashSet<>());
        Set<UUID> want = new HashSet<>();
        Vec3 eye = player.getEyePosition();
        double reach = loadDistance(player);

        for (Tracked entry : this.tracked.values())
        {
            Whim whim = entry.whim();

            if (!entry.visibility().canUse(player, whim))
            {
                continue;
            }

            Vec3 at = position(whim, this.level, eye);

            if (at == null || eye.distanceToSqr(at) > reach * reach)
            {
                continue;
            }

            want.add(whim.id());
        }

        for (UUID id : want)
        {
            if (known.add(id))
            {
                WhimNetwork.sendTo(player, WhimSummonPacket.of(this.level.dimension(), this.tracked.get(id).whim()));
            }
        }

        Iterator<UUID> gone = known.iterator();

        while (gone.hasNext())
        {
            UUID id = gone.next();

            if (!want.contains(id))
            {
                gone.remove();
                WhimNetwork.sendTo(player,
                        new WhimRemovePacket(this.level.dimension(), id, WhimRemoveReason.OUT_OF_RANGE));
            }
        }
    }

    private static Vec3 position(Whim whim, Level level, Vec3 eye)
    {
        return whim.anchor().position(level, eye, 1.0F, whim.data(), WhimAnchors.params(whim.anchor().type()))
                .orElse(null);
    }

    private static double loadDistance(ServerPlayer player)
    {
        return player.serverLevel().getServer().getPlayerList().getViewDistance() * 16.0D;
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

    private static void fire(WhimEvent.Kind kind, UUID id, ServerPlayer player)
    {
        for (WhimRegistry registry : REGISTRIES.values())
        {
            Tracked tracked = registry.tracked.get(id);

            if (tracked != null)
            {
                WhimEvent event = new WhimEvent(kind, registry.level, tracked.whim(), player);

                tracked.whim().anchor().on(event);

                event.removal().ifPresent(reason -> registry.removeWhim(id, reason));

                return;
            }
        }
    }

    public static void setAimed(ServerPlayer player, UUID id)
    {
        UUID before = AIMED.get(player.getUUID());

        if (id == null)
        {
            AIMED.remove(player.getUUID());
        }
        else
        {
            AIMED.put(player.getUUID(), id);
        }

        if (before != null && !before.equals(id))
        {
            fire(WhimEvent.Kind.UNHIGHLIGHT, before, player);
        }

        if (id != null && !id.equals(before))
        {
            fire(WhimEvent.Kind.HIGHLIGHT, id, player);
        }
    }

    public static UUID aimed(ServerPlayer player)
    {
        return AIMED.get(player.getUUID());
    }

    public static boolean use(ServerPlayer player, UUID id, boolean success)
    {
        WhimRegistry registry = of(player.serverLevel());
        Tracked tracked = registry.tracked.get(id);

        if (tracked == null)
        {
            return false;
        }

        if (!tracked.visibility().canUse(player, tracked.whim())
                || !registry.sent.getOrDefault(player.getUUID(), Set.of()).contains(id))
        {
            GlimmerWhim.log("[Whim] use rejected player={} id={}", player.getUUID(), id);
            return false;
        }

        GlimmerWhim.log("[Whim] use player={} id={} success={}", player.getUUID(), id, success);

        if (success)
        {
            fire(WhimEvent.Kind.USE, id, player);
        }
        else if (registry.tracked.containsKey(id))
        {
            registry.removeWhim(id, WhimRemoveReason.DROPPED);
        }

        return true;
    }

    private void unload()
    {
        if (!this.tracked.isEmpty())
        {
            GlimmerWhim.log("[Whim] unload dim={} dropped={}", this.level.dimension().location(),
                    this.tracked.size());
        }

        this.tracked.clear();
        this.sent.clear();
    }

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
            of(player.serverLevel()).sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            AIMED.remove(player.getUUID());
            forget(player);
            of(player.serverLevel()).sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            AIMED.remove(player.getUUID());
            forget(player);
        }
    }

    private static void forget(ServerPlayer player)
    {
        for (WhimRegistry registry : REGISTRIES.values())
        {
            registry.sent.remove(player.getUUID());
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
