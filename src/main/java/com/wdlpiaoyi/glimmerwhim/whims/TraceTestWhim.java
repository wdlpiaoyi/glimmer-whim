package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.EnumSet;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class TraceTestWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "trace_test");

    public static final TraceTestWhim INSTANCE = new TraceTestWhim();

    private TraceTestWhim()
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
        return WhimParams.of(WhimParam.choice("shape", "cube", "cube", "quad"),
                WhimParam.positiveNumber("size", "1"),
                WhimParam.positiveNumber(Whim.PLAYTIME, "1"));
    }

    @Override
    public Set<WhimRole> roles()
    {
        return EnumSet.of(WhimRole.ELEMENT, WhimRole.MODIFIER);
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() == WhimEvent.Kind.USE)
        {
            event.target().ifPresent(target -> event.player()
                    .sendSystemMessage(Component.literal("trace_test: " + target.describe())));
            event.remove(WhimRemoveReason.USED);
        }
    }
}
