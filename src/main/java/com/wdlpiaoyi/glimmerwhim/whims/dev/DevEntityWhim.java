package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.EntityAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimLifecycle;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimReach;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTargets;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// 用望远镜盯住同一实体满一秒后，在它的碰撞箱中心生成
public final class DevEntityWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_entity");

    public static final DevEntityWhim INSTANCE = new DevEntityWhim();

    // 数据里记下锚定的实体，供使用时回显
    private static final String ENTITY = "entity";
    private static final int STARE_TICKS = 20;
    private static final int LIFETIME = 100;

    private static final Map<UUID, Stare> STARES = new HashMap<>();

    private DevEntityWhim()
    {
    }

    @Override
    public ResourceLocation id()
    {
        return ID;
    }

    @Override
    public WhimParams params()
    {
        // 默认画成面向视线的棋盘格面片
        return WhimParams.of(WhimParam.choice("shape", "quad", "cube", "quad"));
    }

    @Override
    public boolean acceptsTarget(WhimTarget target)
    {
        return target != null && WhimTargets.WHIM.equals(target.kind());
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() != WhimEvent.Kind.USE || event.whim() == null)
        {
            return;
        }

        WhimTarget target = event.target().orElse(null);
        UUID anchored = event.whim().data().get(ENTITY).map(DevEntityWhim::parse).orElse(null);
        UUID aimed = target == null ? null : WhimTargets.whim(target.data()).orElse(null);
        boolean self = aimed != null && event.whim().id().equals(aimed);

        if (!self || anchored == null)
        {
            event.remove(WhimRemoveReason.DROPPED);
            return;
        }

        if (event.player() != null)
        {
            event.player().sendSystemMessage(Component.literal("dev_entity: " + anchored));
        }

        event.remove(WhimRemoveReason.USED);
    }

    @SubscribeEvent
    static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
        {
            return;
        }

        Entity looked = player.isScoping() ? look(player) : null;

        if (looked == null)
        {
            STARES.remove(player.getUUID());
            return;
        }

        Stare stare = STARES.get(player.getUUID());

        // 换了目标从头计时；盯满一秒只生成一次
        if (stare == null || !stare.entity.equals(looked.getUUID()))
        {
            STARES.put(player.getUUID(), new Stare(looked.getUUID()));
            return;
        }

        if (!stare.spawned && ++stare.ticks >= STARE_TICKS)
        {
            stare.spawned = true;
            spawn(player, looked);
        }
    }

    private static void spawn(ServerPlayer player, Entity entity)
    {
        WhimData data = WhimData.of(Whim.LIFETIME, Integer.toString(LIFETIME))
                .with(ENTITY, entity.getUUID().toString());

        WhimLifecycle.summon(player.serverLevel(), player, INSTANCE,
                new WhimSpawn(new EntityAnchor(entity.getUUID(), entity.getId()), data), WhimData.EMPTY);
    }

    // 望远镜准星下的实体：先做方块裁剪，再在裁剪范围内找可拾取实体
    private static Entity look(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(WhimReach.blocks(player)));
        BlockHitResult block = level.clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB bounds = player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1.0D);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, player, eye, limit, bounds,
                candidate -> candidate != player && !candidate.isSpectator() && candidate.isPickable());

        return entity == null ? null : entity.getEntity();
    }

    private static UUID parse(String text)
    {
        try
        {
            return UUID.fromString(text);
        }
        catch (IllegalArgumentException exception)
        {
            return null;
        }
    }

    private static final class Stare
    {
        private final UUID entity;
        private int ticks;
        private boolean spawned;

        private Stare(UUID entity)
        {
            this.entity = entity;
        }
    }
}
