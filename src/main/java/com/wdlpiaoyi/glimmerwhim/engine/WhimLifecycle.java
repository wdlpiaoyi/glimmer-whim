package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WhimLifecycle
{
    private WhimLifecycle()
    {
    }

    public static Whim summon(ServerLevel level, ServerPlayer player, WhimType type, WhimSpawn placement,
            WhimData overrides)
    {
        Map<String, String> values = new LinkedHashMap<>(placement.data().values());

        for (Map.Entry<String, String> entry : overrides.values().entrySet())
        {
            values.put(entry.getKey(), entry.getValue());
        }

        String raw = values.remove(Whim.VISIBILITY);
        String lifetime = values.get(Whim.LIFETIME);

        if (lifetime == null)
        {
            lifetime = Integer.toString(type.defaultLifetime());
        }

        int ticks = Integer.parseInt(lifetime);

        if (ticks >= 0)
        {
            values.put(Whim.LIFETIME, Integer.toString(ticks));
        }

        WhimData data = WhimData.of(values);
        WhimVisibility visibility = WhimVisibility.resolve(level.getServer(), player, raw == null ? "all" : raw);
        Whim whim = new Whim(UUID.randomUUID(), placement.anchor(), type, data, visibility);
        WhimRegistry.of(level).summon(whim);

        return whim;
    }
}
