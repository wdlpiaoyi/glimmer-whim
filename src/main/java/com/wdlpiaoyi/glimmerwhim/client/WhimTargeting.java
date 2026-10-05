package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

// 内置目标产生器：灵感优先于视线命中
public final class WhimTargeting
{
    private WhimTargeting()
    {
    }

    public static void register()
    {
        WhimTargeters.register(100, WhimTargeting::aimed);
        WhimTargeters.register(0, WhimTargeting::crosshair);
    }

    // 准星停在「按下之后才进入」的灵感上时以它为目标；链根须先离开再回到
    private static Optional<WhimTarget> aimed(WhimTargeters.Context context)
    {
        UUID aimed = WhimAim.aimed();
        UUID whim = aimed != null && (!aimed.equals(context.root()) || context.left()) ? aimed : null;

        if (whim == null)
        {
            return Optional.empty();
        }

        ClientWhimCache.WhimView view = ClientWhimCache.view(whim);

        if (view == null)
        {
            return Optional.empty();
        }

        return view.anchor().position(context.level(), context.eye(), 1.0F).map(at -> WhimTarget.ofWhim(whim, at));
    }

    // 视线命中：先做方块裁剪，再在裁剪范围内找可拾取实体；实体优先于方块
    private static Optional<WhimTarget> crosshair(WhimTargeters.Context context)
    {
        Entity source = context.player();
        Vec3 end = context.eye().add(context.look().scale(WhimReach.blocks()));
        BlockHitResult block = context.level().clip(
                new ClipContext(context.eye(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        // 以裁剪终点为界扩展包围盒并膨胀 1 格，沿用原版投射物拾取范围
        AABB bounds = source.getBoundingBox().expandTowards(limit.subtract(context.eye())).inflate(1.0D);
        EntityPick pick = pick(context, source, limit, bounds);

        if (pick != null)
        {
            return Optional.of(WhimTarget.ofEntity(pick.entity().getUUID(), pick.location()));
        }

        if (block.getType() != HitResult.Type.MISS)
        {
            return Optional.of(WhimTarget.ofPoint(block.getLocation()));
        }

        return Optional.empty();
    }

    // 沿视线在候选范围内取最近的实体；命中箱按几何中心放大，好瞄
    private static EntityPick pick(WhimTargeters.Context context, Entity source, Vec3 limit, AABB bounds)
    {
        EntityPick best = null;
        double nearest = Double.MAX_VALUE;

        for (Entity candidate : context.level().getEntities(source, bounds,
                entity -> entity != source && !entity.isSpectator() && entity.isPickable()))
        {
            Vec3 hit = scaled(candidate).clip(context.eye(), limit).orElse(null);

            if (hit == null)
            {
                continue;
            }

            double distance = context.eye().distanceToSqr(hit);

            if (distance < nearest)
            {
                nearest = distance;
                best = new EntityPick(unwrap(candidate), hit);
            }
        }

        return best;
    }

    private static AABB scaled(Entity entity)
    {
        AABB box = entity.getBoundingBox();
        double grow = (WhimConfig.targetHitboxScale() - 1.0D) / 2.0D;
        return box.inflate(box.getXsize() * grow, box.getYsize() * grow, box.getZsize() * grow);
    }

    // 多部件实体的部位归到本体（例如末影龙的各段），否则目标会是一个非生物的部件
    private static Entity unwrap(Entity entity)
    {
        if (entity instanceof PartEntity<?> part)
        {
            Entity parent = part.getParent();

            if (parent != null)
            {
                return parent;
            }
        }

        return entity;
    }

    private record EntityPick(Entity entity, Vec3 location)
    {
    }
}
