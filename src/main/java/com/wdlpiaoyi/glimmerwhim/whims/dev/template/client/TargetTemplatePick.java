package com.wdlpiaoyi.glimmerwhim.whims.dev.template.client;

import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.client.WhimTargeters;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.world.phys.Vec3;

// 目标产生器扩展点模板：客户端按优先级把视线解析成 WhimTarget，服务端再用对应的
// WhimTargets.Kind 校验。真实内容通常写成 public static final Kind 的服务端种类 + 这里一个产生器。
// 本类自身不注册；登记是 WhimTargeters.register(priority, producer)。
public final class TargetTemplatePick
{
    // 示例：视线前方固定距离处永远给一个点目标
    private static final double DISTANCE = 4.0D;

    private TargetTemplatePick()
    {
    }

    // 返回 empty 表示本产生器放弃，交给下一个优先级；点目标即内置 point 种类
    public static Optional<WhimTarget> pick(WhimTargeters.Context context)
    {
        Vec3 point = context.eye().add(context.look().scale(DISTANCE));
        return Optional.of(WhimTarget.ofPoint(point));
    }

    // 优先级数值越大越先命中；内置灵感瞄准为 100、视线命中为 0
    public static void register()
    {
        WhimTargeters.register(50, TargetTemplatePick::pick);
    }
}
