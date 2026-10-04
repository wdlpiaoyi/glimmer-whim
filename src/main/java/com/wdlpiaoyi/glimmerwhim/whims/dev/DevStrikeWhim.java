package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.PosAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimLifecycle;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSight;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DevStrikeWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_strike");

    public static final DevStrikeWhim INSTANCE = new DevStrikeWhim();

    // 攻击触发落雷的概率
    private static final float CHANCE = 0.25F;
    // 生成点：水平前方 DISTANCE 格、地表以上 SKY_HEIGHT 格
    private static final double DISTANCE = 64.0D;
    private static final double SKY_HEIGHT = 16.0D;
    private static final int LIFETIME = 100;
    private static final int CHARGE_TICKS = 35;
    private static final int GLOW_TICKS = 200;
    // 足以秒杀绝大多数生物的一次伤害
    private static final float DAMAGE = 32768.0F;
    private static final int BOLTS = 5;
    private static final double BOLT_SPREAD = 2.0D;
    // 被攻击后保持可被锁定时长（tick）
    private static final long MEMORY_TICKS = 100L;

    // 玩家→(被攻击实体→失效游戏刻)，把落雷锁定到刚被该玩家打过的怪
    private static final Map<UUID, Map<UUID, Long>> ATTACKED = new HashMap<>();

    // 已召唤蓄力体、等待到点落雷的记录
    private static final List<Pending> PENDING = new ArrayList<>();

    private DevStrikeWhim()
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
        return WhimParams.of(WhimParam.choice("shape", "quad", "cube", "quad"), WhimParam.positiveNumber("size", "4"));
    }

    @Override
    public Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        // 锚定天空生成点；visibility=me 只对召唤者可见
        return Optional.of(new WhimSpawn(new PosAnchor(placement(context.player())),
                WhimData.of(Whim.LIFETIME, Integer.toString(LIFETIME)).with(Whim.VISIBILITY, "me")));
    }

    @Override
    public boolean requiresLineOfSight()
    {
        return true;
    }

    @Override
    public boolean pausesWhileHeld()
    {
        return true;
    }

    @Override
    public void bind()
    {
        // 注册本类上的 @SubscribeEvent（受击、服务端 tick）
        MinecraftForge.EVENT_BUS.register(getClass());
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() != WhimEvent.Kind.USE)
        {
            return;
        }

        ServerPlayer player = event.player();
        ServerLevel level = event.level();
        WhimTarget target = event.target().orElse(null);
        Entity found = resolveTarget(player, target).orElse(null);

        // 目标须是生物且刚被该玩家打过；否则告警音并放弃
        if (!(found instanceof LivingEntity living) || !attacked(player, living, level.getGameTime()))
        {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.REDSTONE_TORCH_BURNOUT,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            event.remove(WhimRemoveReason.DROPPED);
            return;
        }

        Vec3 at = event.whim().anchor().position(level, player.getEyePosition(), 1.0F).orElse(player.getEyePosition());
        // 在锚点召唤蓄力体，存活到落雷后再留 5 tick
        Whim charge = WhimLifecycle.summon(level, player, DevStrikeChargeWhim.ID,
                new WhimSpawn(new PosAnchor(at), WhimData.of(Whim.LIFETIME, Integer.toString(CHARGE_TICKS + 5))),
                WhimData.EMPTY);

        if (charge == null)
        {
            event.remove(WhimRemoveReason.DROPPED);
            return;
        }

        // 蓄力期间目标发光
        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
        // 用视距作为音效可听半径（方块）
        float range = level.getServer().getPlayerList().getViewDistance();
        level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, range, 1.0F);
        PENDING.add(new Pending(level, charge.id(), living.getUUID(), living.position(), CHARGE_TICKS));
        event.remove(WhimRemoveReason.USED);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event)
    {
        // 只认玩家直接造成的伤害
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer player))
        {
            return;
        }

        LivingEntity hurt = event.getEntity();
        long now = player.serverLevel().getGameTime();
        Map<UUID, Long> seen = ATTACKED.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
        // 清理过期记录并刷新该目标的可锁定窗口
        seen.entrySet().removeIf(entry -> entry.getValue() < now);
        seen.put(hurt.getUUID(), now + MEMORY_TICKS);

        // 概率未命中：只记录目标，不触发落雷
        if (player.getRandom().nextFloat() >= CHANCE)
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        WhimSpawn spawn = INSTANCE.spawn(new WhimSpawnContext(level, player, player.getRandom())).orElse(null);

        if (spawn == null)
        {
            return;
        }

        Vec3 at = spawn.anchor().position(level, eye, 1.0F).orElse(null);

        // 生成点被方块遮挡则不触发（忽略实体遮挡）
        if (at == null || WhimSight.occluded(level, eye, at, player, true, false, false))
        {
            return;
        }

        player.playNotifySound(SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.0F);
        WhimLifecycle.summon(level, player, INSTANCE, spawn, WhimData.EMPTY);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        // 只在服务端 tick 末尾推进
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty())
        {
            return;
        }

        Iterator<Pending> iterator = PENDING.iterator();

        while (iterator.hasNext())
        {
            Pending pending = iterator.next();
            Entity found = pending.level.getEntities().get(pending.entity);

            // 落点跟随目标当前位置
            if (found != null)
            {
                pending.point = found.position();
            }

            if (--pending.remaining > 0)
            {
                continue;
            }

            iterator.remove();
            strike(pending);
        }
    }

    private static void strike(Pending pending)
    {
        // 先移除蓄力体，再在落点周围随机撒 BOLTS 道闪电
        WhimRegistry.of(pending.level).removeWhim(pending.charge, WhimRemoveReason.USED);
        RandomSource random = pending.level.getRandom();

        for (int i = 0; i < BOLTS; i++)
        {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(pending.level);

            if (bolt == null)
            {
                continue;
            }

            double dx = (random.nextDouble() * 2.0D - 1.0D) * BOLT_SPREAD;
            double dz = (random.nextDouble() * 2.0D - 1.0D) * BOLT_SPREAD;
            bolt.moveTo(pending.point.add(dx, 0.0D, dz));
            pending.level.addFreshEntity(bolt);
        }

        Entity found = pending.level.getEntities().get(pending.entity);

        if (found instanceof LivingEntity living)
        {
            living.hurt(living.damageSources().generic(), DAMAGE);
        }
    }

    private static Vec3 placement(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);

        // 垂直俯仰时水平投影退化，用 +Z 兜底
        if (flat.lengthSqr() < 1.0E-6D)
        {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }

        Vec3 base = eye.add(flat.normalize().scale(DISTANCE));
        double surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(base.x), Mth.floor(base.z));

        // 高度取地形表面与眼高的较大值，再抬 SKY_HEIGHT 格
        return new Vec3(base.x, Math.max(surface, eye.y) + SKY_HEIGHT, base.z);
    }

    // 无记忆视为可锁定；有记忆时须包含该实体
    private static boolean attacked(ServerPlayer player, LivingEntity entity, long now)
    {
        Map<UUID, Long> seen = ATTACKED.get(player.getUUID());

        if (seen == null)
        {
            return true;
        }

        seen.entrySet().removeIf(entry -> entry.getValue() < now);

        if (seen.isEmpty())
        {
            return true;
        }

        return seen.containsKey(entity.getUUID());
    }

    // 一次待落雷：蓄力体 id、目标实体 id、跟踪落点、剩余 tick
    private static final class Pending
    {
        private final ServerLevel level;
        private final UUID charge;
        private final UUID entity;
        private Vec3 point;
        private int remaining;

        private Pending(ServerLevel level, UUID charge, UUID entity, Vec3 point, int remaining)
        {
            this.level = level;
            this.charge = charge;
            this.entity = entity;
            this.point = point;
            this.remaining = remaining;
        }
    }
}
