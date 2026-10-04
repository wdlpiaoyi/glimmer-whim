package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public record WhimSpawnContext(ServerLevel level, ServerPlayer player, RandomSource random)
{
    // 以玩家眼位统计半径内同维灵感数，供生成条件
    public int nearby(double radius)
    {
        return WhimRegistry.of(this.level).countNear(this.player, radius);
    }

    public int nearby(double radius, WhimType type)
    {
        return WhimRegistry.of(this.level).countNear(this.player, radius, type);
    }

    // 玩家周围环形采样：角度均匀、距离 [min,max)、垂直在眼高附近抖动
    public Vec3 randomAround(double min, double max)
    {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double distance = min + this.random.nextDouble() * (max - min);
        double offset = (this.random.nextDouble() - 0.5D) * (max - min);
        return new Vec3(this.player.getX() + Math.cos(angle) * distance, this.player.getEyeY() + offset,
                this.player.getZ() + Math.sin(angle) * distance);
    }

    // 构造只含 lifetime 的参数集
    public WhimData lifetime(int ticks)
    {
        return WhimData.of(Whim.LIFETIME, Integer.toString(ticks));
    }
}
