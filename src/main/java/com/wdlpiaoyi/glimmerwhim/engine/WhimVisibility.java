package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WhimVisibility
{
    public static final WhimVisibility ALL = new WhimVisibility(null);

    private final UUID player;

    public WhimVisibility(UUID player)
    {
        this.player = player;
    }

    public static WhimVisibility resolve(MinecraftServer server, ServerPlayer player, String raw)
    {
        if (raw.equals("all"))
        {
            return ALL;
        }

        if (raw.equals("me"))
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

    public boolean canUse(ServerPlayer player)
    {
        return this.player == null || this.player.equals(player.getUUID());
    }

    public String describe()
    {
        return this.player == null ? "all" : this.player.toString();
    }
}
