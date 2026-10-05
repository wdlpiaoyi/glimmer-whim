package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WhimEvent
{
    // 事件来源：生成/销毁、瞄准边沿高亮/取消、存活 tick、按住、使用链、使用被拒、寿命到点
    public enum Kind
    {
        SUMMON,
        HIGHLIGHT,
        UNHIGHLIGHT,
        TICK,
        HOLD,
        USE,
        REJECT,
        EXPIRE,
        REMOVE
    }

    private final Kind kind;
    private final ServerLevel level;
    private final Whim whim;
    private final ServerPlayer player;
    private final WhimChain chain;
    private final WhimRemoveReason reason; // 仅 REMOVE：实际移除原因
    private WhimRemoveReason removal; // on() 中调用 remove() 登记，消费方据此移除

    WhimEvent(Kind kind, ServerLevel level, Whim whim, ServerPlayer player, WhimChain chain)
    {
        this(kind, level, whim, player, chain, null);
    }

    WhimEvent(Kind kind, ServerLevel level, Whim whim, ServerPlayer player, WhimChain chain, WhimRemoveReason reason)
    {
        this.kind = kind;
        this.level = level;
        this.whim = whim;
        this.player = player;
        this.chain = chain;
        this.reason = reason;
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

    public WhimRemoveReason reason()
    {
        return this.reason;
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

    // 以本灵感为 owner 延迟执行；灵感移除即自动取消
    public WhimTask schedule(int delayTicks, Runnable action)
    {
        return WhimScheduler.schedule(this.level, this.whim == null ? null : this.whim.id(), delayTicks, action);
    }

    // 以本灵感为 owner 周期执行
    public WhimTask scheduleRepeating(int intervalTicks, Runnable action)
    {
        return WhimScheduler.scheduleRepeating(this.level, this.whim == null ? null : this.whim.id(), intervalTicks,
                action);
    }
}
