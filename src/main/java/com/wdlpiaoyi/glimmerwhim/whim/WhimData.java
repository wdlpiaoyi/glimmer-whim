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
 * 有哪些名字、值合不合法，由它所属的元素类型说了算（{@link WhimTypes}）；这里只管装。
 */
public final class WhimData
{
    public static final WhimData EMPTY = new WhimData(Map.of());

    /** 命令尾巴里的一组 {@code {名字:值}}。 */
    private static final Pattern GROUP = Pattern.compile("\\{([^{}:]+):([^{}]*)\\}");

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
            values.put(matcher.group(1).trim(), matcher.group(2).trim());
            end = matcher.end();
        }

        rest.append(text.substring(end));

        return new Split(of(values), rest.toString().trim().replaceAll("\\s+", " "));
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
