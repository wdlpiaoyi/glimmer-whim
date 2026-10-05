// parked：已移出构建。恢复到 src 前需按当前引擎 API 校对（如 modid 用 GlimmerWhim.MODID；WhimChain 已无 value()/factor()；周期生成器 spawnInterval 已删除）。
package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class TraceTestWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "trace_test");

    public static final TraceTestWhim INSTANCE = new TraceTestWhim();

    private TraceTestWhim()
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
                WhimParam.positiveNumber("size", "1"),
                WhimParam.positiveNumber(Whim.PLAYTIME, "20"));
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() == WhimEvent.Kind.USE)
        {
            event.target().ifPresent(target -> event.player()
                    .sendSystemMessage(Component.literal("trace_test: " + target.describe())));
            event.remove(WhimRemoveReason.USED);
        }
    }
}
