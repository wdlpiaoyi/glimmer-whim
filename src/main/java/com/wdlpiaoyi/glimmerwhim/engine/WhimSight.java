package com.wdlpiaoyi.glimmerwhim.engine;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhimSight
{
    private WhimSight()
    {
    }

    public static boolean occluded(Level level, Vec3 eye, Vec3 at, Entity entity)
    {
        BlockHitResult hit = level.clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                entity));

        return hit.getType() != HitResult.Type.MISS;
    }
}
