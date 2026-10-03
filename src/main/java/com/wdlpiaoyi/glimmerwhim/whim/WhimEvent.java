package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 引擎叫锚的那一声：出什么事了、跟谁有关，以及锚怎么回话。
 * <p>
 * 锚在 {@link WhimAnchor#on} 里看一眼 {@link #kind()}，想让它走就 {@link #remove}；
 * 引擎读完 {@link #removal()} 才决定要不要移。以后多一种事，只在这儿多一个 {@link Kind}、
 * 在事发的地方 {@code fire} 一下就行 —— 引擎和锚的接口都不用动。
 */
public final class WhimEvent
{
    /** 引擎现在会叫的那几声。 */
    public enum Kind
    {
        /** 有人开始瞄着它。 */
        HIGHLIGHT,
        /** 先前瞄着它的人不看它了。 */
        UNHIGHLIGHT,
        /** 过了一 tick。 */
        TICK
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

    /** 说的是哪条灵感。 */
    public Whim whim()
    {
        return this.whim;
    }

    /** 谁惹的事；{@link Kind#TICK} 这种没主的返回 null。 */
    public ServerPlayer player()
    {
        return this.player;
    }

    /** 锚回话：让它走，理由是 {@code reason}。 */
    public void remove(WhimRemoveReason reason)
    {
        this.removal = reason;
    }

    /** 锚回的话，空表示留着。 */
    public Optional<WhimRemoveReason> removal()
    {
        return Optional.ofNullable(this.removal);
    }
}
