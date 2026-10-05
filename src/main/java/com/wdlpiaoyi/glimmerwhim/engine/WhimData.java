package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;

public final class WhimData
{
    public static final WhimData EMPTY = new WhimData(Map.of());

    // 匹配 {名称:值} 组；锚数据是组外的剩余文本
    private static final Pattern GROUP = Pattern.compile("\\{([^{}]*)\\}");

    // 组内以逗号/分号分隔多个 名称:值；命令补全也复用它
    public static final Pattern SEPARATOR = Pattern.compile("[,;]");

    private final Map<String, String> values;

    private WhimData(Map<String, String> values)
    {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static WhimData of(Map<String, String> values)
    {
        return values.isEmpty() ? EMPTY : new WhimData(values);
    }

    public static WhimData of(String name, String value)
    {
        return of(Map.of(name, value));
    }

    // 不可变；返回覆盖/追加一个键的新实例
    public WhimData with(String name, String value)
    {
        Map<String, String> copy = new LinkedHashMap<>(this.values);
        copy.put(name, value);
        return new WhimData(copy);
    }

    public Optional<String> get(String name)
    {
        return Optional.ofNullable(this.values.get(name));
    }

    public Map<String, String> values()
    {
        return this.values;
    }

    public boolean isEmpty()
    {
        return this.values.isEmpty();
    }

    // 区间写法 {名称:最小值..最大值}：灵感成形时在区间内取一个随机值
    public static final String RANGE = "..";

    public static String range(int min, int max)
    {
        return min + RANGE + max;
    }

    public static String range(double min, double max)
    {
        return format(Math.min(min, max)) + RANGE + format(Math.max(min, max));
    }

    // 逐项把区间值换成随机值，非区间值原样保留；用于生成时给参数一点浮动
    public WhimData roll(RandomSource random)
    {
        Map<String, String> rolled = new LinkedHashMap<>();

        this.values.forEach((name, value) -> rolled.put(name, randomise(value, random)));

        return new WhimData(rolled);
    }

    private static String randomise(String text, RandomSource random)
    {
        int index = text.indexOf(RANGE);

        // 分隔符必须在两侧都有内容，否则不是区间
        if (index <= 0 || index + RANGE.length() >= text.length())
        {
            return text;
        }

        String low = text.substring(0, index);
        String high = text.substring(index + RANGE.length());

        try
        {
            // 两端都是整数就取整数，保证 lifetime 这类整数参数仍可解析
            if (isInteger(low) && isInteger(high))
            {
                int min = Integer.parseInt(low);
                int max = Integer.parseInt(high);

                if (min > max)
                {
                    int swap = min;
                    min = max;
                    max = swap;
                }

                return Integer.toString(min + random.nextInt(max - min + 1));
            }

            double min = Double.parseDouble(low);
            double max = Double.parseDouble(high);

            if (!Double.isFinite(min) || !Double.isFinite(max))
            {
                return text;
            }

            return format(min + random.nextDouble() * (max - min));
        }
        catch (NumberFormatException e)
        {
            return text;
        }
    }

    private static boolean isInteger(String text)
    {
        int start = text.startsWith("-") ? 1 : 0;

        if (text.length() <= start)
        {
            return false;
        }

        for (int i = start; i < text.length(); i++)
        {
            if (!Character.isDigit(text.charAt(i)))
            {
                return false;
            }
        }

        return true;
    }

    // 最多三位小数并去掉尾随零，让写进 data 的数字短一些
    private static String format(double value)
    {
        String text = String.format(Locale.ROOT, "%.3f", value);

        if (text.indexOf('.') < 0)
        {
            return text;
        }

        text = text.replaceAll("0+$", "");

        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    // 拆出所有 {名称:值} 组，剩余文本压掉多余空白后作为锚数据
    public static Split split(String tail)
    {
        String text = tail == null ? "" : tail;
        Map<String, String> values = new LinkedHashMap<>();
        StringBuilder rest = new StringBuilder();
        Matcher matcher = GROUP.matcher(text);
        int end = 0;

        while (matcher.find())
        {
            rest.append(text, end, matcher.start()).append(' ');
            put(values, matcher.group(1));
            end = matcher.end();
        }

        rest.append(text.substring(end));
        String anchorData = rest.toString().trim().replaceAll("\\s+", " ");

        // 组外仍残留括号说明 {...} 未闭合
        if (anchorData.indexOf('{') >= 0 || anchorData.indexOf('}') >= 0)
        {
            throw new IllegalArgumentException("参数格式应为 {名称:值}，此处括号不匹配: " + text);
        }

        return new Split(of(values), anchorData);
    }

    private static void put(Map<String, String> values, String group)
    {
        for (String chunk : SEPARATOR.split(group))
        {
            String pair = chunk.trim();

            if (pair.isEmpty())
            {
                continue;
            }

            int cut = cut(pair);

            if (cut < 0)
            {
                throw new IllegalArgumentException("参数格式应为 名称:值，缺少分隔符: " + pair);
            }

            String name = pair.substring(0, cut).trim();

            if (name.isEmpty())
            {
                throw new IllegalArgumentException("参数缺少名称: " + pair);
            }

            values.put(name, pair.substring(cut + 1).trim());
        }
    }

    // 冒号与等号都算分隔符，取更靠前者；命令补全也复用它
    public static int cut(String pair)
    {
        int colon = pair.indexOf(':');
        int equals = pair.indexOf('=');

        if (colon < 0)
        {
            return equals;
        }

        return equals < 0 ? colon : Math.min(colon, equals);
    }

    // 网络编码：varInt 数量 + 若干 key/value，保持插入顺序
    public void write(FriendlyByteBuf buf)
    {
        buf.writeVarInt(this.values.size());

        this.values.forEach((name, value) ->
        {
            buf.writeUtf(name);
            buf.writeUtf(value);
        });
    }

    public static WhimData read(FriendlyByteBuf buf)
    {
        Map<String, String> values = new LinkedHashMap<>();

        for (int i = buf.readVarInt(); i > 0; i--)
        {
            values.put(buf.readUtf(), buf.readUtf());
        }

        return of(values);
    }

    // anchorData 为不属于任何参数组的原始锚文本
    public record Split(WhimData data, String anchorData)
    {
    }

    @Override
    public String toString()
    {
        StringBuilder text = new StringBuilder();

        this.values.forEach((name, value) ->
        {
            if (text.length() > 0)
            {
                text.append(' ');
            }

            text.append('{').append(name).append(':').append(value).append('}');
        });

        return text.toString();
    }
}
