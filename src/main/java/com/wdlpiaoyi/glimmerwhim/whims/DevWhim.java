package com.wdlpiaoyi.glimmerwhim.whims;

import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.resources.ResourceLocation;

public final class DevWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev");

    public static final DevWhim INSTANCE = new DevWhim();

    private DevWhim()
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
                WhimParam.positiveNumber("size", "1"));
    }
}
