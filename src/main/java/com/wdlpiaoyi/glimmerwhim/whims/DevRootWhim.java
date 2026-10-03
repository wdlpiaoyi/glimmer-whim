package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.Locale;

import com.wdlpiaoyi.glimmerwhim.engine.WhimDimensions;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DevRootWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_root");

    public static final DevRootWhim INSTANCE = new DevRootWhim();

    private DevRootWhim()
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
        if (event.kind() != WhimEvent.Kind.USE)
        {
            return;
        }

        double power = event.chain().map(chain -> chain.value(WhimDimensions.POWER)).orElse(0.0D);
        double range = event.chain().map(chain -> chain.value(WhimDimensions.RANGE)).orElse(0.0D);
        double factor = event.chain().map(chain -> chain.factor()).orElse(1.0D);

        event.player().sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                "dev_root: %s power=%.2f range=%.2f factor=%.2f",
                event.target().map(target -> target.describe()).orElse("none"), power, range, factor)));
        event.remove(WhimRemoveReason.USED);
    }
}
