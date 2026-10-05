package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WhimVisibility
{
    // 可见性词汇的唯一来源：补全候选、参数校验与解析都从这里取
    public static final String ALL_MODE = "all";

    public static final String ME_MODE = "me";

    public static final String DEFAULT_MODE = ALL_MODE;

    public static final List<String> MODES = List.of(ALL_MODE, ME_MODE);

    public static final String HINT = "all、me、玩家名或 UUID";

    // 玩家名形状（离线服也适用）
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    // player==null 表示对所有玩家可见可用
    public static final WhimVisibility ALL = new WhimVisibility(null);

    private final UUID player;

    public WhimVisibility(UUID player)
    {
        this.player = player;
    }

    // 解析模式名 / 玩家名 / UUID；找不到则报错
    public static WhimVisibility resolve(MinecraftServer server, ServerPlayer player, String raw)
    {
        if (raw.equals(ALL_MODE))
        {
            return ALL;
        }

        if (raw.equals(ME_MODE))
        {
            return new WhimVisibility(player.getUUID());
        }

        ServerPlayer target = server.getPlayerList().getPlayerByName(raw);

        if (target != null)
        {
            return new WhimVisibility(target.getUUID());
        }

        try
        {
            return new WhimVisibility(UUID.fromString(raw));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("找不到玩家: " + raw);
        }
    }

    // 值是否有可能解析成功：模式名、玩家名形状或 UUID。是否真的存在只在服务端 resolve 时判断
    public static boolean valid(String raw)
    {
        if (MODES.contains(raw) || NAME.matcher(raw).matches())
        {
            return true;
        }

        try
        {
            UUID.fromString(raw);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    // 无归属时任何玩家可用，否则仅限该 UUID
    public boolean canUse(ServerPlayer player)
    {
        return this.player == null || this.player.equals(player.getUUID());
    }

    public String describe()
    {
        return this.player == null ? ALL_MODE : this.player.toString();
    }
}
