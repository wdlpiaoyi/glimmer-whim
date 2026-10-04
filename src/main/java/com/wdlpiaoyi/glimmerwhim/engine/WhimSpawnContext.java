package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public record WhimSpawnContext(ServerLevel level, ServerPlayer player, RandomSource random)
{
    public Vec3 eye()
    {
        return this.player.getEyePosition();
    }

    public long gameTime()
    {
        return this.level.getGameTime();
    }

    public int nearby(double radius)
    {
        return WhimRegistry.of(this.level).countNear(this.player, radius);
    }

    public int nearby(double radius, WhimType type)
    {
        return WhimRegistry.of(this.level).countNear(this.player, radius, type);
    }

    public Vec3 randomAround(double min, double max)
    {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double distance = min + this.random.nextDouble() * (max - min);
        double offset = (this.random.nextDouble() - 0.5D) * (max - min);
        return new Vec3(this.player.getX() + Math.cos(angle) * distance, this.player.getEyeY() + offset,
                this.player.getZ() + Math.sin(angle) * distance);
    }

    public WhimData lifetime(int ticks)
    {
        return WhimData.of(Whim.LIFETIME, Integer.toString(ticks));
    }
}
