package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.UUID;

// 调度句柄：cancel() 后不再执行
public final class WhimTask
{
    private final UUID owner; // 所属灵感，null = 全局
    private final int interval; // 0 = 单次
    private final Runnable action;
    private long executeAt;
    private boolean cancelled;

    WhimTask(UUID owner, int interval, long executeAt, Runnable action)
    {
        this.owner = owner;
        this.interval = interval;
        this.executeAt = executeAt;
        this.action = action;
    }

    UUID owner()
    {
        return this.owner;
    }

    long executeAt()
    {
        return this.executeAt;
    }

    boolean repeating()
    {
        return this.interval > 0;
    }

    void advance()
    {
        this.executeAt += this.interval;
    }

    boolean cancelled()
    {
        return this.cancelled;
    }

    void run()
    {
        this.action.run();
    }

    public void cancel()
    {
        this.cancelled = true;
    }
}
