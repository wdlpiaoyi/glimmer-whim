package com.wdlpiaoyi.glimmerwhim.anchor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class WhimAnchors
{
    @FunctionalInterface
    public interface AnchorDataParser
    {
        WhimAnchor parse(CommandSourceStack source, String data);
    }

    private record AnchorType(Function<FriendlyByteBuf, WhimAnchor> reader, AnchorDataParser parser,
            Function<CommandSourceStack, Collection<String>> suggestions, String hint)
    {
    }

    private static final Map<ResourceLocation, AnchorType> TYPES = new LinkedHashMap<>();

    static
    {
        register(RayAnchor.TYPE, RayAnchor::read, RayAnchor::parse, RayAnchor::suggestData,
                "dx dy dz [distance]；~ 表示视线方向");

        register(PosAnchor.TYPE, PosAnchor::read, PosAnchor::parse, PosAnchor::suggestData,
                "x y z；~ 表示当前位置");
    }

    private WhimAnchors()
    {
    }

    public static void register(ResourceLocation type, Function<FriendlyByteBuf, WhimAnchor> reader,
            AnchorDataParser parser, Function<CommandSourceStack, Collection<String>> suggestions, String hint)
    {
        TYPES.put(type, new AnchorType(reader, parser, suggestions, hint));
    }

    public static Collection<ResourceLocation> types()
    {
        return List.copyOf(TYPES.keySet());
    }

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

    public static Collection<String> suggestData(ResourceLocation type, CommandSourceStack source)
    {
        AnchorType anchor = type == null ? null : TYPES.get(type);

        return anchor == null ? List.of() : anchor.suggestions().apply(source);
    }

    public static String hint(ResourceLocation type)
    {
        AnchorType anchor = type == null ? null : TYPES.get(type);

        return anchor == null ? "" : anchor.hint();
    }

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
