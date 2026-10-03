package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WhimParams
{
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

    public static WhimParams lifetime(int defaultValue)
    {
        return new WhimParams(List.of(WhimParam.lifetime(Integer.toString(defaultValue))));
    }

    public Collection<WhimParam> all()
    {
        return this.params.values();
    }

    public WhimParam get(String name)
    {
        return this.params.get(name);
    }

    public WhimParams plus(WhimParams other)
    {
        if (other.params.isEmpty())
        {
            return this;
        }

        if (this.params.isEmpty())
        {
            return other;
        }

        Map<String, WhimParam> merged = new LinkedHashMap<>(this.params);

        merged.putAll(other.params);

        return new WhimParams(merged.values());
    }

    public String text(WhimData data, String name)
    {
        WhimParam param = get(name);

        return data.get(name).orElse(param == null ? "" : param.defaultValue());
    }

    public String text(WhimData data, String name, String fallback)
    {
        return get(name) == null ? fallback : text(data, name);
    }

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
            }
        }

        WhimParam param = get(name);

        return param == null ? 0.0D : Double.parseDouble(param.defaultValue());
    }

    public double number(WhimData data, String name, double fallback)
    {
        return get(name) == null ? fallback : number(data, name);
    }

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
                        ? "该灵感不接受参数: " + name
                        : "未知参数: " + name + "（可用: " + String.join(", ", this.params.keySet()) + "）");
            }

            if (!param.valid(value))
            {
                throw new IllegalArgumentException("参数 " + name + " 的值无效: " + value + "（只能为 " + param.hint() + "）");
            }

            values.put(name, value);
        });

        return WhimData.of(values);
    }
}
