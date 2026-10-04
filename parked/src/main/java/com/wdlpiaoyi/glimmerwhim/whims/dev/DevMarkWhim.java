package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DevMarkWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_mark");

    public static final DevMarkWhim INSTANCE = new DevMarkWhim();

    private DevMarkWhim()
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
            event.target().ifPresent(target -> event.player()
                    .sendSystemMessage(Component.literal("dev_mark: " + target.describe())));
            event.remove(WhimRemoveReason.USED);
        }
    }
}
