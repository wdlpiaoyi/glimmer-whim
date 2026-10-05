package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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
        WhimVisibility visibility = WhimVisibility.resolve(level.getServer(), player, raw == null ? WhimVisibility.DEFAULT_MODE : raw);
        Whim whim = new Whim(UUID.randomUUID(), placement.anchor(), type, data, visibility, level.getGameTime());
        WhimRegistry.of(level).summon(whim);

        return whim;
    }

    // 自然生成：抽取生成规则、校验生成点，召唤成功后再交给类型做生成表现；未声明规则或生成点不合法时返回 null
    public static Whim generate(ServerLevel level, ServerPlayer player, WhimType type)
    {
        WhimSpawnContext context = new WhimSpawnContext(level, player, player.getRandom());
        WhimSpawn placement = type.spawn(context).orElse(null);

        if (placement == null)
        {
            return null;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 at = placement.anchor().position(level, eye, 1.0F).orElse(null);

        // 生成点必须合法：被方块挡住就看不见也瞄不到
        if (at == null || WhimSight.occluded(level, eye, at, player, true, null, false))
        {
            return null;
        }

        Whim whim = summon(level, player, type, placement, WhimData.EMPTY);

        if (whim != null)
        {
            type.onGenerated(context, placement, whim);
        }

        return whim;
    }
}
