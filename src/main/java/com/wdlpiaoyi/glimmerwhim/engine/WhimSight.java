package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhimSight
{
    // 步进上限，防止在透明方块处死循环
    private static final int MAX_BLOCK_STEPS = 64;
    // 命中后沿方向前进的量，用于越过当前方块
    private static final double STEP = 0.01D;

    private WhimSight()
    {
    }

    // 以灵感中心点 at 为终点向 eye 做一条射线判定遮挡。
    // 方块：命中后仅当 canOcclude() 为真才算（玻璃等透明方块不算）。
    // 实体：命中攻击碰撞箱 getBoundingBox()；renderBox 为真时改用渲染剔除框 getBoundingBoxForCulling()。
    public static boolean occluded(Level level, Vec3 eye, Vec3 at, Entity viewer,
            boolean blocks, boolean entities, boolean renderBox)
    {
        return blocks && blockedByBlock(level, eye, at, viewer)
                || entities && blockedByEntity(level, eye, at, viewer, renderBox);
    }

    private static boolean blockedByBlock(Level level, Vec3 eye, Vec3 at, Entity viewer)
    {
        Vec3 direction = at.subtract(eye);
        double lengthSqr = direction.lengthSqr();

        if (lengthSqr < 1.0E-6D)
        {
            return false;
        }

        Vec3 step = direction.scale(1.0D / Math.sqrt(lengthSqr));
        Vec3 from = eye;

        for (int i = 0; i < MAX_BLOCK_STEPS; i++)
        {
            BlockHitResult hit = level.clip(new ClipContext(from, at, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, viewer));

            if (hit.getType() == HitResult.Type.MISS)
            {
                return false;
            }

            if (level.getBlockState(hit.getBlockPos()).canOcclude())
            {
                return true;
            }

            from = hit.getLocation().add(step.scale(STEP));

            if (from.subtract(eye).lengthSqr() >= lengthSqr)
            {
                return false;
            }
        }

        return false;
    }

    private static boolean blockedByEntity(Level level, Vec3 eye, Vec3 at, Entity viewer, boolean renderBox)
    {
        AABB bounds = new AABB(eye, at).inflate(1.0D);

        for (Entity candidate : level.getEntities(viewer, bounds, entity -> entity instanceof LivingEntity))
        {
            AABB box = renderBox ? candidate.getBoundingBoxForCulling() : candidate.getBoundingBox();
            Optional<Vec3> hit = box.clip(eye, at);

            if (hit.isPresent())
            {
                return true;
            }
        }

        return false;
    }
}
