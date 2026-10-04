package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public final class WhimVisibility
{
    public static final WhimVisibility ALL = new WhimVisibility(null);

    private final UUID player;

    public WhimVisibility(UUID player)
    {
        this.player = player;
    }

    public boolean canUse(ServerPlayer player)
    {
        return this.player == null || this.player.equals(player.getUUID());
    }
}
