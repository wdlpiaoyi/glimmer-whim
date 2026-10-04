package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;

// 每维度任务表：按游戏刻到点执行；owner 灵感移除即取消
public final class WhimScheduler
{
    private final List<WhimTask> tasks = new ArrayList<>();

    public static WhimTask schedule(ServerLevel level, UUID owner, int delayTicks, Runnable action)
    {
        return WhimRegistry.of(level).scheduler().add(level, owner, 0, Math.max(0, delayTicks), action);
    }

    public static WhimTask scheduleRepeating(ServerLevel level, UUID owner, int intervalTicks, Runnable action)
    {
        int interval = Math.max(1, intervalTicks);
        return WhimRegistry.of(level).scheduler().add(level, owner, interval, interval, action);
    }

    private WhimTask add(ServerLevel level, UUID owner, int interval, int delay, Runnable action)
    {
        WhimTask task = new WhimTask(owner, interval, level.getGameTime() + delay, action);
        this.tasks.add(task);
        return task;
    }

    // 先收集到期任务再执行，避免执行中改动任务表
    void tick(ServerLevel level)
    {
        if (this.tasks.isEmpty())
        {
            return;
        }

        long now = level.getGameTime();
        List<WhimTask> due = null;
        Iterator<WhimTask> iterator = this.tasks.iterator();

        while (iterator.hasNext())
        {
            WhimTask task = iterator.next();

            if (task.cancelled())
            {
                iterator.remove();
                continue;
            }

            if (task.executeAt() > now)
            {
                continue;
            }

            if (task.repeating())
            {
                task.advance();
            }
            else
            {
                iterator.remove();
            }

            if (due == null)
            {
                due = new ArrayList<>();
            }

            due.add(task);
        }

        if (due == null)
        {
            return;
        }

        for (WhimTask task : due)
        {
            if (!task.cancelled())
            {
                task.run();
            }
        }
    }

    void cancelAll(UUID owner)
    {
        if (owner == null)
        {
            return;
        }

        this.tasks.removeIf(task -> owner.equals(task.owner()));
    }

    void clear()
    {
        this.tasks.clear();
    }
}
