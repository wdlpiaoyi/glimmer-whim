package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.DevPosAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.DevRayAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.DevRemoveOnHighlightAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.DevVoidTestAnchor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 锚类型注册表。
 * <p>
 * 一种锚在这里登记它自己的事：怎么把包读回成 {@link WhimAnchor}、怎么从命令参数造一条、
 * {@code anchordata} 补全出什么、{@code anchordata} 该怎么写（用法提示用），
 * 以及认哪些 {@code {名字:值}} 参数。
 */
public final class WhimAnchors
{
    /** 从命令参数造一条锚。{@code data} 可能是 null，表示玩家没写。 */
    @FunctionalInterface
    public interface AnchorDataParser
    {
        WhimAnchor parse(CommandSourceStack source, String data);
    }

    private record AnchorType(Function<FriendlyByteBuf, WhimAnchor> reader, AnchorDataParser parser,
            Function<CommandSourceStack, Collection<String>> suggestions, String hint, WhimParams params)
    {
    }

    private static final Map<ResourceLocation, AnchorType> TYPES = new LinkedHashMap<>();

    static
    {
        register(DevRayAnchor.TYPE, DevRayAnchor::read, DevRayAnchor::parse, DevRayAnchor::suggestData, "dx dy dz，~ 取视线",
                WhimParam.choice("shape", "quad", "quad", "cube"),
                WhimParam.positiveNumber("size", "1"),
                WhimParam.positiveNumber("distance", "8"));

        register(DevPosAnchor.TYPE, DevPosAnchor::read, DevPosAnchor::parse, DevPosAnchor::suggestData, "x y z，~ 取当前位置",
                WhimParam.choice("shape", "cube", "quad", "cube"),
                WhimParam.positiveNumber("size", "1"));

        // 什么都不收：没锚数据、没参数，就在原点，被瞄上就没。
        register(DevRemoveOnHighlightAnchor.TYPE, DevRemoveOnHighlightAnchor::read, DevRemoveOnHighlightAnchor::parse,
                DevRemoveOnHighlightAnchor::suggestData, "不收锚数据，就在 0 0 0");

        // 同上，但条件是"瞄上、松开、再瞄上并盯住 40 tick"，到了发一条 test 再给 10 点虚空伤害。
        register(DevVoidTestAnchor.TYPE, DevVoidTestAnchor::read, DevVoidTestAnchor::parse,
                DevVoidTestAnchor::suggestData, "不收锚数据，就在 0 0 0");
    }

    private WhimAnchors()
    {
    }

    public static void register(ResourceLocation type, Function<FriendlyByteBuf, WhimAnchor> reader,
            AnchorDataParser parser, Function<CommandSourceStack, Collection<String>> suggestions, String hint,
            WhimParam... params)
    {
        TYPES.put(type, new AnchorType(reader, parser, suggestions, hint, WhimParams.of(params)));
    }

    /** 已登记的锚类型，顺序就是登记顺序。 */
    public static Collection<ResourceLocation> types()
    {
        return List.copyOf(TYPES.keySet());
    }

    /** 只写路径不写命名空间时，缺省补本模组的；写坏了返回空。 */
    public static Optional<ResourceLocation> resolve(String text)
    {
        try
        {
            return Optional.of(text.indexOf(':') >= 0
                    ? ResourceLocation.parse(text)
                    : ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, text));
        }
        catch (RuntimeException e)
        {
            return Optional.empty();
        }
    }

    /** {@code anchordata} 的补全候选。没有建议就返回空表。 */
    public static Collection<String> suggestData(ResourceLocation type, CommandSourceStack source)
    {
        AnchorType anchor = type == null ? null : TYPES.get(type);

        return anchor == null ? List.of() : anchor.suggestions().apply(source);
    }

    /** {@code anchordata} 该怎么写。用法提示用。 */
    public static String hint(ResourceLocation type)
    {
        AnchorType anchor = type == null ? null : TYPES.get(type);

        return anchor == null ? "" : anchor.hint();
    }

    /** 这种锚认的 {@code {名字:值}}。没登记过的锚一个都不认。 */
    public static WhimParams params(ResourceLocation type)
    {
        AnchorType anchor = type == null ? null : TYPES.get(type);

        return anchor == null ? WhimParams.NONE : anchor.params();
    }

    /** 从命令参数造一条锚。 */
    public static WhimAnchor create(ResourceLocation type, CommandSourceStack source, String data)
    {
        AnchorType anchor = TYPES.get(type);

        if (anchor == null)
        {
            throw new IllegalArgumentException("未知的锚类型: " + type);
        }

        return anchor.parser().parse(source, data);
    }

    public static void write(FriendlyByteBuf buf, WhimAnchor anchor)
    {
        buf.writeResourceLocation(anchor.type());
        anchor.write(buf);
    }

    public static WhimAnchor read(FriendlyByteBuf buf)
    {
        ResourceLocation type = buf.readResourceLocation();
        AnchorType anchor = TYPES.get(type);

        if (anchor == null)
        {
            throw new IllegalArgumentException("未知的锚类型: " + type);
        }

        return anchor.reader().apply(buf);
    }
}
