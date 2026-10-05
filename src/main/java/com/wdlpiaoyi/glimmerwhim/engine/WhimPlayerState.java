package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

// 按玩家记录的临时状态（键 -> 到期 tick）。用服务器的全局 tick 计数而不是某个维度的游戏刻，跨维度一致；
// 不落盘，登出整份丢弃，取用时顺手剔除过期项。
public final class WhimPlayerState
{
    private static final Map<UUID, Map<String, Long>> STATES = new HashMap<>();

    private WhimPlayerState()
    {
    }

    public static void mark(ServerPlayer player, String key, int ticks)
    {
        if (ticks > 0)
        {
            STATES.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                    .put(key, (long) player.getServer().getTickCount() + ticks);
        }
    }

    public static boolean active(ServerPlayer player, String key)
    {
        return remaining(player, key) > 0L;
    }

    public static long remaining(ServerPlayer player, String key)
    {
        Map<String, Long> state = STATES.get(player.getUUID());

        if (state == null)
        {
            return 0L;
        }

        Long expiry = state.get(key);

        if (expiry == null)
        {
            return 0L;
        }

        long left = expiry - player.getServer().getTickCount();

        if (left <= 0L)
        {
            state.remove(key);

            if (state.isEmpty())
            {
                STATES.remove(player.getUUID());
            }

            return 0L;
        }

        return left;
    }

    public static void forget(UUID player)
    {
        STATES.remove(player);
    }
}
