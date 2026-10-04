package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WhimEvent
{
    // 事件来源：瞄准边沿高亮/取消、存活 tick、使用链、按住
    public enum Kind
    {
        HIGHLIGHT,
        UNHIGHLIGHT,
        TICK,
        USE,
        HOLD
    }

    private final Kind kind;
    private final ServerLevel level;
    private final Whim whim;
    private final ServerPlayer player;
    private final WhimChain chain;
    private WhimRemoveReason removal; // on() 中调用 remove() 登记，消费方据此移除

    WhimEvent(Kind kind, ServerLevel level, Whim whim, ServerPlayer player, WhimChain chain)
    {
        this.kind = kind;
        this.level = level;
        this.whim = whim;
        this.player = player;
        this.chain = chain;
    }

    public Kind kind()
    {
        return this.kind;
    }

    public ServerLevel level()
    {
        return this.level;
    }

    public Whim whim()
    {
        return this.whim;
    }

    public ServerPlayer player()
    {
        return this.player;
    }

    public Optional<WhimTarget> target()
    {
        return this.chain == null ? Optional.empty() : Optional.ofNullable(this.chain.target());
    }

    public Optional<WhimChain> chain()
    {
        return Optional.ofNullable(this.chain);
    }

    // 由 on() 调用，请求移除当前灵感
    public void remove(WhimRemoveReason reason)
    {
        this.removal = reason;
    }

    public Optional<WhimRemoveReason> removal()
    {
        return Optional.ofNullable(this.removal);
    }
}
