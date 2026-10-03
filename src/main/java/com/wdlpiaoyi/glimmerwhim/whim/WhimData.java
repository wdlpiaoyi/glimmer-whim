package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 一条灵感自己带的数据，就是命令里写的那些 {@code {名字:值}}。
 * <p>
 * 有哪些名字、值合不合法，由它那种锚说了算；这里只管装。
 */
public final class WhimData
{
    public static final WhimData EMPTY = new WhimData(Map.of());

    /** 命令尾巴里的一个花括号。 */
    private static final Pattern GROUP = Pattern.compile("\\{([^{}]*)\\}");

    /** 一个花括号里分隔多个参数。 */
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

    /** 从命令尾巴里把 {@code {名字:值}} 全抠出来，剩下的当锚数据。 */
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
            throw new IllegalArgumentException("参数得写成 {名字:值}，这里多了半个括号: " + text);
        }

        return new Split(of(values), anchorData);
    }

    /** 拆一个花括号里的内容，{@code {shape:cube}} 和 {@code {shape:cube,size:1}} 都收。 */
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
                throw new IllegalArgumentException("参数得写成 名字:值，这里没写分隔: " + pair);
            }

            String name = pair.substring(0, cut).trim();

            if (name.isEmpty())
            {
                throw new IllegalArgumentException("参数没写名字: " + pair);
            }

            values.put(name, pair.substring(cut + 1).trim());
        }
    }

    /** 名字和值之间的那个分隔符，冒号和等号都认。 */
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

    /** 抠完参数之后剩下的那段。 */
    public record Split(WhimData data, String anchorData)
    {
    }

    /** 写回 {@code {名字:值}} 的样子。 */
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
