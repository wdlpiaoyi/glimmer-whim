package com.wdlpiaoyi.glimmerwhim.whim;

import net.minecraft.server.level.ServerPlayer;

/**
 * 谁能用这条灵感。
 * <p>
 * 用不了的连显示都没有 —— 过滤发生在同步层，发都不发给他，不是发过去再让他客户端装看不见。
 * 默认所有人，可选字段，所以它不在 {@link Whim} 里，走注册表的重载。
 */
@FunctionalInterface
public interface WhimVisibility
{
    WhimVisibility ALL = (player, whim) -> true;

    boolean canUse(ServerPlayer player, Whim whim);
}
