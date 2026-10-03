package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.List;
import java.util.function.Predicate;

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

    public static WhimParam positiveNumber(String name, String defaultValue)
    {
        return new WhimParam(name, defaultValue, WhimParam::isPositive, "正数", List.of());
    }

    public static WhimParam lifetime(String defaultValue)
    {
        return new WhimParam("lifetime", defaultValue, WhimParam::isLifetime, "正整数或 -1（永久）", List.of());
    }

    private static boolean isPositive(String value)
    {
        try
        {
            return Double.parseDouble(value) > 0.0D;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

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
