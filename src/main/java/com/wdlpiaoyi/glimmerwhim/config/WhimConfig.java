package com.wdlpiaoyi.glimmerwhim.config;

import java.util.HashMap;
import java.util.Map;

import com.wdlpiaoyi.glimmerwhim.whim.Whim;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

/** 客户端配置：config/glimmerwhim-client.toml。 */
public final class WhimConfig
{
    public static final ForgeConfigSpec CLIENT_SPEC;

    private static final ForgeConfigSpec.DoubleValue DEFAULT_CONE;

    private static final Map<ResourceLocation, ForgeConfigSpec.DoubleValue> CONES = new HashMap<>();

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("瞄准。");
        builder.push("aim");
        DEFAULT_CONE = builder.comment("瞄准锥半角（度）。没在 cone 里单独写的元素用这个。")
                .defineInRange("defaultConeDegrees", 3.0D, 0.0D, 180.0D);
        builder.comment("按元素覆盖。");
        builder.push("cone");

        for (ResourceLocation element : Whim.ELEMENTS)
        {
            CONES.put(element, builder.defineInRange(element.toString(), 3.0D, 0.0D, 180.0D));
        }

        builder.pop(2);

        CLIENT_SPEC = builder.build();
    }

    private WhimConfig()
    {
    }

    /** 这条灵感用的瞄准锥半角（度）。 */
    public static double aimConeDegrees(ResourceLocation element)
    {
        ForgeConfigSpec.DoubleValue cone = CONES.get(element);

        return cone == null ? DEFAULT_CONE.get() : cone.get();
    }
}
