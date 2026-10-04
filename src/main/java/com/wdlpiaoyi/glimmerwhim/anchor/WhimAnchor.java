package com.wdlpiaoyi.glimmerwhim.anchor;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 服务端权威锚点：负责网络编解码，并在给定眼位/插值下解析世界坐标
public interface WhimAnchor
{
    ResourceLocation type();

    void write(FriendlyByteBuf buf);

    // partialTick 用于插值；无法解析时返回 empty
    Optional<Vec3> position(Level level, Vec3 eye, float partialTick);
}
