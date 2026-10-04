package com.wdlpiaoyi.glimmerwhim.engine;

import net.minecraft.server.level.ServerPlayer;

public final class WhimReach
{
    public static final double BLOCKS_PER_CHUNK = 16.0D;

    private WhimReach()
    {
    }

    public static double blocks(int chunkDistance)
    {
        return chunkDistance * BLOCKS_PER_CHUNK;
    }

    public static double blocks(ServerPlayer player)
    {
        return blocks(player.serverLevel().getServer().getPlayerList().getViewDistance());
    }
}
