package com.wdlpiaoyi.glimmerwhim.client;

import net.minecraft.client.Minecraft;

public final class WhimReach
{
    private WhimReach()
    {
    }

    public static double blocks()
    {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0D;
    }
}
