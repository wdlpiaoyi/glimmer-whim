package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WhimSpawner
{
    private WhimSpawner()
    {
    }

    public static void tick(ServerLevel level)
    {
        if (!WhimConfig.spawnEnabled())
        {
            return;
        }

        long gameTime = level.getGameTime();
        WhimRegistry registry = WhimRegistry.of(level);

        for (ServerPlayer player : level.players())
        {
            WhimSpawnContext context = new WhimSpawnContext(level, player, level.random);

            for (WhimType type : Whims.types())
            {
                if (gameTime % Math.max(1, type.spawnInterval()) != 0)
                {
                    continue;
                }

                type.spawn(context).ifPresent(
                        spawn -> registry.summon(new Whim(UUID.randomUUID(), spawn.anchor(), type, spawn.data())));
            }
        }
    }
}
