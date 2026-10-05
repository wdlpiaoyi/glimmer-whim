package com.wdlpiaoyi.glimmerwhim.whims.dev.template;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTargets;

import net.minecraft.resources.ResourceLocation;

// 目标种类扩展点模板：kind id + 中文提示 + 服务端校验谓词 + payload 位置语义。
// 真实内容通常写成 public static final Kind 字段，在类加载时登记（见 WhimTargets 的 POINT/ENTITY/WHIM）。
// payload 键的约定见 WhimTargets（内置 x/y/z 命中点、entity/whim 的 uuid）。
// 这里只做服务端校验；要在客户端产生这种目标，还得配一个 WhimTargeters 产生器（见 TargetTemplatePick）。
// 本类自身不注册。
public final class TargetTemplate
{
    private TargetTemplate()
    {
    }

    // 示范一次登记；把返回值存成字段即可像内置种类那样使用
    public static WhimTargets.Kind register()
    {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "template_mark");

        // 第 3 参是服务端校验（这里恒真）；第 4 参是 payload 的位置语义，默认读 payload 的 x/y/z
        return WhimTargets.register(id, "模板标记点", (player, data) -> true, WhimTargets::position);
    }
}
