package com.wdlpiaoyi.glimmerwhim.whim;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.RayAnchor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 锚类型注册表。
 * <p>
 * 一种锚在这里登记三件事：怎么把包读回成 {@link WhimAnchor}、怎么从命令参数造一条、
 * 以及 {@code anchordata} 该写成什么样子（命令补全用）。
 * 加一种锚 = 加一个类 + 在这里注册一行，别处不动。
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
            Function<CommandSourceStack, Collection<String>> suggestions)
    {
    }

    private static final Map<ResourceLocation, AnchorType> TYPES = new LinkedHashMap<>();

    static
    {
        register(RayAnchor.TYPE, RayAnchor::read, RayAnchor::parse, RayAnchor::suggestData);
    }

    private WhimAnchors()
    {
    }

    public static void register(ResourceLocation type, Function<FriendlyByteBuf, WhimAnchor> reader,
            AnchorDataParser parser, Function<CommandSourceStack, Collection<String>> suggestions)
    {
        TYPES.put(type, new AnchorType(reader, parser, suggestions));
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
