package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 灵感长在哪儿。
 * <p>
 * 锚是接口，具体锚以后补。这里只定死两件事：锚怎么进包，以及客户端怎么知道它在哪。
 * <p>
 * {@link #position} 永远不进包 —— 每帧现算，所以有的锚可以没有距离。
 */
public interface WhimAnchor
{
    /** 这是哪种锚。 */
    ResourceLocation type();

    /** 怎么进包。 */
    void write(FriendlyByteBuf buf);

    /**
     * 客户端：它现在在哪。
     * <p>
     * 可以没有距离 —— 只知道自己朝哪的锚，从 {@code params} 里挑一个距离补上就行。返回空表示这一帧算不出来。
     */
    Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params);
}
