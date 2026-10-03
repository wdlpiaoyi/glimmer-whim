package com.wdlpiaoyi.glimmerwhim.config;

import java.util.HashMap;
import java.util.Map;

import com.wdlpiaoyi.glimmerwhim.whim.Whim;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

/** 客户端配置：config/glimmerwhim-client.toml。 */
public final class WhimConfig
{
    /** 自由视角的按法。 */
    public enum FreeLookMode
    {
        /** 按住才看，松手就回来。 */
        HOLD,
        /** 按一下开着，再按一下关掉。 */
        TOGGLE
    }

    public static final ForgeConfigSpec CLIENT_SPEC;

    private static final ForgeConfigSpec.DoubleValue DEFAULT_CONE;

    private static final ForgeConfigSpec.EnumValue<FreeLookMode> FREE_LOOK_MODE;

    private static final Map<ResourceLocation, ForgeConfigSpec.DoubleValue> CONES = new HashMap<>();

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("瞄准。");
        builder.push("aim");
        DEFAULT_CONE = builder.comment("瞄准锥半角（度）。没在 cone 里单独写的元素用这个。")
                .defineInRange("defaultConeDegrees", 10.0D, 0.0D, 180.0D);
        builder.comment("按元素覆盖。");
        builder.push("cone");

        for (ResourceLocation element : Whim.ELEMENTS)
        {
            CONES.put(element, builder.defineInRange(element.toString(), 10.0D, 0.0D, 180.0D));
        }

        builder.pop(2);

        builder.comment("自由视角：镜头和人的朝向分家，人站哪儿镜头就在哪儿。键位在 选项 → 控制 里改。");
        builder.push("freelook");
        FREE_LOOK_MODE = builder.comment("按住还是切换。HOLD = 按住才看，TOGGLE = 按一下切换。")
                .defineEnum("mode", FreeLookMode.HOLD);
        builder.pop();

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

    /** 自由视角是按住还是切换，默认按住。 */
    public static FreeLookMode freeLookMode()
    {
        return FREE_LOOK_MODE.get();
    }
}
