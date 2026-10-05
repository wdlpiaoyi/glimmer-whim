package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimModifier;
import com.wdlpiaoyi.glimmerwhim.whims.WhimRole;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;

public final class DevPowerWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_power");

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
        return WhimParams.of(WhimParam.positiveNumber("amount", "2"));
    }

    @Override
    public Set<WhimRole> roles()
    {
        return EnumSet.of(WhimRole.MODIFIER);
    }

    @Override
    public Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.of(new WhimModifier(DevDomains.POWER.id(), params().number(data, "amount")));
    }
}
