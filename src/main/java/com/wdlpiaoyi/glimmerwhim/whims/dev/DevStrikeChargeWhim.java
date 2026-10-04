package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;

// 落雷前的蓄力体，由 DevStrikeWhim 召唤；只做视觉占位，lifetime 由召唤方写入
public final class DevStrikeChargeWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_strike_charge");

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
