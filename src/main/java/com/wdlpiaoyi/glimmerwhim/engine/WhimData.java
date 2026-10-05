package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.FriendlyByteBuf;

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
