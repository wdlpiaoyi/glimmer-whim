package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一份参数表：谁认哪些 {@code {名字:值}}，谁就拿着它。
 * <p>
 * 参数在命令那一侧校验完才进 {@link WhimData}，客户端收到的已经是过完这一关的。
 */
public final class WhimParams
{
    /** 一个参数都不认。 */
    public static final WhimParams NONE = new WhimParams(List.of());

    private final Map<String, WhimParam> params;

    private WhimParams(Collection<WhimParam> params)
    {
        Map<String, WhimParam> map = new LinkedHashMap<>();

        for (WhimParam param : params)
        {
            map.put(param.name(), param);
        }

        this.params = Collections.unmodifiableMap(map);
    }

    public static WhimParams of(WhimParam... params)
    {
        return new WhimParams(List.of(params));
    }

    /** 表里的参数，顺序就是声明顺序。 */
    public Collection<WhimParam> all()
    {
        return this.params.values();
    }

    public WhimParam get(String name)
    {
        return this.params.get(name);
    }

    /** 没写就用声明里的默认值。 */
    public String text(WhimData data, String name)
    {
        WhimParam param = get(name);

        return data.get(name).orElse(param == null ? "" : param.defaultValue());
    }

    /** 读一个数参数；读不成数就退回默认值。 */
    public double number(WhimData data, String name)
    {
        String value = data.get(name).orElse(null);

        if (value != null)
        {
            try
            {
                return Double.parseDouble(value);
            }
            catch (NumberFormatException e)
            {
                // 命令那侧校验过了，走到这儿说明数据不是本模组发的 —— 退回默认值。
            }
        }

        WhimParam param = get(name);

        return param == null ? 0.0D : Double.parseDouble(param.defaultValue());
    }

    /**
     * 校验一条灵感的数据：不认识的名字、不合法值都抛 {@link IllegalArgumentException}。
     *
     * @return 只含这张表认的那些参数
     */
    public WhimData parse(WhimData raw)
    {
        if (raw.isEmpty())
        {
            return raw;
        }

        Map<String, String> values = new LinkedHashMap<>();

        raw.values().forEach((name, value) ->
        {
            WhimParam param = this.params.get(name);

            if (param == null)
            {
                throw new IllegalArgumentException(this.params.isEmpty()
                        ? "这种锚不收参数: " + name
                        : "没有这个参数: " + name + "（可用: " + String.join(", ", this.params.keySet()) + "）");
            }

            if (!param.valid(value))
            {
                throw new IllegalArgumentException("参数 " + name + " 的值不对: " + value + "（只能是" + param.hint() + "）");
            }

            values.put(name, value);
        });

        return WhimData.of(values);
    }
}
