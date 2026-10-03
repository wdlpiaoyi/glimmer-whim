package com.wdlpiaoyi.glimmerwhim.whim;

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
        USE
    }

    private final Kind kind;
    private final ServerLevel level;
    private final Whim whim;
    private final ServerPlayer player;
    private WhimRemoveReason removal;

    WhimEvent(Kind kind, ServerLevel level, Whim whim, ServerPlayer player)
    {
        this.kind = kind;
        this.level = level;
        this.whim = whim;
        this.player = player;
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

    public void remove(WhimRemoveReason reason)
    {
        this.removal = reason;
    }

    public Optional<WhimRemoveReason> removal()
    {
        return Optional.ofNullable(this.removal);
    }
}
