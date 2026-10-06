package com.wdlpiaoyi.glimmerwhim.whims.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.RayAnchor;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimLifecycle;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimPlayerState;
import com.wdlpiaoyi.glimmerwhim.engine.WhimReach;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimScheduler;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTargets;
import com.wdlpiaoyi.glimmerwhim.engine.WhimVisibility;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

// 落雷：对过于强大的目标近战命中时（这一击相对其最大生命微不足道）按概率在固定方向的高空生成；拖到生物后本体进入蓄力态，蓄力结束时在目标处落下纯视觉闪电并造成范围伤害
public final class StrikeWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "strike");
    public static final ResourceLocation DAMAGE_TYPE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "strike");
    public static final StrikeWhim INSTANCE = new StrikeWhim();

    public static final String SIZE = "size";
    public static final String DAMAGE_RATIO = "damage_ratio";
    private static final String RADIUS = "radius";
    private static final String BOLTS = "bolts";
    private static final String SPREAD = "spread";
    private static final String CHARGE_TICKS = "charge_ticks";
    private static final String GLOW_TICKS = "glow_ticks";

    // 本体状态：蓄力中不可再交互，落雷时由落雷流程移除
    public static final String CHARGING = "charging";

    // 玩家侧临时状态（engine/WhimPlayerState），单位 tick；战斗状态由 DAMAGE 的起点推出来，不单独存
    private static final String DAMAGE = "strike_damage";
    private static final String COOLDOWN = "strike_cooldown";
    private static final String ROLL = "strike_roll";

    // 生成：相对视线水平的 ±45°，仰角 20°~70°，距离不超过加载距离减余量
    private static final double HORIZONTAL_SPREAD_DEGREES = 45.0D;
    private static final double MIN_ELEVATION_DEGREES = 20.0D;
    private static final double MAX_ELEVATION_DEGREES = 70.0D;
    private static final double DISTANCE = 128.0D;
    private static final double DISTANCE_MARGIN = 32.0D;
    private static final double MIN_DISTANCE = 16.0D;
    // 击退强度（按衰减比例缩放）
    private static final double KNOCKBACK = 0.8D;
    // 消失后碎片散开的时长（毫秒）
    private static final int VANISH_MILLIS = 550;
    // 生成数据的浮动幅度：寿命、尺寸与伤害比例在基准值上下浮动，避免每次生成完全相同
    private static final double LIFETIME_SPREAD = 0.2D;
    private static final double SIZE_SPREAD = 0.15D;
    private static final double DAMAGE_SPREAD = 0.25D;

    private StrikeWhim()
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
        return WhimParams.of(
                WhimParam.positiveNumber(SIZE, Double.toString(WhimConfig.strikeSize())),
                // 默认值来自 [strike] damage_ratio，单次召唤可用 {damage_ratio} 覆盖
                WhimParam.positiveNumber(DAMAGE_RATIO, Double.toString(WhimConfig.strikeDamageRatio())),
                WhimParam.positiveNumber(RADIUS, Double.toString(WhimConfig.strikeRadius())),
                // 默认道数随 {damage_ratio} 推算；显式写 {bolts} 才以它为准
                WhimParam.positiveNumber(BOLTS, Integer.toString(boltCount(WhimConfig.strikeDamageRatio()))),
                WhimParam.positiveNumber(SPREAD, Double.toString(WhimConfig.strikeSpread())),
                WhimParam.positiveNumber(CHARGE_TICKS, Integer.toString(WhimConfig.strikeChargeTicks())),
                WhimParam.positiveNumber(GLOW_TICKS, Integer.toString(WhimConfig.strikeGlowTicks())));
    }

    @Override
    public ResourceLocation elementTrace()
    {
        return ContentTraces.CURRENT;
    }

    // 本体画成光效、命中体积取球
    @Override
    public ResourceLocation defaultShape()
    {
        return ContentShapes.GLOW;
    }

    @Override
    public ResourceLocation defaultHit()
    {
        return ContentHits.GLOW;
    }

    @Override
    public int defaultLifetime()
    {
        return WhimConfig.strikeCooldownTicks();
    }

    @Override
    public boolean requiresLineOfSight()
    {
        return true;
    }

    // 天上的面片要被方块与实体挡住
    @Override
    public boolean depthOcclusion(WhimData data)
    {
        return true;
    }

    // 消失时炸成碎片
    @Override
    public int vanishMillis(WhimData data)
    {
        return VANISH_MILLIS;
    }

    // 蓄力体不再可瞄/可链；消耗则取决于是否真的进了蓄力态
    @Override
    public boolean interactable(WhimData data)
    {
        return data.get(CHARGING).isEmpty();
    }

    @Override
    public boolean consumedOnUse(WhimData data)
    {
        return data.get(CHARGING).isEmpty();
    }

    @Override
    public boolean acceptsTarget(WhimTarget target)
    {
        return WhimTargets.ENTITY.equals(target.kind());
    }

    @Override
    public Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        return Optional.of(roll(context.player(), context.random()));
    }

    // 自然生成的表现：在灵感实际生成点放 wither 环境音，只发给本人
    @Override
    public void onGenerated(WhimSpawnContext context, WhimSpawn placement, Whim whim)
    {
        ServerPlayer player = context.player();
        Vec3 at = placement.anchor().position(context.level(), player.getEyePosition(), 1.0F).orElse(null);

        if (at == null)
        {
            return;
        }

        player.connection.send(new ClientboundSoundPacket(
                ForgeRegistries.SOUND_EVENTS.getDelegateOrThrow(SoundEvents.WITHER_AMBIENT), SoundSource.HOSTILE,
                at.x, at.y, at.z, soundRange(context.level()), 1.0F, player.getRandom().nextLong()));
    }

    // 抽一次生成：方向、距离与数据一起定下
    private static WhimSpawn roll(ServerPlayer player, RandomSource random)
    {
        Vec3 look = player.getLookAngle();
        double yaw = Math.atan2(look.z, look.x)
                + Math.toRadians((random.nextDouble() * 2.0D - 1.0D) * HORIZONTAL_SPREAD_DEGREES);
        double elevation = Math.toRadians(
                MIN_ELEVATION_DEGREES + random.nextDouble() * (MAX_ELEVATION_DEGREES - MIN_ELEVATION_DEGREES));
        double distance = Math.max(MIN_DISTANCE, Math.min(DISTANCE, WhimReach.blocks(player) - DISTANCE_MARGIN));
        Vec3 direction = new Vec3(Math.cos(yaw) * Math.cos(elevation), Math.sin(elevation),
                Math.sin(yaw) * Math.cos(elevation));
        int cooldown = WhimConfig.strikeCooldownTicks();
        double size = WhimConfig.strikeSize();
        double damage = WhimConfig.strikeDamageRatio();
        // 区间值由 WhimLifecycle 在成形时抽定：寿命、尺寸与伤害比例每次生成都略有不同
        WhimData data = WhimData.of(Whim.VISIBILITY, WhimVisibility.ME_MODE)
                .with(Whim.LIFETIME, WhimData.range((int) Math.round(cooldown * (1.0D - LIFETIME_SPREAD)),
                        (int) Math.round(cooldown * (1.0D + LIFETIME_SPREAD))))
                .with(SIZE, WhimData.range(size * (1.0D - SIZE_SPREAD), size * (1.0D + SIZE_SPREAD)))
                .with(DAMAGE_RATIO, WhimData.range(damage * (1.0D - DAMAGE_SPREAD), damage * (1.0D + DAMAGE_SPREAD)));

        // ray 锚：位置 = 眼睛 + 方向 × 距离，用来表示固定方向上的远处物体；方向生成时固定
        return new WhimSpawn(new RayAnchor(direction, distance), data);
    }

    // 音效传播半径（格 = 16 × 返回的音量）；音量只影响传播距离，客户端最终增益仍按距离衰减
    private static float soundRange(ServerLevel level)
    {
        return level.getServer().getPlayerList().getViewDistance();
    }

    // 目标不对（没选、选了方块、选的不是生物）时的失败反馈
    private static void burnout(ServerPlayer player)
    {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // 表现强度：{damage_ratio} 越大，光环越多越亮、闪电越多；对数刻度，比例 1 → 约 1/3，7 以上封顶
    public static double intensity(double ratio)
    {
        return Math.max(0.0D, Math.min(1.0D, Math.log(ratio + 1.0D) / Math.log(8.0D)));
    }

    // 默认闪电道数
    public static int boltCount(double ratio)
    {
        return (int) Math.round(4.0D + 16.0D * intensity(ratio));
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.player() == null)
        {
            return;
        }

        // 引擎在丢弃整链前发来 REJECT：目标为空或不被接受
        if (event.kind() == WhimEvent.Kind.REJECT)
        {
            burnout(event.player());
            return;
        }

        if (event.kind() != WhimEvent.Kind.USE)
        {
            return;
        }

        ServerPlayer player = event.player();
        Entity resolved = resolveTarget(player, event.target().orElse(null)).orElse(null);
        LivingEntity target = resolved instanceof LivingEntity living ? living : null;

        if (target == null)
        {
            burnout(player);
            event.remove(WhimRemoveReason.DROPPED);
            return;
        }

        Whim body = event.whim();
        ServerLevel level = player.serverLevel();
        WhimData charging = body.data().with(CHARGING, "true");

        // 进入蓄力态：本体停止倒计时并公开，蓄力期间所有人可见可闻
        WhimRegistry.update(body.id(), charging, WhimVisibility.ALL);
        // 蓄力中不让本体自然到期：否则落雷任务会被「本体移除即取消」连带取消
        WhimRegistry.of(level).freeze(body.id());
        // 蓄力音在目标处，视距内所有人可闻；音量只放大传播半径，否则远处目标听不见
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_SONIC_CHARGE,
                SoundSource.HOSTILE, soundRange(level), 1.0F);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) params().number(charging, GLOW_TICKS), 0, false,
                false, false));

        WhimParams params = params();
        float base = (float) (target.getMaxHealth() * params.number(charging, DAMAGE_RATIO));
        int charge = Math.max(1, (int) params.number(charging, CHARGE_TICKS));
        // 闪电道数默认随伤害比例推算，{bolts} 显式给出时以它为准
        int bolts = charging.get(BOLTS).isPresent()
                ? (int) params.number(charging, BOLTS)
                : boltCount(params.number(charging, DAMAGE_RATIO));
        // 目标身份与落点在此固定：目标还在就跟着它，消失了就打记录点
        WhimScheduler.schedule(level, body.id(), charge,
                () -> strike(level, player, body.id(), target.getUUID(), target.position(), base,
                        params.number(charging, RADIUS), bolts, params.number(charging, SPREAD)));
    }

    // 落雷：本体消失，落点附近按原版爆炸衰减统一结算伤害与击退
    private static void strike(ServerLevel level, ServerPlayer player, UUID body, UUID targetId, Vec3 point, float base,
            double radius, int bolts, double spread)
    {
        WhimRegistry.of(level).removeWhim(body, WhimRemoveReason.USED);

        LivingEntity main = level.getEntities().get(targetId) instanceof LivingEntity living ? living : null;
        Vec3 center = main == null ? point : main.position();

        // 纯视觉闪电：只做表现，伤害不再走原版闪电（避免引火与无归属伤害）
        for (int i = 0; i < bolts; i++)
        {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);

            if (bolt == null)
            {
                continue;
            }

            bolt.moveTo(center.x + (level.random.nextDouble() * 2.0D - 1.0D) * spread, center.y,
                    center.z + (level.random.nextDouble() * 2.0D - 1.0D) * spread);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        DamageSource source = damageSource(level, player);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), entity -> entity != player && entity.isAlive());

        for (LivingEntity victim : victims)
        {
            double distance = Math.sqrt(victim.distanceToSqr(center));
            // 曝光度与衰减照原版爆炸：g 先乘可见比例，再取 (g^2 + g) / 2
            double g = (1.0D - distance / (radius * 2.0D)) * Explosion.getSeenPercent(center, victim);

            if (g <= 0.0D)
            {
                continue;
            }

            double attenuation = (g * g + g) / 2.0D;
            victim.hurt(source, (float) (base * attenuation));

            Vec3 away = new Vec3(victim.getX() - center.x, victim.getEyeY() - center.y, victim.getZ() - center.z);
            double length = away.length();

            if (length > 0.0D)
            {
                Vec3 push = away.scale(attenuation * KNOCKBACK / length);
                victim.push(push.x, push.y, push.z);
            }
        }
    }

    // 自定义伤害类型：不带任何 BYPASSES_* 标签，因此照常受护甲、保护、抗性与无敌帧影响
    private static DamageSource damageSource(ServerLevel level, ServerPlayer player)
    {
        Registry<DamageType> types = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> type = types.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, DAMAGE_TYPE));

        return new DamageSource(type, player, player);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event)
    {
        if (!WhimConfig.strikeEnabled())
        {
            return;
        }

        // 有源伤害（来源带实体）给双方刷「造成过伤害」标记；造成与受到都算
        if (event.getSource().getEntity() != null)
        {
            markDamage(event.getEntity());
            markDamage(event.getSource().getEntity());
        }

        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer player))
        {
            return;
        }

        LivingEntity target = event.getEntity();

        // 必须先处于战斗状态：从本场第一击起连续刷新该标记满 combat_window_ticks（本次命中只续标记，自己不开门）
        if (!inCombat(player))
        {
            return;
        }

        // 只认「对目标而言微不足道」的一击：单次伤害不超过目标最大生命的一定比例
        if (event.getAmount() > target.getMaxHealth() * WhimConfig.strikeMaxDamageRatio())
        {
            return;
        }

        if (target.getMaxHealth() < WhimConfig.strikeMinHealth())
        {
            return;
        }

        ServerLevel level = player.serverLevel();

        if (WhimConfig.strikeOverworldOnly() && level.dimension() != Level.OVERWORLD)
        {
            return;
        }

        // 抽签间隔与生成冷却分开：命中后压 1 秒再抽签，抽中则另压一段本体寿命的冷却
        if (WhimPlayerState.active(player, COOLDOWN) || WhimPlayerState.active(player, ROLL))
        {
            return;
        }

        WhimPlayerState.mark(player, ROLL, WhimConfig.strikeRollIntervalTicks());

        if (player.getRandom().nextFloat() >= WhimConfig.strikeChance())
        {
            return;
        }

        // 生成规则、生成点合法性与生成表现都交给生成管线（表现走 strike 自己的 onGenerated）
        Whim summoned = WhimLifecycle.generate(level, player, INSTANCE);

        if (summoned != null)
        {
            WhimPlayerState.mark(player, COOLDOWN, Math.max(1, summoned.lifetime(level.getGameTime())));
        }
    }

    private static void markDamage(Entity entity)
    {
        if (entity instanceof ServerPlayer player)
        {
            WhimPlayerState.mark(player, DAMAGE, WhimConfig.strikeDamageWindowTicks());
        }
    }

    // 战斗状态：从本场第一击起，该标记连续保持满 combat_window_ticks；标记一断（停手超过 damage_window_ticks）即结束
    private static boolean inCombat(ServerPlayer player)
    {
        long since = WhimPlayerState.since(player, DAMAGE);

        return since > 0L && player.getServer().getTickCount() - since >= WhimConfig.strikeCombatWindowTicks();
    }
}
