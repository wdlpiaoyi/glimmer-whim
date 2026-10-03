package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRemoveReason;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DevRemoveOnUseAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim",
            "dev_removeonuse");

    private static final String WRONG = "该锚不接受锚数据，固定于 0 0 0";

    public static DevRemoveOnUseAnchor parse(CommandSourceStack source, String data)
    {
        if (data != null && !data.isBlank())
        {
            throw new IllegalArgumentException(WRONG);
        }

        return new DevRemoveOnUseAnchor();
    }

    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of();
    }

    public static DevRemoveOnUseAnchor read(FriendlyByteBuf buf)
    {
        return new DevRemoveOnUseAnchor();
    }

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

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() == WhimEvent.Kind.USE)
        {
            event.remove(WhimRemoveReason.USED);
        }
    }
}
