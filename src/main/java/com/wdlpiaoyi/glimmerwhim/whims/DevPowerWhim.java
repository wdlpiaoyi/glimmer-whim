package com.wdlpiaoyi.glimmerwhim.whims;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimDimensions;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.resources.ResourceLocation;

public final class DevPowerWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_power");

    public static final DevPowerWhim INSTANCE = new DevPowerWhim();

    private DevPowerWhim()
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
        return WhimParams.of(WhimParam.positiveNumber("amount", "1"));
    }

    @Override
    public Set<WhimRole> roles()
    {
        return EnumSet.of(WhimRole.MODIFIER);
    }

    @Override
    public Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.of(new WhimModifier(WhimDimensions.POWER, params().number(data, "amount")));
    }
}
