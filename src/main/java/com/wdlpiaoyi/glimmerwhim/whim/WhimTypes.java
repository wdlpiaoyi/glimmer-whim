package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/**
 * 元素类型注册表。
 * <p>
 * 一个类型在这里登记它认哪些参数。参数在命令那一侧校验完才进 {@link WhimData}，
 * 客户端收到的已经是过完这一关的。
 */
public final class WhimTypes
{
    private static final Map<ResourceLocation, Map<String, WhimParam>> TYPES = new LinkedHashMap<>();

    static
    {
        register(Whim.DEV_ELEMENT,
                WhimParam.choice("shape", "quad", "quad", "cube"),
                WhimParam.positiveNumber("size", "0.25"));
    }

    private WhimTypes()
    {
    }

    public static void register(ResourceLocation element, WhimParam... params)
    {
        Map<String, WhimParam> map = new LinkedHashMap<>();

        for (WhimParam param : params)
        {
            map.put(param.name(), param);
        }

        TYPES.put(element, map);
    }

    /** 登记过的元素类型，顺序就是登记顺序。 */
    public static Set<ResourceLocation> elements()
    {
        return Collections.unmodifiableSet(TYPES.keySet());
    }

    /** 这个类型认的参数，顺序就是登记顺序。 */
    public static Collection<WhimParam> params(ResourceLocation element)
    {
        Map<String, WhimParam> params = TYPES.get(element);

        return params == null ? List.of() : params.values();
    }

    public static WhimParam param(ResourceLocation element, String name)
    {
        Map<String, WhimParam> params = TYPES.get(element);

        return params == null ? null : params.get(name);
    }

    /** 没写就用默认值。 */
    public static String text(ResourceLocation element, WhimData data, String name)
    {
        WhimParam param = param(element, name);

        return data.get(name).orElse(param == null ? "" : param.defaultValue());
    }

    /** 读一个数参数；读不成数就退回默认值。 */
    public static double number(ResourceLocation element, WhimData data, String name)
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

        WhimParam param = param(element, name);

        return param == null ? 0.0D : Double.parseDouble(param.defaultValue());
    }

    /**
     * 校验一条灵感的数据：不认识的名字、不合法值都抛 {@link IllegalArgumentException}。
     *
     * @return 只含这个类型认识的那些参数
     */
    public static WhimData parse(ResourceLocation element, WhimData raw)
    {
        Map<String, WhimParam> params = TYPES.get(element);

        if (params == null || raw.isEmpty())
        {
            return raw;
        }

        Map<String, String> values = new LinkedHashMap<>();

        raw.values().forEach((name, value) ->
        {
            WhimParam param = params.get(name);

            if (param == null)
            {
                throw new IllegalArgumentException(element.getPath() + " 没有参数: " + name);
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
