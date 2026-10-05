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
    private static final ForgeConfigSpec.DoubleValue TARGET_HITBOX_SCALE;

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

    private static final ForgeConfigSpec.IntValue RENDER_DEFAULT_CHECKER_CELLS;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_DEFAULT_COLOR_ELEMENT;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_DEFAULT_COLOR_ELEMENT_ALT;

    // 颜色字符串解析失败时的兜底 RGBA
    private static final float[] FALLBACK_COLOR = { 1.0F, 0.0F, 1.0F, 1.0F };

    private static final float[] FALLBACK_COLOR_ALT = { 0.0F, 0.0F, 0.0F, 1.0F };

    private static final float[] FALLBACK_AIMED = { 1.0F, 1.0F, 1.0F, 1.0F };

    private static final ForgeConfigSpec.IntValue MAX_CHAIN_LENGTH;

    private static final ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL;

    private static final ForgeConfigSpec.DoubleValue COMMAND_SUGGEST_REACH;

    private static final ForgeConfigSpec.BooleanValue STRIKE_ENABLED;

    private static final ForgeConfigSpec.DoubleValue STRIKE_CHANCE;

    private static final ForgeConfigSpec.IntValue STRIKE_MIN_HEALTH;

    private static final ForgeConfigSpec.DoubleValue STRIKE_MAX_DAMAGE_RATIO;
    private static final ForgeConfigSpec.DoubleValue STRIKE_DAMAGE_RATIO;
    private static final ForgeConfigSpec.DoubleValue STRIKE_SIZE;
    private static final ForgeConfigSpec.DoubleValue STRIKE_RADIUS;
    private static final ForgeConfigSpec.IntValue STRIKE_BOLTS;
    private static final ForgeConfigSpec.DoubleValue STRIKE_SPREAD;
    private static final ForgeConfigSpec.IntValue STRIKE_CHARGE_TICKS;
    private static final ForgeConfigSpec.IntValue STRIKE_GLOW_TICKS;

    private static final ForgeConfigSpec.IntValue STRIKE_COMBAT_WINDOW;

    private static final ForgeConfigSpec.IntValue STRIKE_ROLL_INTERVAL;

    private static final ForgeConfigSpec.IntValue STRIKE_COOLDOWN;

    private static final ForgeConfigSpec.BooleanValue STRIKE_OVERWORLD_ONLY;

    private static final ForgeConfigSpec.BooleanValue DEBUG_VERBOSE_LOG;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("瞄准：准星落在灵感绘制的体积内时，灵感高亮。");
        builder.push("aim");
        AIM_THROUGH_WALLS = builder.comment("忽略方块遮挡：命中体积即视为瞄准，不检查中间遮挡。默认 true。")
                .define("throughWalls", true);
        TARGET_HITBOX_SCALE = builder.comment("目标拾取：实体命中箱的放大倍率，越大越好瞄。默认 1.5。")
                .defineInRange("targetHitboxScale", 1.5D, 1.0D, 10.0D);

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
                "这里与具体元素无关；没有自定义绘制的元素用 [render.default] 的外观。");
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

        builder.comment("默认外观：没有自定义绘制的元素都以此绘制。",
                "元素用 WhimRenders 登记自己的绘制后不再受这里影响。");
        builder.push("default");
        RENDER_DEFAULT_CHECKER_CELLS = builder.comment("每个面划分的棋盘格数。默认 2。")
                .defineInRange("checkerCells", 2, 1, 8);
        RENDER_DEFAULT_COLOR_ELEMENT = builder.comment("棋盘主色，RRGGBB 或 AARRGGBB（可含 #）。默认 FF00FF（紫）。")
                .define("colorElement", "FF00FF");
        RENDER_DEFAULT_COLOR_ELEMENT_ALT = builder.comment("棋盘副色。默认 000000（黑）。")
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

        builder.comment("落雷（strike）：触发条件与数值默认值。");
        builder.push("strike");
        STRIKE_ENABLED = builder.comment("是否启用触发。默认 true。")
                .define("enabled", true);
        STRIKE_CHANCE = builder.comment("满足全部条件后每次抽签的触发概率。默认 0.005。")
                .defineInRange("chance", 0.005D, 0.0D, 1.0D);
        STRIKE_MIN_HEALTH = builder.comment("目标最大生命下限；低于此值不触发。默认 500。")
                .defineInRange("min_health", 500, 1, Integer.MAX_VALUE);
        // 阈值参数，上界设为无穷：超过 1 只意味着「不因伤害过大而拒绝」，有限上界只是画蛇添足
        STRIKE_MAX_DAMAGE_RATIO = builder.comment("本次伤害相对目标最大生命的上限（可大于 1，一击可能超过目标最大生命）；高于此值不触发（说明目标还不够强）。默认 0.05。")
                .defineInRange("max_damage_ratio", 0.05D, 0.0D, Double.POSITIVE_INFINITY);
        STRIKE_COMBAT_WINDOW = builder.comment("战斗标记时长（tick）：窗口内造成或受到过有源伤害才算在战斗中。默认 240（12 秒）。")
                .defineInRange("combat_window_ticks", 240, 1, 72000);
        STRIKE_ROLL_INTERVAL = builder.comment("两次抽签的最小间隔（tick）。默认 20（1 秒）。")
                .defineInRange("roll_interval_ticks", 20, 0, 72000);
        STRIKE_COOLDOWN = builder.comment("生成冷却（tick），与灵感自身寿命一致；{lifetime} 可覆盖。默认 100。")
                .defineInRange("cooldown_ticks", 100, 0, 72000);
        STRIKE_OVERWORLD_ONLY = builder.comment("是否只在主世界触发。默认 true。")
                .define("overworld_only", true);
        STRIKE_DAMAGE_RATIO = builder.comment("落雷中心命中伤害相对目标最大生命的比例，作为 {damage_ratio} 的默认值（单次召唤可覆盖）；可大于 1。默认 1。")
                .defineInRange("damage_ratio", 1.0D, 0.0D, Double.POSITIVE_INFINITY);
        STRIKE_SIZE = builder.comment("面片边长（方块），作为 {size} 的默认值；同时决定命中体积。默认 4。")
                .defineInRange("size", 4.0D, 0.0D, Double.POSITIVE_INFINITY);
        STRIKE_RADIUS = builder.comment("范围伤害半径（方块），作为 {radius} 的默认值。默认 2。")
                .defineInRange("radius", 2.0D, 0.0D, Double.POSITIVE_INFINITY);
        STRIKE_BOLTS = builder.comment("视觉闪电道数，作为 {bolts} 的默认值。默认 10。")
                .defineInRange("bolts", 10, 1, 1000);
        STRIKE_SPREAD = builder.comment("闪电散布半径（方块），作为 {spread} 的默认值。默认 2。")
                .defineInRange("spread", 2.0D, 0.0D, Double.POSITIVE_INFINITY);
        STRIKE_CHARGE_TICKS = builder.comment("蓄力时长（tick），作为 {charge} 的默认值。默认 35。")
                .defineInRange("charge_ticks", 35, 0, 72000);
        STRIKE_GLOW_TICKS = builder.comment("目标发光时长（tick），作为 {glow} 的默认值。默认 40。")
                .defineInRange("glow_ticks", 40, 0, 72000);
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

    public static double targetHitboxScale()
    {
        return TARGET_HITBOX_SCALE.get();
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

    public static int renderDefaultCheckerCells()
    {
        return RENDER_DEFAULT_CHECKER_CELLS.get();
    }

    public static boolean renderFaceShade()
    {
        return RENDER_FACE_SHADE.get();
    }

    public static float[] renderDefaultColorElement()
    {
        return color(RENDER_DEFAULT_COLOR_ELEMENT.get(), FALLBACK_COLOR);
    }

    public static float[] renderDefaultColorElementAlt()
    {
        return color(RENDER_DEFAULT_COLOR_ELEMENT_ALT.get(), FALLBACK_COLOR_ALT);
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

    public static boolean strikeEnabled()
    {
        return STRIKE_ENABLED.get();
    }

    public static double strikeChance()
    {
        return STRIKE_CHANCE.get();
    }

    public static int strikeMinHealth()
    {
        return STRIKE_MIN_HEALTH.get();
    }

    public static double strikeMaxDamageRatio()
    {
        return STRIKE_MAX_DAMAGE_RATIO.get();
    }

    // {damage_ratio} 的默认值
    public static double strikeDamageRatio()
    {
        return STRIKE_DAMAGE_RATIO.get();
    }

    // {size} 的默认值
    public static double strikeSize()
    {
        return STRIKE_SIZE.get();
    }

    // {radius} 的默认值
    public static double strikeRadius()
    {
        return STRIKE_RADIUS.get();
    }

    // {bolts} 的默认值
    public static int strikeBolts()
    {
        return STRIKE_BOLTS.get();
    }

    // {spread} 的默认值
    public static double strikeSpread()
    {
        return STRIKE_SPREAD.get();
    }

    // {charge} 的默认值
    public static int strikeChargeTicks()
    {
        return STRIKE_CHARGE_TICKS.get();
    }

    // {glow} 的默认值
    public static int strikeGlowTicks()
    {
        return STRIKE_GLOW_TICKS.get();
    }

    public static int strikeCombatWindowTicks()
    {
        return STRIKE_COMBAT_WINDOW.get();
    }

    public static int strikeRollIntervalTicks()
    {
        return STRIKE_ROLL_INTERVAL.get();
    }

    public static int strikeCooldownTicks()
    {
        return STRIKE_COOLDOWN.get();
    }

    public static boolean strikeOverworldOnly()
    {
        return STRIKE_OVERWORLD_ONLY.get();
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
