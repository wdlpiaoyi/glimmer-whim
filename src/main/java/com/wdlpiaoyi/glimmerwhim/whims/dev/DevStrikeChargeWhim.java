package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;

public final class DevStrikeChargeWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_strike_charge");

    public static final DevStrikeChargeWhim INSTANCE = new DevStrikeChargeWhim();

    private DevStrikeChargeWhim()
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
        return WhimParams.of(WhimParam.choice("shape", "quad", "cube", "quad"), WhimParam.positiveNumber("size", "4"));
    }

    @Override
    public boolean requiresLineOfSight()
    {
        return true;
    }

    @Override
    public boolean interactable()
    {
        return false;
    }
}
