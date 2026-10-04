package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;

// 每维度任务表：按游戏刻有序，每 tick 只取到点任务；owner 灵感移除即取消
public final class WhimScheduler
{
    private final TreeMap<Long, List<WhimTask>> tasks = new TreeMap<>();

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
        this.tasks.computeIfAbsent(task.executeAt(), key -> new ArrayList<>()).add(task);
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
        List<WhimTask> due = new ArrayList<>();

        while (!this.tasks.isEmpty() && this.tasks.firstKey() <= now)
        {
            due.addAll(this.tasks.pollFirstEntry().getValue());
        }

        for (WhimTask task : due)
        {
            if (task.cancelled())
            {
                continue;
            }

            if (task.repeating())
            {
                reschedule(task);
            }

            task.run();
        }
    }

    private void reschedule(WhimTask task)
    {
        task.advance();
        this.tasks.computeIfAbsent(task.executeAt(), key -> new ArrayList<>()).add(task);
    }

    void cancelAll(UUID owner)
    {
        if (owner == null)
        {
            return;
        }

        this.tasks.values().forEach(list -> list.removeIf(task -> owner.equals(task.owner())));
        this.tasks.values().removeIf(List::isEmpty);
    }

    void clear()
    {
        this.tasks.clear();
    }
}
