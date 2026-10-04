package com.wdlpiaoyi.glimmerwhim.client;

import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhimTargeting
{
    private WhimTargeting()
    {
    }

    // 先做方块裁剪，再在裁剪范围内找可拾取实体；实体优先于方块
    public static WhimTarget pick(ClientLevel level, Entity source, Vec3 eye, Vec3 look)
    {
        double reach = WhimReach.blocks();
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult block = level.clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        // 以裁剪终点为界扩展包围盒并膨胀 1 格，沿用原版投射物拾取范围
        AABB bounds = source.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1.0D);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, source, eye, limit, bounds,
                candidate -> candidate != source && !candidate.isSpectator() && candidate.isPickable());

        if (entity != null)
        {
            return new WhimTarget(entity.getEntity().getUUID(), entity.getLocation());
        }

        if (block.getType() != HitResult.Type.MISS)
        {
            return new WhimTarget(null, block.getLocation());
        }

        return null;
    }
}
