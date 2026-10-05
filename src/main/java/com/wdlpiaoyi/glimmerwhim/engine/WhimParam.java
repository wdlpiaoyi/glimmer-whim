package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceLocation;

public final class WhimParam
{
    private final String name;
    private final String defaultValue;
    private final Predicate<String> valid;
    private final String hint;
    private final List<String> choices;

    private WhimParam(String name, String defaultValue, Predicate<String> valid, String hint, List<String> choices)
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.valid = valid;
        this.hint = hint;
        this.choices = choices;
    }

    public static WhimParam choice(String name, String defaultValue, String... choices)
    {
        List<String> list = List.of(choices);

        return new WhimParam(name, defaultValue, list::contains, String.join(" 或 ", list), list);
    }

    // 接受任意正 double（排除 NaN/Inf）
    public static WhimParam positiveNumber(String name, String defaultValue)
    {
        return new WhimParam(name, defaultValue, WhimParam::isPositive, "正数", List.of());
    }

    // -1 或 1..Integer.MAX_VALUE
    public static WhimParam lifetime(String defaultValue)
    {
        return new WhimParam(Whim.LIFETIME, defaultValue, WhimParam::isLifetime, "正整数或 -1（永久）", List.of());
    }

    // 取值词汇与校验都来自 WhimVisibility
    public static WhimParam player(String name, String defaultValue)
    {
        return new WhimParam(name, defaultValue, WhimVisibility::valid, WhimVisibility.HINT, WhimVisibility.MODES);
    }

    // 轨迹样式 id，须是 WhimTraces 已登记的（含运行期登记）
    public static WhimParam trace(String name, String defaultValue)
    {
        return named(name, defaultValue, WhimTraces::resolve, "已登记的轨迹样式", WhimTraces.names());
    }

    // 外观 id，须是 WhimShapes 已登记的（含运行期登记）
    public static WhimParam shape(String name, String defaultValue)
    {
        return named(name, defaultValue, WhimShapes::resolve, "已登记的外观", WhimShapes.names());
    }

    // 命中体积 id，须是 WhimHits 已登记的（含运行期登记）
    public static WhimParam hitVolume(String name, String defaultValue)
    {
        return named(name, defaultValue, WhimHits::resolve, "已登记的命中体积", WhimHits.names());
    }

    // 取值是某个具名清单里的 id；允许省略命名空间
    private static WhimParam named(String name, String defaultValue, Function<String, Optional<ResourceLocation>> resolve,
            String hint, List<String> choices)
    {
        return new WhimParam(name, defaultValue, value -> resolve.apply(value).isPresent(), hint, choices);
    }

    private static boolean isPositive(String value)
    {
        try
        {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0.0D;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    // 先用 long 解析再判范围，避免 int 溢出
    private static boolean isLifetime(String value)
    {
        try
        {
            long parsed = Long.parseLong(value);
            return parsed == -1L || parsed > 0L && parsed <= Integer.MAX_VALUE;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public String name()
    {
        return this.name;
    }

    public String defaultValue()
    {
        return this.defaultValue;
    }

    public String hint()
    {
        return this.hint;
    }

    public List<String> choices()
    {
        return this.choices;
    }

    public boolean valid(String value)
    {
        return this.valid.test(value);
    }
}
