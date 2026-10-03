package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 灵感长在哪儿。
 * <p>
 * 锚是接口，具体锚以后补。这里只定死两件事：锚怎么进包，以及客户端怎么知道往哪看。
 * <p>
 * {@link #direction} 永远不进包 —— 两个端各算一份。天球锚那种指向无穷远、没有距离的东西
 * 之所以能成立，全靠这一条：它需要同步的只有"这条灵感存在、是什么、还剩多久"，方向自己算。
 */
public interface WhimAnchor
{
    /** 这是哪种锚。 */
    ResourceLocation type();

    /** 怎么进包。 */
    void write(FriendlyByteBuf buf);

    /**
     * 客户端：往哪看能找到它。
     * <p>
     * 可以没有距离 —— 返回一个方向就够了。返回空表示这一帧算不出来（比如天还没黑）。
     */
    Optional<Vec3> direction(Level level, Vec3 eye, float partialTick);
}
