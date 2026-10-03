package com.wdlpiaoyi.glimmerwhim.anchor;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import java.util.Optional;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface WhimAnchor
{
    ResourceLocation type();

    void write(FriendlyByteBuf buf);

    Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params);
}
