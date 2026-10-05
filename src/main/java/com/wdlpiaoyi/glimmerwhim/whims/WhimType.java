package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTargets;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTraces;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface WhimType
{
    ResourceLocation id();

    WhimParams params();

    // 类型参数 + 内置 lifetime/visibility/trace；后加者覆盖同名
    default WhimParams effectiveParams()
    {
        return params()
                .plus(WhimParams.lifetime(defaultLifetime()))
                .plus(WhimParams.visibility())
                .plus(WhimParams.traces(elementTrace().toString(), modifierTrace().toString()));
    }

    // 链的轨迹样式：element 决定整条链，modifier 叠在其上
    default ResourceLocation elementTrace()
    {
        return WhimTraces.LINE;
    }

    default ResourceLocation modifierTrace()
    {
        return WhimTraces.NONE;
    }

    // -1 = 永久
    default int defaultLifetime()
    {
        return -1;
    }

    // 角色能力：ELEMENT 由 canRoot 决定，MODIFIER 由 modifier(data) 是否有值决定
    default Set<WhimRole> roles(WhimData data)
    {
        EnumSet<WhimRole> roles = EnumSet.noneOf(WhimRole.class);

        if (canRoot())
        {
            roles.add(WhimRole.ELEMENT);
        }

        if (modifier(data).isPresent())
        {
            roles.add(WhimRole.MODIFIER);
        }

        return roles;
    }

    // 作为链修饰符时输出的数值；空 = 无
    default Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.empty();
    }

    // 根元素是否接受该交互目标
    default boolean acceptsTarget(WhimTarget target)
    {
        return true;
    }

    default Optional<Entity> resolveTarget(ServerPlayer player, WhimTarget target)
    {
        if (target == null)
        {
            return Optional.empty();
        }

        return WhimTargets.entity(target.data()).map(id -> player.serverLevel().getEntities().get(id));
    }

    default boolean requiresLineOfSight()
    {
        return false;
    }

    default boolean occludedByBlocks(WhimData data)
    {
        return true;
    }

    // 参与实体遮挡判定的实体；null = 实体不遮挡它
    default Predicate<Entity> entityOccluders(WhimData data)
    {
        return null;
    }

    default boolean entityOcclusionRenderBox(WhimData data)
    {
        return false;
    }

    default boolean interactable()
    {
        return true;
    }

    // 可作链根
    default boolean canRoot()
    {
        return true;
    }

    // 可入链 = interactable
    default boolean canChain()
    {
        return interactable();
    }

    // 真则进链（被按住或作为修饰）时暂停寿命倒计时；声明假才继续倒计时
    default boolean pausesInChain()
    {
        return true;
    }

    // 真则每 tick 收到 TICK 事件
    default boolean ticks()
    {
        return false;
    }

    // 事件回调；可调用 event.remove() 请求移除
    default void on(WhimEvent event)
    {
    }

    // 注册时的一次性初始化钩子
    default void bind()
    {
    }

    // 无显式锚时生成默认落点；空表示必须指定锚
    default Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        return Optional.empty();
    }
}
