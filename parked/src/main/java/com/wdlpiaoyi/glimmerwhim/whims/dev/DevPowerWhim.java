// parked：已移出构建。恢复到 src 前需按当前引擎 API 校对（如 modid 用 GlimmerWhim.MODID；WhimChain 已无 value()/factor()；周期生成器 spawnInterval 已删除）。
package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimDimensions;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimModifier;
import com.wdlpiaoyi.glimmerwhim.whims.WhimRole;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

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
