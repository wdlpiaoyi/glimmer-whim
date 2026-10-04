package com.wdlpiaoyi.glimmerwhim.engine;

import net.minecraft.server.level.ServerPlayer;

public final class WhimReach
{
    // 1 区块 = 16 方块
    public static final double BLOCKS_PER_CHUNK = 16.0D;

    private WhimReach()
    {
    }

    // 区块距离换算为方块
    public static double blocks(int chunkDistance)
    {
        return chunkDistance * BLOCKS_PER_CHUNK;
    }

    // 交互/可见半径 = 服务端视距（区块）×16
    public static double blocks(ServerPlayer player)
    {
        return blocks(player.serverLevel().getServer().getPlayerList().getViewDistance());
    }
}
