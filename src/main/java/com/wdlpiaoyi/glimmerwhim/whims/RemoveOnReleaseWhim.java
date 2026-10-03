package com.wdlpiaoyi.glimmerwhim.whims;

import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;

import net.minecraft.resources.ResourceLocation;

public final class RemoveOnReleaseWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_removeonrelease");

    public static final RemoveOnReleaseWhim INSTANCE = new RemoveOnReleaseWhim();

    private RemoveOnReleaseWhim()
    {
    }

    @Override
    public ResourceLocation id()
    {
        return ID;
    }

    @Override
    public WhimParams params()
    {
        return WhimParams.NONE;
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
