package com.wdlpiaoyi.glimmerwhim.whims;

import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DevCoordWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_coord");

    public static final DevCoordWhim INSTANCE = new DevCoordWhim();

    private DevCoordWhim()
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
    public boolean acceptsTarget(WhimTarget target)
    {
        return target != null && target.entity() == null;
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() == WhimEvent.Kind.USE)
        {
            event.target().ifPresent(target -> event.player()
                    .sendSystemMessage(Component.literal("dev_coord: " + target.describe())));
            event.remove(WhimRemoveReason.USED);
        }
    }
}
