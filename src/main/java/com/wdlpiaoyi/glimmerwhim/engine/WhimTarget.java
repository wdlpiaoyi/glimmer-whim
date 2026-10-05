package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.world.phys.Vec3;

// 交互目标：种类 + 通用 payload；payload 的含义由种类自己定义
public record WhimTarget(WhimTargets.Kind kind, WhimData data)
{
    public static WhimTarget ofPoint(Vec3 point)
    {
        return new WhimTarget(WhimTargets.POINT, WhimTargets.position(point));
    }

    public static WhimTarget ofEntity(UUID entity, Vec3 point)
    {
        return new WhimTarget(WhimTargets.ENTITY,
                WhimTargets.position(point).with(WhimTargets.ENTITY_KEY, entity.toString()));
    }

    // 以某个灵感本身为目标；point 仅供射程校验与轨迹终点
    public static WhimTarget ofWhim(UUID whim, Vec3 point)
    {
        return new WhimTarget(WhimTargets.WHIM,
                WhimTargets.position(point).with(WhimTargets.WHIM_KEY, whim.toString()));
    }

    public Optional<Vec3> point()
    {
        return this.kind == null ? Optional.empty() : this.kind.position().apply(this.data);
    }

    // 日志/回显用的简短描述
    public String describe()
    {
        return this.kind == null ? "unregistered" : this.kind.describe().apply(this.data);
    }
}
