package com.wdlpiaoyi.glimmerwhim.client;

import net.minecraft.client.Minecraft;

/**
 * "看得见"的边界：原版自己的渲染距离。
 * <p>
 * 灵感画到这儿为止、瞄也算到这儿为止 —— 再远的东西屏幕上根本没有，算了也白算。
 * <p>
 * 这个数不另立配置项：直接读原版那个渲染距离选项（选项 → 视频设置 → 渲染距离）。
 * 玩家把渲染距离调小，灵感跟着就近了，两边永远一致，不用记两个地方。
 */
public final class WhimReach
{
    private WhimReach()
    {
    }

    /** 渲染距离多少格（原版那个选项的单位是区块，一格 16 方块）。 */
    public static double blocks()
    {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0D;
    }
}
