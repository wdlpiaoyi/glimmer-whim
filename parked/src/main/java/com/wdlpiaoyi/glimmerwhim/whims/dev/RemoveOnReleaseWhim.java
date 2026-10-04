// parked：已移出构建。恢复到 src 前需按当前引擎 API 校对（如 modid 用 GlimmerWhim.MODID；WhimChain 已无 value()/factor()；周期生成器 spawnInterval 已删除）。
package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;

public final class RemoveOnReleaseWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_removeonrelease");

    public static final RemoveOnReleaseWhim INSTANCE = new RemoveOnReleaseWhim();

    private RemoveOnReleaseWhim()
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
        if (event.kind() == WhimEvent.Kind.USE)
        {
            event.remove(WhimRemoveReason.USED);
        }
    }
}
