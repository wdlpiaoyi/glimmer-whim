package com.wdlpiaoyi.glimmerwhim.whim;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface WhimVisibility
{
    WhimVisibility ALL = (player, whim) -> true;

    boolean canUse(ServerPlayer player, Whim whim);
}
