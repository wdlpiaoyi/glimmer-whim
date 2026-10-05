package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WhimParams
{
    // 空参数集单例；plus 对空集走快捷返回
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

    public static WhimParams visibility()
    {
        return new WhimParams(List.of(WhimParam.player(Whim.VISIBILITY, "all")));
    }

    // 链的轨迹样式：element 决定整条链，modifier 叠在其上
    public static WhimParams traces(String elementDefault, String modifierDefault)
    {
        return new WhimParams(List.of(
                WhimParam.trace(Whim.ELEMENT_TRACE, elementDefault),
                WhimParam.trace(Whim.MODIFIER_TRACE, modifierDefault)));
    }

    public Collection<WhimParam> all()
    {
        return this.params.values();
    }

    public WhimParam get(String name)
    {
        return this.params.get(name);
    }

    // 合并参数集：other 同名覆盖 this
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

    // 只做白名单校验（未知参数/非法值报错）；不填默认，读取时由 text/number 兜底
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
