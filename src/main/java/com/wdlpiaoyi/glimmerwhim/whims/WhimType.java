package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface WhimType
{
    ResourceLocation id();

    WhimParams params();

    // 类型参数 + 内置 lifetime + visibility；后加者覆盖同名
    default WhimParams effectiveParams()
    {
        return params().plus(WhimParams.lifetime(defaultLifetime())).plus(WhimParams.visibility());
    }

    // -1 = 永久
    default int defaultLifetime()
    {
        return -1;
    }

    // 默认仅 ELEMENT 角色
    default Set<WhimRole> roles()
    {
        return EnumSet.of(WhimRole.ELEMENT);
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
        if (target == null || target.entity() == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(player.serverLevel().getEntities().get(target.entity()));
    }

    default boolean requiresLineOfSight()
    {
        return false;
    }

    default boolean occludedByBlocks(WhimData data)
    {
        return true;
    }

    default boolean occludedByEntities(WhimData data)
    {
        return false;
    }

    default boolean entityOcclusionRenderBox(WhimData data)
    {
        return false;
    }

    default boolean interactable()
    {
        return true;
    }

    // 可作链根 = 具 ELEMENT 角色
    default boolean canRoot()
    {
        return roles().contains(WhimRole.ELEMENT);
    }

    // 可入链 = interactable
    default boolean canChain()
    {
        return interactable();
    }

    // 真则被 HELD 时暂停寿命倒计时
    default boolean pausesWhileHeld()
    {
        return false;
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
