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

    private static final Pattern GROUP = Pattern.compile("\\{([^{}]*)\\}");

    private static final Pattern SEPARATOR = Pattern.compile("[,;]");

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

    private static int cut(String pair)
    {
        int colon = pair.indexOf(':');
        int equals = pair.indexOf('=');

        if (colon < 0)
        {
            return equals;
        }

        return equals < 0 ? colon : Math.min(colon, equals);
    }

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
