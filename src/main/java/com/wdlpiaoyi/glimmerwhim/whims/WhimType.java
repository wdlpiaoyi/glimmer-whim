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

    default WhimParams effectiveParams()
    {
        return params().plus(WhimParams.lifetime(defaultLifetime())).plus(WhimParams.visibility());
    }

    default int defaultLifetime()
    {
        return -1;
    }

    default Set<WhimRole> roles()
    {
        return EnumSet.of(WhimRole.ELEMENT);
    }

    default Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.empty();
    }

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

    default boolean interactable()
    {
        return true;
    }

    default boolean canRoot()
    {
        return roles().contains(WhimRole.ELEMENT);
    }

    default boolean canChain()
    {
        return interactable();
    }

    default boolean pausesWhileHeld()
    {
        return false;
    }

    default void on(WhimEvent event)
    {
    }

    default Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        return Optional.empty();
    }
}
