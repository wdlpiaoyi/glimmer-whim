package com.wdlpiaoyi.glimmerwhim.config;

import java.util.List;

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

    public static final ForgeConfigSpec COMMON_SPEC;

    private static final ForgeConfigSpec.DoubleValue DEFAULT_CONE;

    private static final ForgeConfigSpec.BooleanValue AIM_THROUGH_WALLS;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> AIM_CONE_OVERRIDES;

    private static final ForgeConfigSpec.EnumValue<FreeLookMode> FREE_LOOK_MODE;

    private static final ForgeConfigSpec.IntValue FREE_LOOK_FADE;

    private static final ForgeConfigSpec.DoubleValue FREE_LOOK_SENSITIVITY;

    private static final ForgeConfigSpec.DoubleValue FREE_LOOK_PITCH_LIMIT;

    private static final ForgeConfigSpec.DoubleValue FREE_LOOK_YAW_LIMIT;

    private static final ForgeConfigSpec.DoubleValue RENDER_OUTLINE;

    private static final ForgeConfigSpec.BooleanValue RENDER_FACE_SHADE;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_COLOR_AIMED;

    // ---- [render.dev]：dev 元素自己的样子（棋盘格），别的元素各有各的 ----

    private static final ForgeConfigSpec.IntValue RENDER_DEV_CHECKER_CELLS;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_DEV_COLOR_ELEMENT;

    private static final ForgeConfigSpec.ConfigValue<String> RENDER_DEV_COLOR_ELEMENT_ALT;

    /** 颜色写错的时候用的兜底。 */
    private static final float[] FALLBACK_DEV_ELEMENT = { 1.0F, 0.0F, 1.0F, 1.0F };

    private static final float[] FALLBACK_DEV_ELEMENT_ALT = { 0.0F, 0.0F, 0.0F, 1.0F };

    private static final float[] FALLBACK_AIMED = { 1.0F, 1.0F, 1.0F, 1.0F };

    // ---- COMMON：config/glimmerwhim-common.toml，服务端也要读 ----

    private static final ForgeConfigSpec.IntValue DEFAULT_LIFETIME_TICKS;

    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_ANCHOR;

    private static final ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL;

    private static final ForgeConfigSpec.DoubleValue COMMAND_SUGGEST_REACH;

    private static final ForgeConfigSpec.IntValue VOID_TEST_HOLD_TICKS;

    private static final ForgeConfigSpec.DoubleValue VOID_TEST_DAMAGE;

    private static final ForgeConfigSpec.BooleanValue DEBUG_VERBOSE_LOG;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("瞄：瞄准锥（准星）碰到灵感，它就高亮。");
        builder.push("aim");
        DEFAULT_CONE = builder.comment("瞄准锥半角（度）。改这一个就全改了 —— 只有下面 cone 里点名过的元素例外。默认 10。")
                .defineInRange("defaultConeDegrees", 10.0D, 0.0D, 180.0D);
        AIM_THROUGH_WALLS = builder.comment("隔着方块也算瞄上（在锥里就亮，中间挡着什么不管）。默认 true。")
                .define("throughWalls", true);
        AIM_CONE_OVERRIDES = builder
                .comment("想给某个元素单独一个角度，就在这里加一条，写成 \"元素id=角度\"（元素 id 写全，像 \"glimmerwhim:dev=30\"）。",
                        "只写跟 defaultConeDegrees 不一样的；没点名的元素都跟默认走，所以这儿默认是空的。")
                .defineListAllowEmpty(List.of("cone"), List.of(), entry -> entry instanceof String);

        builder.pop();

        builder.comment("自由视角：镜头和人的朝向分家，人站哪儿镜头就在哪儿。键位在 选项 → 控制 里改。");
        builder.push("freelook");
        FREE_LOOK_MODE = builder.comment("按住还是切换。HOLD = 按住才看，TOGGLE = 按一下切换。默认 HOLD。")
                .defineEnum("mode", FreeLookMode.HOLD);
        FREE_LOOK_FADE = builder.comment("松手以后镜头退回人的朝向用多久（毫秒）。0 = 立刻弹回去。默认 180。")
                .defineInRange("fadeMillis", 180, 0, 5000);
        FREE_LOOK_SENSITIVITY = builder.comment("转镜头的快慢倍率。1.0 = 和原版一样，调小变慢、调大变快。默认 1.0。")
                .defineInRange("sensitivity", 1.0D, 0.05D, 5.0D);
        FREE_LOOK_PITCH_LIMIT = builder.comment("上下最多能看多少度。原版是 90（正上正下），调小就是不许抬那么高。默认 90。")
                .defineInRange("pitchLimitDegrees", 90.0D, 1.0D, 90.0D);
        FREE_LOOK_YAW_LIMIT = builder
                .comment("左右最多能离开人的朝向多少度。人是冻着的，所以就是从正前方往两边算。",
                        "180 = 不限，跟原版一样；调小就转不到旁边去。默认 180。")
                .defineInRange("yawLimitDegrees", 180.0D, 1.0D, 180.0D);
        builder.pop();

        builder.comment("画：灵感画出来长什么样。",
                "所有元素通用的放这一层；某个元素自己长什么样，放到 [render.<元素>] 里（现在只有 [render.dev]）。");
        builder.push("render");
        RENDER_OUTLINE = builder.comment("被瞄上的时候，那层高亮比本体宽出去多少（格）。0 = 不画高亮。默认 0.08。")
                .defineInRange("outlineWidth", 0.08D, 0.0D, 0.5D);
        RENDER_FACE_SHADE = builder.comment("六面要不要按原版那样分明暗（上亮下暗）。关掉就是六面一样亮。默认 true。")
                .define("faceShade", true);
        RENDER_COLOR_AIMED = builder.comment("被瞄上时那层的颜色。默认 FFFFFF 白。")
                .define("colorAimed", "FFFFFF");

        builder.comment("dev 元素长什么样：一副紫黑棋盘格。",
                "正式的元素各写各的，不吃这一套 —— 棋盘格只是拿来看位置的。");
        builder.push("dev");
        RENDER_DEV_CHECKER_CELLS = builder.comment("一面切成几格棋盘。2 = 原版贴图丢了的那副样子。默认 2。")
                .defineInRange("checkerCells", 2, 1, 8);
        RENDER_DEV_COLOR_ELEMENT = builder.comment("棋盘的第一色，RRGGBB 或 AARRGGBB（可带 #）。默认 FF00FF 紫。")
                .define("colorElement", "FF00FF");
        RENDER_DEV_COLOR_ELEMENT_ALT = builder.comment("棋盘的第二色。默认 000000 黑。")
                .define("colorElementAlt", "000000");
        builder.pop(2);

        CLIENT_SPEC = builder.build();
    }

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("灵感：/glimmerwhim summon 出来的默认怎么算。");
        builder.push("whim");
        DEFAULT_LIFETIME_TICKS = builder
                .comment("summon 不写 tick 时灵感活多久（1200 tick = 60 秒）；-1 = 永久。默认 1200。")
                .defineInRange("defaultLifetimeTicks", 1200, -1, 72000);
        DEFAULT_ANCHOR = builder
                .comment("summon 不写锚类型时默认用哪个锚；写锚的路径名（如 dev_ray、dev_pos），写错了退回 dev_ray。默认 dev_ray。")
                .define("defaultAnchor", "dev_ray");
        builder.pop();

        builder.comment("命令：/glimmerwhim 的权限和补全。");
        builder.push("command");
        COMMAND_PERMISSION_LEVEL = builder.comment("谁能用 /glimmerwhim：0 = 所有人，2 = OP（原版默认）。默认 2。")
                .defineInRange("permissionLevel", 2, 0, 4);
        COMMAND_SUGGEST_REACH = builder.comment("TAB 补全里“你正看着的那个点”时，射线打多远（格）。默认 32。")
                .defineInRange("suggestReach", 32.0D, 1.0D, 256.0D);
        builder.pop();

        builder.comment("锚：某一种锚自己的参数。");
        builder.push("anchor");
        builder.push("dev_voidtest");
        VOID_TEST_HOLD_TICKS = builder.comment("dev_voidtest 被瞄上以后要连盯多少 tick 才触发（20 tick = 1 秒）。默认 40。")
                .defineInRange("holdTicks", 40, 1, 600);
        VOID_TEST_DAMAGE = builder.comment("dev_voidtest 触发时给多少点虚空伤害（10 点 = 5 颗心）。默认 10。")
                .defineInRange("damage", 10.0D, 0.0D, 1000.0D);
        builder.pop(2);

        builder.comment("调试：日志刷不刷屏。");
        builder.push("debug");
        DEBUG_VERBOSE_LOG = builder.comment("把调试信息打成 info 日志；关掉以后这些只进 debug，不刷屏。默认 true。")
                .define("verboseLog", true);
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private WhimConfig()
    {
    }

    /**
     * 这条灵感用的瞄准锥半角（度）。
     * <p>
     * 在 {@code [aim] cone} 里点名过的元素用它自己那份，其余（绝大多数）一律用 {@code [aim] defaultConeDegrees}
     * —— 所以改默认值能一次改掉全部，只有你专门写过的才例外。
     */
    public static double aimConeDegrees(ResourceLocation element)
    {
        for (String entry : AIM_CONE_OVERRIDES.get())
        {
            int equal = entry.indexOf('=');

            if (equal < 0 || !element.equals(ResourceLocation.tryParse(entry.substring(0, equal).trim())))
            {
                continue;
            }

            try
            {
                double degrees = Double.parseDouble(entry.substring(equal + 1).trim());

                return Math.min(180.0D, Math.max(0.0D, degrees));
            }
            catch (NumberFormatException ignored)
            {
                // 角度写坏了就当没写过这一条，退回默认。
            }
        }

        return DEFAULT_CONE.get();
    }

    /** 隔着方块算不算瞄上。 */
    public static boolean aimThroughWalls()
    {
        return AIM_THROUGH_WALLS.get();
    }

    /** 自由视角是按住还是切换，默认按住。 */
    public static FreeLookMode freeLookMode()
    {
        return FREE_LOOK_MODE.get();
    }

    /** 自由视角松手以后镜头退回人的朝向用多久（毫秒）。 */
    public static int freeLookFadeMillis()
    {
        return FREE_LOOK_FADE.get();
    }

    /** 自由视角转镜头的快慢倍率，1.0 就是和原版一样。 */
    public static double freeLookSensitivity()
    {
        return FREE_LOOK_SENSITIVITY.get();
    }

    /** 自由视角上下最多能看多少度。 */
    public static double freeLookPitchLimitDegrees()
    {
        return FREE_LOOK_PITCH_LIMIT.get();
    }

    /** 自由视角左右最多能离开人的朝向多少度，180 就是不限。 */
    public static double freeLookYawLimitDegrees()
    {
        return FREE_LOOK_YAW_LIMIT.get();
    }

    /** 被瞄上时那层高亮比本体宽出去多少（格）。 */
    public static double renderOutlineWidth()
    {
        return RENDER_OUTLINE.get();
    }

    /** dev 元素：一面切成几格棋盘。 */
    public static int renderDevCheckerCells()
    {
        return RENDER_DEV_CHECKER_CELLS.get();
    }

    /** 六面分不分原版那套明暗。 */
    public static boolean renderFaceShade()
    {
        return RENDER_FACE_SHADE.get();
    }

    /** dev 元素：棋盘的第一色。 */
    public static float[] renderDevColorElement()
    {
        return color(RENDER_DEV_COLOR_ELEMENT.get(), FALLBACK_DEV_ELEMENT);
    }

    /** dev 元素：棋盘的第二色。 */
    public static float[] renderDevColorElementAlt()
    {
        return color(RENDER_DEV_COLOR_ELEMENT_ALT.get(), FALLBACK_DEV_ELEMENT_ALT);
    }

    /** 被瞄上时那层的颜色。 */
    public static float[] renderColorAimed()
    {
        return color(RENDER_COLOR_AIMED.get(), FALLBACK_AIMED);
    }

    /** summon 不写 tick 时灵感活多久；-1 是永久。 */
    public static int defaultLifetimeTicks()
    {
        return DEFAULT_LIFETIME_TICKS.get();
    }

    /** summon 不写锚类型时默认用哪个锚，写的是路径名。 */
    public static String defaultAnchor()
    {
        return DEFAULT_ANCHOR.get();
    }

    /** 谁能用 /glimmerwhim。 */
    public static int commandPermissionLevel()
    {
        return COMMAND_PERMISSION_LEVEL.get();
    }

    /** TAB 补全里“你正看着的那个点”时射线打多远。 */
    public static double commandSuggestReach()
    {
        return COMMAND_SUGGEST_REACH.get();
    }

    /** dev_voidtest 被瞄上以后要连盯多少 tick 才触发。 */
    public static int voidTestHoldTicks()
    {
        return VOID_TEST_HOLD_TICKS.get();
    }

    /** dev_voidtest 触发时给多少点虚空伤害。 */
    public static double voidTestDamage()
    {
        return VOID_TEST_DAMAGE.get();
    }

    /** 调试信息打不打成 info 日志。 */
    public static boolean verboseLog()
    {
        return DEBUG_VERBOSE_LOG.get();
    }

    /**
     * 把 {@code RRGGBB} 或 {@code AARRGGBB}（前面可以带 {@code #}）读成 RGBA 四个分量。
     * 读不出来就用兜底那个，不报错、不崩。
     */
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
