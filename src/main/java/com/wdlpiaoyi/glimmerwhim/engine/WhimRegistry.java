package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;

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
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.net.WhimRemovePacket;
import com.wdlpiaoyi.glimmerwhim.net.WhimSummonPacket;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

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
    // 每维度一张登记表；维度卸载时移除
    private static final Map<ResourceKey<Level>, WhimRegistry> REGISTRIES = new HashMap<>();

    // 玩家 -> 瞄准的灵感，仅服务端权威，用于 HIGHLIGHT 边沿
    private static final Map<UUID, UUID> AIMED = new HashMap<>();

    // 玩家 -> 按住的灵感，HOLD 状态由它维持
    private static final Map<UUID, UUID> HELD = new HashMap<>();

    // 每 20 tick（1 秒）全量重算一次可见性
    private static final int SYNC_INTERVAL = 20;

    public record Found(ResourceKey<Level> dimension, Whim whim)
    {
    }

    private final ServerLevel level;
    // 本维度存活灵感，按召唤顺序
    private final Map<UUID, Whim> tracked = new LinkedHashMap<>();
    // 玩家 UUID -> 已下发召唤包的灵感集，用于增删同步
    private final Map<UUID, Set<UUID>> sent = new HashMap<>();

    private WhimRegistry(ServerLevel level)
    {
        this.level = level;
    }

    // 懒建；同维度共享同一实例
    public static WhimRegistry of(ServerLevel level)
    {
        return REGISTRIES.computeIfAbsent(level.dimension(), key -> new WhimRegistry(level));
    }

    public ServerLevel level()
    {
        return this.level;
    }

    // 入表后立即向在场玩家同步，不等周期
    public void summon(Whim whim)
    {
        this.tracked.put(whim.id(), whim);

        GlimmerWhim.log("[Whim] summon id={} dim={} anchor={} element={} lifetime={} visibility={}",
                whim.id(), this.level.dimension().location(), whim.anchor().type(), whim.type().id(), whim.lifetime(),
                whim.visibility().describe());

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

        this.announceRemoval(id, reason);
    }

    // 清理瞄准/按住记录，只给曾收到过该灵感的玩家发移除包
    private void announceRemoval(UUID id, WhimRemoveReason reason)
    {
        GlimmerWhim.log("[Whim] remove id={} dim={} reason={}", id, this.level.dimension().location(), reason);

        AIMED.values().removeIf(id::equals);
        HELD.values().removeIf(id::equals);

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

    // 先扣寿命再派发 TICK；被按住且类型声明暂停时跳过倒计时
    public void tick()
    {
        Iterator<Map.Entry<UUID, Whim>> iterator = this.tracked.entrySet().iterator();

        while (iterator.hasNext())
        {
            Map.Entry<UUID, Whim> entry = iterator.next();
            Whim whim = entry.getValue();
            boolean held = HELD.containsValue(entry.getKey()) && whim.type().pausesWhileHeld();

            // 寿命归零按 EXPIRED 移除
            if (!held && whim.tick() == 0)
            {
                iterator.remove();
                this.announceRemoval(entry.getKey(), WhimRemoveReason.EXPIRED);
                continue;
            }

            WhimEvent event = new WhimEvent(WhimEvent.Kind.TICK, this.level, whim, null, null);
            whim.type().on(event);

            if (event.removal().isPresent())
            {
                iterator.remove();
                this.announceRemoval(entry.getKey(), event.removal().get());
            }
        }

        // 周期性对在场玩家重算可见性
        if (this.level.getGameTime() % SYNC_INTERVAL == 0)
        {
            for (ServerPlayer player : this.level.players())
            {
                this.sync(player);
            }
        }
    }

    // 计算玩家应可见的灵感：归属可见 + 锚点可解析 + 在 reach 内
    private void sync(ServerPlayer player)
    {
        if (player.serverLevel() != this.level)
        {
            return;
        }

        Set<UUID> known = this.sent.computeIfAbsent(player.getUUID(), key -> new HashSet<>());
        Set<UUID> want = new HashSet<>();
        List<UUID> tooFar = new ArrayList<>();
        Vec3 eye = player.getEyePosition();
        double reach = WhimReach.blocks(player);

        for (Whim entry : this.tracked.values())
        {
            Whim whim = entry;

            if (!entry.visibility().canUse(player))
            {
                continue;
            }

            Vec3 at = position(whim, this.level, eye);

            if (at == null)
            {
                continue;
            }

            if (eye.distanceToSqr(at) > reach * reach)
            {
                tooFar.add(whim.id());
                continue;
            }

            want.add(whim.id());
        }

        for (UUID id : want)
        {
            if (known.add(id))
            {
                WhimNetwork.sendTo(player, WhimSummonPacket.of(this.level.dimension(), this.tracked.get(id)));
            }
        }

        // 仅当该维度内再无其他可见玩家在 reach 内，才真正移除
        for (UUID id : tooFar)
        {
            if (!this.anyPlayerInRange(id))
            {
                this.removeWhim(id, WhimRemoveReason.OUT_OF_RANGE);
            }
        }

        // 本轮不可见的灵感只对这名玩家发移除包，不删服务端实体
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

    public int countNear(ServerPlayer player, double radius)
    {
        return countNear(player, radius, null);
    }

    // 以眼位为心统计半径内灵感；type 非空时只数该类型
    public int countNear(ServerPlayer player, double radius, WhimType type)
    {
        Vec3 eye = player.getEyePosition();
        double square = radius * radius;
        int count = 0;

        for (Whim entry : this.tracked.values())
        {
            if (type != null && entry.type() != type)
            {
                continue;
            }

            Vec3 at = position(entry, this.level, eye);

            if (at != null && at.distanceToSqr(eye) <= square)
            {
                count++;
            }
        }

        return count;
    }

    // partialTick=1.0 取当前刻坐标；锚点无法解析时为 null
    private static Vec3 position(Whim whim, Level level, Vec3 eye)
    {
        return whim.anchor().position(level, eye, 1.0F).orElse(null);
    }

    // 任一可用玩家在该灵感 reach 内即返回真
    private boolean anyPlayerInRange(UUID id)
    {
        Whim entry = this.tracked.get(id);

        if (entry == null)
        {
            return false;
        }

        for (ServerPlayer player : this.level.players())
        {
            if (!entry.visibility().canUse(player))
            {
                continue;
            }

            Vec3 eye = player.getEyePosition();
            Vec3 at = position(entry, this.level, eye);
            double reach = WhimReach.blocks(player);

            if (at != null && eye.distanceToSqr(at) <= reach * reach)
            {
                return true;
            }
        }

        return false;
    }

    public Collection<Whim> all()
    {
        List<Whim> whimes = new ArrayList<>(this.tracked.size());

        for (Whim entry : this.tracked.values())
        {
            whimes.add(entry);
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
            Whim tracked = entry.getValue().tracked.get(id);

            if (tracked != null)
            {
                return Optional.of(new Found(entry.getKey(), tracked));
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

    // 单灵感事件（无链）；on() 登记了移除原因就执行
    private static void fire(WhimEvent.Kind kind, UUID id, ServerPlayer player)
    {
        for (WhimRegistry registry : REGISTRIES.values())
        {
            Whim tracked = registry.tracked.get(id);

            if (tracked != null)
            {
                WhimEvent event = new WhimEvent(kind, registry.level, tracked, player, null);

                tracked.type().on(event);

                event.removal().ifPresent(reason -> registry.removeWhim(id, reason));

                return;
            }
        }
    }

    // 链使用事件交给根元素；默认移除原因 USED，整条链一并移除
    private static void fireChain(WhimRegistry registry, WhimChain chain, ServerPlayer player)
    {
        WhimEvent event = new WhimEvent(WhimEvent.Kind.USE, registry.level, chain.root(), player, chain);

        chain.root().type().on(event);

        WhimRemoveReason reason = event.removal().orElse(WhimRemoveReason.USED);

        for (Whim whim : chain.order())
        {
            registry.removeWhim(whim.id(), reason);
        }
    }

    // 目标不可见/超距/被遮挡时视为未瞄准；仅在变化时发高亮边沿
    public static void setAimed(ServerPlayer player, UUID id)
    {
        if (id != null && (!visible(player, id) || !withinSight(player, of(player.serverLevel()).tracked.get(id))))
        {
            id = null;
        }

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

    // 可见 = 归属允许 + 已下发过（sent）
    private static boolean visible(ServerPlayer player, UUID id)
    {
        WhimRegistry registry = of(player.serverLevel());
        Whim tracked = registry.tracked.get(id);

        return tracked != null
                && tracked.visibility().canUse(player)
                && registry.sent.getOrDefault(player.getUUID(), Set.of()).contains(id);
    }

    // 在 reach 内，且类型要求时通过方块/实体遮挡检测
    private static boolean withinSight(ServerPlayer player, Whim whim)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 at = whim.anchor().position(player.serverLevel(), eye, 1.0F).orElse(null);

        if (at == null)
        {
            return false;
        }

        double reach = WhimReach.blocks(player);

        if (eye.distanceToSqr(at) > reach * reach)
        {
            return false;
        }

        return !whim.type().requiresLineOfSight() || !WhimSight.occluded(player.serverLevel(), eye, at, player,
                whim.type().occludedByBlocks(whim.data()), whim.type().occludedByEntities(whim.data()),
                whim.type().entityOcclusionRenderBox(whim.data()));
    }

    // 按客户端顺序解析链：须已下发、可链；根须 canRoot，长度截到 maxChainLength
    public static boolean use(ServerPlayer player, List<UUID> chain, WhimTarget target)
    {
        if (chain == null || chain.isEmpty())
        {
            return false;
        }

        WhimRegistry registry = of(player.serverLevel());
        Set<UUID> sent = registry.sent.getOrDefault(player.getUUID(), Set.of());
        int limit = Math.max(1, WhimConfig.maxChainLength());
        List<Whim> resolved = new ArrayList<>();

        for (UUID id : chain)
        {
            Whim tracked = registry.tracked.get(id);

            if (tracked == null || !tracked.visibility().canUse(player) || !sent.contains(id)
                    || !tracked.type().canChain())
            {
                // 根节点无效直接拒绝；链中后续无效节点跳过
                if (resolved.isEmpty())
                {
                    GlimmerWhim.log("[Whim] chain rejected player={} id={}", player.getUUID(), id);
                    return false;
                }

                continue;
            }

            if (resolved.isEmpty() && !tracked.type().canRoot())
            {
                GlimmerWhim.log("[Whim] chain rejected player={} id={} roles={}", player.getUUID(), id,
                        tracked.type().roles());
                return false;
            }

            if (resolved.size() >= limit)
            {
                break;
            }

            resolved.add(tracked);
        }

        if (resolved.isEmpty())
        {
            return false;
        }

        WhimChain built = new WhimChain(resolved, target);

        GlimmerWhim.log("[Whim] use player={} chain={} target={}", player.getUUID(),
                resolved.stream().map(whim -> whim.type().id().toString()).toList(),
                target == null ? "none" : target.describe());

        // 目标无效则整链按 DROPPED 丢弃（仍算一次使用）
        if (target == null || !validTarget(player, target) || !built.root().type().acceptsTarget(target))
        {
            for (Whim whim : resolved)
            {
                registry.removeWhim(whim.id(), WhimRemoveReason.DROPPED);
            }

            return true;
        }

        fireChain(registry, built, player);
        return true;
    }

    // 清空客户端已构建的链（未使用）；逐一按 DROPPED 移除
    public static boolean voidChain(ServerPlayer player, List<UUID> chain)
    {
        if (chain == null || chain.isEmpty())
        {
            return false;
        }

        WhimRegistry registry = of(player.serverLevel());
        Set<UUID> sent = registry.sent.getOrDefault(player.getUUID(), Set.of());

        for (UUID id : chain)
        {
            Whim tracked = registry.tracked.get(id);

            if (tracked == null || !tracked.visibility().canUse(player) || !sent.contains(id)
                    || !tracked.type().canChain())
            {
                continue;
            }

            GlimmerWhim.log("[Whim] void player={} id={}", player.getUUID(), id);
            registry.removeWhim(id, WhimRemoveReason.DROPPED);
        }

        return true;
    }

    // 命中点须在 reach 内；带实体时须仍存在于该维度
    private static boolean validTarget(ServerPlayer player, WhimTarget target)
    {
        if (target.point() == null)
        {
            return false;
        }

        double reach = WhimReach.blocks(player);

        if (player.getEyePosition().distanceToSqr(target.point()) > reach * reach)
        {
            return false;
        }

        if (target.entity() != null)
        {
            return player.serverLevel().getEntities().get(target.entity()) != null;
        }

        return true;
    }

    // 按住要求可见且在视距/无遮挡内；成功回传 HOLD
    public static boolean hold(ServerPlayer player, UUID id)
    {
        WhimRegistry registry = of(player.serverLevel());
        Whim tracked = registry.tracked.get(id);

        if (tracked == null)
        {
            return false;
        }

        if (!tracked.visibility().canUse(player)
                || !registry.sent.getOrDefault(player.getUUID(), Set.of()).contains(id)
                || !withinSight(player, tracked))
        {
            GlimmerWhim.log("[Whim] hold rejected player={} id={}", player.getUUID(), id);
            return false;
        }

        GlimmerWhim.log("[Whim] hold player={} id={}", player.getUUID(), id);

        HELD.put(player.getUUID(), id);
        fire(WhimEvent.Kind.HOLD, id, player);
        return true;
    }

    // 维度卸载丢弃本表全部状态
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
    // 仅 END 阶段，服务端每 tick 驱动本维度
    public static void onLevelTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level)
        {
            of(level).tick();
        }
    }

    @SubscribeEvent
    // 登录补发当前可见灵感
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            of(player.serverLevel()).sync(player);
        }
    }

    @SubscribeEvent
    // 换维度：清瞄准与各维度 sent，再对新维度重算
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
    // 登出清瞄准与 sent，避免残留
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            AIMED.remove(player.getUUID());
            forget(player);
        }
    }

    // 清 HELD 与所有维度中该玩家的 sent
    private static void forget(ServerPlayer player)
    {
        HELD.remove(player.getUUID());

        for (WhimRegistry registry : REGISTRIES.values())
        {
            registry.sent.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event)
    {
        // 维度卸载移除登记表并丢弃其状态
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
