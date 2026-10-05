package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimHits;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimShapes;
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

    // 类型参数 + 内置 lifetime/visibility/trace/shape/hit；后加者覆盖同名
    default WhimParams effectiveParams()
    {
        return params()
                .plus(WhimParams.lifetime(defaultLifetime()))
                .plus(WhimParams.visibility())
                .plus(WhimParams.traces(elementTrace().toString(), modifierTrace().toString()))
                .plus(WhimParams.appearance(defaultShape().toString(), defaultHit().toString()));
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

    // 本体的具名外观；{shape} 覆盖它
    default ResourceLocation defaultShape()
    {
        return WhimShapes.QUAD;
    }

    // 本体的具名命中体积；{hit} 覆盖它
    default ResourceLocation defaultHit()
    {
        return WhimHits.QUAD;
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

    // 绘制时是否开深度测试：让方块与实体都能挡住它（深度缓冲区分不了两者）；默认沿用实体遮挡声明
    default boolean depthOcclusion(WhimData data)
    {
        return entityOccluders(data) != null;
    }

    // 是否可被瞄准/交互；数据可带状态（如进入某状态后不可再交互）
    default boolean interactable(WhimData data)
    {
        return true;
    }

    // 可作链根
    default boolean canRoot()
    {
        return true;
    }

    // 可入链 = interactable
    default boolean canChain(WhimData data)
    {
        return interactable(data);
    }

    // 真则进链（被按住或作为修饰）时暂停寿命倒计时；声明假才继续倒计时
    default boolean pausesInChain()
    {
        return true;
    }

    // 真则随链一并消耗；声明假的应用需自行移除（例如转入下一状态的本体）
    default boolean consumedOnUse(WhimData data)
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

    // 自然生成后的表现（音效、粒子等）；只由生成管线调用，指令召唤不走这里
    default void onGenerated(WhimSpawnContext context, WhimSpawn placement, Whim whim)
    {
    }

    // 消失后在客户端演多久的消散（毫秒）；0 = 直接消失。画法由渲染登记里的消散样式决定
    default int vanishMillis(WhimData data)
    {
        return 0;
    }
}
