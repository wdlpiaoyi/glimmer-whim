package com.wdlpiaoyi.glimmerwhim.client;

import net.minecraft.client.Minecraft;

public final class WhimReach
{
    private WhimReach()
    {
    }

    public static double blocks()
    {
        return com.wdlpiaoyi.glimmerwhim.engine.WhimReach.blocks(
                Minecraft.getInstance().options.getEffectiveRenderDistance());
    }
}
