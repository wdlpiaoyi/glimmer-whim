package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WhimEvent
{
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
    private final WhimTarget target;
    private WhimRemoveReason removal;

    WhimEvent(Kind kind, ServerLevel level, Whim whim, ServerPlayer player, WhimTarget target)
    {
        this.kind = kind;
        this.level = level;
        this.whim = whim;
        this.player = player;
        this.target = target;
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
        return Optional.ofNullable(this.target);
    }

    public void remove(WhimRemoveReason reason)
    {
        this.removal = reason;
    }

    public Optional<WhimRemoveReason> removal()
    {
        return Optional.ofNullable(this.removal);
    }
}
