package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimModifier;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;

public final class DevRangeWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_range");

    public static final DevRangeWhim INSTANCE = new DevRangeWhim();

    private DevRangeWhim()
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
        return WhimParams.of(WhimParam.positiveNumber("amount", "4"));
    }

    // 只能作修饰，不能起链
    @Override
    public boolean canRoot()
    {
        return false;
    }

    @Override
    public Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.of(new WhimModifier(DevDomains.RANGE.id(), params().number(data, "amount")));
    }
}
