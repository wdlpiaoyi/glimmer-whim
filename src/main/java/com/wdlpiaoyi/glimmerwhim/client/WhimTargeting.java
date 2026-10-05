package com.wdlpiaoyi.glimmerwhim.client;

import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(context.level(), source, context.eye(), limit, bounds,
                candidate -> candidate != source && !candidate.isSpectator() && candidate.isPickable());

        if (entity != null)
        {
            return Optional.of(WhimTarget.ofEntity(entity.getEntity().getUUID(), entity.getLocation()));
        }

        if (block.getType() != HitResult.Type.MISS)
        {
            return Optional.of(WhimTarget.ofPoint(block.getLocation()));
        }

        return Optional.empty();
    }
}
