package com.wdlpiaoyi.glimmerwhim.anchor;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FixedAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "fixed");

    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
    }

    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params)
    {
        return Optional.of(Vec3.ZERO);
    }

    public static FixedAnchor read(FriendlyByteBuf buf)
    {
        return new FixedAnchor();
    }

    public static FixedAnchor parse(CommandSourceStack source, String data)
    {
        return new FixedAnchor();
    }

    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of();
    }
}
