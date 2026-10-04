package com.wdlpiaoyi.glimmerwhim.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class WhimConfig
{
    // 自由视角按键模式：HOLD = 按住，TOGGLE = 切换
    public enum FreeLookMode
    {
        HOLD,
        TOGGLE
    }

    // 客户端配置树：瞄准 / 自由视角 / 渲染
    public static final ForgeConfigSpec CLIENT_SPEC;

    // 两端通用配置树：灵感 / 命令 / 调试
    public static final ForgeConfigSpec COMMON_SPEC;

    private static final ForgeConfigSpec.BooleanValue AIM_THROUGH_WALLS;

    private static final ForgeConfigSpec.EnumValue<FreeLookMode> FREE_LOOK_MODE;

    private static final ForgeConfigSpec.IntValue FREE_LOOK_FADE;

    private static final ForgeConfigSpec.DoubleValue FREE_LOOK_SENSITIVITY;

    private static final ForgeConfigSpec.DoubleValue FREE_LOOK_PITCH_LIMIT;

    private static final ForgeConfigSpec.DoubleValue FREE_LOOK_YAW_LIMIT;

    private static final ForgeConfigSpec.DoubleValue RENDER_OUTLINE;

    private static final ForgeConfigSpec.BooleanValue RENDER_FACE_SHADE;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_COLOR_AIMED;

    private static final ForgeConfigSpec.BooleanValue RENDER_TRACE_ENABLED;

    private static final ForgeConfigSpec.DoubleValue RENDER_TRACE_WIDTH;

    private static final ForgeConfigSpec.IntValue RENDER_DEV_CHECKER_CELLS;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_DEV_COLOR_ELEMENT;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_DEV_COLOR_ELEMENT_ALT;

    // 颜色字符串解析失败时的兜底 RGBA
    private static final float[] FALLBACK_DEV_ELEMENT = { 1.0F, 0.0F, 1.0F, 1.0F };

    private static final float[] FALLBACK_DEV_ELEMENT_ALT = { 0.0F, 0.0F, 0.0F, 1.0F };

    private static final float[] FALLBACK_AIMED = { 1.0F, 1.0F, 1.0F, 1.0F };

    private static final ForgeConfigSpec.IntValue MAX_CHAIN_LENGTH;

    private static final ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL;

    private static final ForgeConfigSpec.DoubleValue COMMAND_SUGGEST_REACH;

    private static final ForgeConfigSpec.BooleanValue DEBUG_VERBOSE_LOG;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("瞄准：准星落在灵感绘制的体积内时，灵感高亮。");
        builder.push("aim");
        AIM_THROUGH_WALLS = builder.comment("忽略方块遮挡：命中体积即视为瞄准，不检查中间遮挡。默认 true。")
                .define("throughWalls", true);

        builder.pop();

        builder.comment("自由视角：镜头朝向与玩家朝向独立，镜头位置仍跟随玩家。键位在“选项 → 控制”中修改。");
        builder.push("freelook");
        FREE_LOOK_MODE = builder.comment("按键模式。HOLD = 按住时启用，TOGGLE = 按键切换。默认 HOLD。")
                .defineEnum("mode", FreeLookMode.HOLD);
        FREE_LOOK_FADE = builder.comment("松开后镜头回正到玩家朝向的时长（毫秒）。0 = 立即回正。默认 180。")
                .defineInRange("fadeMillis", 180, 0, 5000);
        FREE_LOOK_SENSITIVITY = builder.comment("镜头转向速度倍率。1.0 = 与原版一致。默认 1.0。")
                .defineInRange("sensitivity", 1.0D, 0.05D, 5.0D);
        FREE_LOOK_PITCH_LIMIT = builder.comment("俯仰角上限（度）。原版为 90（正上/正下）。默认 90。")
                .defineInRange("pitchLimitDegrees", 90.0D, 1.0D, 90.0D);
        FREE_LOOK_YAW_LIMIT = builder
                .comment("偏航角相对玩家朝向的最大偏移（度）。",
                        "180 = 不限制，与原版一致。默认 180。")
                .defineInRange("yawLimitDegrees", 180.0D, 1.0D, 180.0D);
        builder.pop();

        builder.comment("渲染：灵感的绘制外观。",
                "通用设置在顶层；单个元素的外观放在 [render.<元素>] 下（当前仅有 [render.dev]）。");
        builder.push("render");
        RENDER_OUTLINE = builder.comment("瞄准时高亮轮廓相对本体的外扩宽度（方块）。0 = 不绘制高亮。默认 0.08。")
                .defineInRange("outlineWidth", 0.08D, 0.0D, 0.5D);
        RENDER_FACE_SHADE = builder.comment("是否按原版对面片施加明暗（上亮下暗）。关闭后六面亮度一致。默认 true。")
                .define("faceShade", true);
        RENDER_COLOR_AIMED = builder.comment("瞄准时高亮轮廓颜色。默认 FFFFFF（白）。")
                .define("colorAimed", "FFFFFF");
        RENDER_TRACE_ENABLED = builder.comment("是否绘制串联灵感时的牵引折线（trace）。默认 true。")
                .define("traceEnabled", true);
        RENDER_TRACE_WIDTH = builder.comment("牵引折线的线宽（像素）。默认 2.0。")
                .defineInRange("traceWidth", 2.0D, 1.0D, 16.0D);

        builder.comment("dev 元素外观：紫黑棋盘格。",
                "该配置仅用于 dev 元素，正式元素各自实现。");
        builder.push("dev");
        RENDER_DEV_CHECKER_CELLS = builder.comment("每个面划分的棋盘格数。默认 2。")
                .defineInRange("checkerCells", 2, 1, 8);
        RENDER_DEV_COLOR_ELEMENT = builder.comment("棋盘主色，RRGGBB 或 AARRGGBB（可含 #）。默认 FF00FF（紫）。")
                .define("colorElement", "FF00FF");
        RENDER_DEV_COLOR_ELEMENT_ALT = builder.comment("棋盘副色。默认 000000（黑）。")
                .define("colorElementAlt", "000000");
        builder.pop(2);

        CLIENT_SPEC = builder.build();
    }

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("灵感：/glimmerwhim summon 的默认行为。");
        builder.push("whim");
        MAX_CHAIN_LENGTH = builder
                .comment("一次施法最多串联的灵感条数（含根）；0 = 禁止串联（只能用根）。默认 8。")
                .defineInRange("maxChainLength", 8, 0, 64);
        builder.pop();

        builder.comment("命令：/glimmerwhim 的权限与补全。");
        builder.push("command");
        COMMAND_PERMISSION_LEVEL = builder.comment("/glimmerwhim 所需权限等级：0 = 所有人，2 = OP（原版默认）。默认 2。")
                .defineInRange("permissionLevel", 2, 0, 4);
        COMMAND_SUGGEST_REACH = builder.comment("TAB 补全中射线拾取瞄准点的最大距离（方块）。默认 32。")
                .defineInRange("suggestReach", 32.0D, 1.0D, 256.0D);
        builder.pop();

        builder.comment("调试：日志输出。");
        builder.push("debug");
        DEBUG_VERBOSE_LOG = builder.comment("是否将调试信息输出为 info 级别；关闭后仅输出 debug 级别。默认 true。")
                .define("verboseLog", true);
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private WhimConfig()
    {
    }

    public static boolean aimThroughWalls()
    {
        return AIM_THROUGH_WALLS.get();
    }

    public static FreeLookMode freeLookMode()
    {
        return FREE_LOOK_MODE.get();
    }

    public static int freeLookFadeMillis()
    {
        return FREE_LOOK_FADE.get();
    }

    public static double freeLookSensitivity()
    {
        return FREE_LOOK_SENSITIVITY.get();
    }

    public static double freeLookPitchLimitDegrees()
    {
        return FREE_LOOK_PITCH_LIMIT.get();
    }

    public static double freeLookYawLimitDegrees()
    {
        return FREE_LOOK_YAW_LIMIT.get();
    }

    public static double renderOutlineWidth()
    {
        return RENDER_OUTLINE.get();
    }

    public static int renderDevCheckerCells()
    {
        return RENDER_DEV_CHECKER_CELLS.get();
    }

    public static boolean renderFaceShade()
    {
        return RENDER_FACE_SHADE.get();
    }

    public static float[] renderDevColorElement()
    {
        return color(RENDER_DEV_COLOR_ELEMENT.get(), FALLBACK_DEV_ELEMENT);
    }

    public static float[] renderDevColorElementAlt()
    {
        return color(RENDER_DEV_COLOR_ELEMENT_ALT.get(), FALLBACK_DEV_ELEMENT_ALT);
    }

    public static float[] renderColorAimed()
    {
        return color(RENDER_COLOR_AIMED.get(), FALLBACK_AIMED);
    }

    public static boolean traceEnabled()
    {
        return RENDER_TRACE_ENABLED.get();
    }

    public static double traceWidth()
    {
        return RENDER_TRACE_WIDTH.get();
    }

    public static int maxChainLength()
    {
        return MAX_CHAIN_LENGTH.get();
    }

    public static int commandPermissionLevel()
    {
        return COMMAND_PERMISSION_LEVEL.get();
    }

    public static double commandSuggestReach()
    {
        return COMMAND_SUGGEST_REACH.get();
    }

    public static boolean verboseLog()
    {
        return DEBUG_VERBOSE_LOG.get();
    }

    // 解析 #RRGGBB 或 #AARRGGBB；6 位补不透明 alpha；长度/字符非法时回退 fallback
    private static float[] color(String text, float[] fallback)
    {
        String hex = text == null ? "" : text.trim();

        if (hex.startsWith("#"))
        {
            hex = hex.substring(1);
        }

        if (hex.length() != 6 && hex.length() != 8)
        {
            return fallback;
        }

        try
        {
            long value = Long.parseLong(hex, 16);

            if (hex.length() == 6)
            {
                value |= 0xFF000000L;
            }

            return new float[] { ((value >> 16) & 0xFF) / 255.0F, ((value >> 8) & 0xFF) / 255.0F,
                    (value & 0xFF) / 255.0F, ((value >> 24) & 0xFF) / 255.0F };
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }
}
