// parked：已移出构建。恢复到 src 前需按当前引擎 API 校对（如 modid 用 GlimmerWhim.MODID；WhimChain 已无 value()/factor()；周期生成器 spawnInterval 已删除）。
package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.anchor.PosAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class SpawnTestWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "spawn_test");

    public static final SpawnTestWhim INSTANCE = new SpawnTestWhim();

    private static final int LIFETIME = 400;
    private static final int CAP = 6;
    private static final double CAP_RADIUS = 40.0D;

    private SpawnTestWhim()
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
        return WhimParams.NONE;
    }

    @Override
    public int spawnInterval()
    {
        return 100;
    }

    @Override
    public Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        if (context.nearby(CAP_RADIUS, this) >= CAP)
        {
            return Optional.empty();
        }

        Vec3 at = context.randomAround(6.0D, 18.0D);
        return Optional.of(new WhimSpawn(new PosAnchor(at), context.lifetime(LIFETIME)));
    }
}
