package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

// 客户端目标产生器登记表：按优先级从高到低取第一个命中；新种类 = 注册一个函数
public final class WhimTargeters
{
    // root = 链根；left = 按住期间是否离开过链根
    public record Context(ClientLevel level, LocalPlayer player, Vec3 eye, Vec3 look, UUID root, boolean left)
    {
    }

    @FunctionalInterface
    public interface Targeter
    {
        Optional<WhimTarget> pick(Context context);
    }

    private record Entry(int priority, Targeter targeter)
    {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private WhimTargeters()
    {
    }

    public static void register(int priority, Targeter targeter)
    {
        ENTRIES.add(new Entry(priority, targeter));
        ENTRIES.sort(Comparator.comparingInt(Entry::priority).reversed());
    }

    public static Optional<WhimTarget> pick(Context context)
    {
        for (Entry entry : ENTRIES)
        {
            Optional<WhimTarget> target = entry.targeter().pick(context);

            if (target.isPresent())
            {
                return target;
            }
        }

        return Optional.empty();
    }
}
