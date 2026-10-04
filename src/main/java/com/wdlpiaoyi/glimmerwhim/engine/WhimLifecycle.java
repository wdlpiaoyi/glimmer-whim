package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WhimLifecycle
{
    private WhimLifecycle()
    {
    }

    // 未注册类型返回 null
    public static Whim summon(ServerLevel level, ServerPlayer player, ResourceLocation typeId, WhimSpawn placement,
            WhimData overrides)
    {
        WhimType type = Whims.get(typeId);

        return type == null ? null : summon(level, player, type, placement, overrides);
    }

    public static Whim summon(ServerLevel level, ServerPlayer player, WhimType type, WhimSpawn placement,
            WhimData overrides)
    {
        Map<String, String> values = new LinkedHashMap<>(placement.data().values());

        // overrides 覆盖锚点自带参数
        for (Map.Entry<String, String> entry : overrides.values().entrySet())
        {
            values.put(entry.getKey(), entry.getValue());
        }

        // visibility 是元数据，取出后不留在 data
        String raw = values.remove(Whim.VISIBILITY);
        String lifetime = values.get(Whim.LIFETIME);

        // 缺省取类型默认寿命；仅非负值写回 data，-1（永久）不落盘
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
