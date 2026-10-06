package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

// 按玩家记录的临时状态（键 -> 标记）。用服务器的全局 tick 计数而不是某个维度的游戏刻，跨维度一致；
// 不落盘，登出整份丢弃，取用时顺手剔除过期项。
// 标记记下起点与到期：未过期时刷新只延长到期时刻、起点不变，于是能判断「已经连续保持了多久」。
public final class WhimPlayerState
{
    // since = 本轮起点 tick；expiry = 到期 tick
    private record Marker(long since, long expiry)
    {
    }

    private static final Map<UUID, Map<String, Marker>> STATES = new HashMap<>();

    private WhimPlayerState()
    {
    }

    // 同名标记尚未过期时只续期，起点保持不变
    public static void mark(ServerPlayer player, String key, int ticks)
    {
        if (ticks <= 0)
        {
            return;
        }

        long now = player.getServer().getTickCount();
        Map<String, Marker> state = STATES.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Marker previous = state.get(key);
        long since = previous != null && previous.expiry() > now ? previous.since() : now;

        state.put(key, new Marker(since, now + ticks));
    }

    public static boolean active(ServerPlayer player, String key)
    {
        return remaining(player, key) > 0L;
    }

    // 本轮起点 tick；没有标记或已过期返回 0
    public static long since(ServerPlayer player, String key)
    {
        Marker marker = live(player, key);

        return marker == null ? 0L : marker.since();
    }

    public static long remaining(ServerPlayer player, String key)
    {
        Marker marker = live(player, key);

        return marker == null ? 0L : marker.expiry() - player.getServer().getTickCount();
    }

    public static void forget(UUID player)
    {
        STATES.remove(player);
    }

    // 取未过期的标记，顺手清掉过期项；没有则返回 null
    private static Marker live(ServerPlayer player, String key)
    {
        Map<String, Marker> state = STATES.get(player.getUUID());

        if (state == null)
        {
            return null;
        }

        Marker marker = state.get(key);

        if (marker == null)
        {
            return null;
        }

        if (marker.expiry() <= player.getServer().getTickCount())
        {
            state.remove(key);

            if (state.isEmpty())
            {
                STATES.remove(player.getUUID());
            }

            return null;
        }

        return marker;
    }
}
