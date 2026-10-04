package com.wdlpiaoyi.glimmerwhim.anchor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class WhimAnchors
{
    // 命令字符串 -> 锚实例；失败抛 IllegalArgumentException
    @FunctionalInterface
    public interface AnchorDataParser
    {
        WhimAnchor parse(CommandSourceStack source, String data);
    }

    // 一种锚的读取、解析、补全与提示
    private record AnchorType(ResourceLocation type, Function<FriendlyByteBuf, WhimAnchor> reader,
            AnchorDataParser parser, Function<CommandSourceStack, Collection<String>> suggestions, String hint)
    {
    }

    // 注册顺序即补全/展示顺序
    private static final List<AnchorType> ANCHORS = List.of(
            new AnchorType(RayAnchor.TYPE, RayAnchor::read, RayAnchor::parse, RayAnchor::suggestData,
                    "dx dy dz [distance]；~ 表示视线方向"),
            new AnchorType(PosAnchor.TYPE, PosAnchor::read, PosAnchor::parse, PosAnchor::suggestData,
                    "x y z；~ 表示当前位置"));

    private static final Map<ResourceLocation, AnchorType> TYPES = new LinkedHashMap<>();

    static
    {
        for (AnchorType anchor : ANCHORS)
        {
            TYPES.put(anchor.type(), anchor);
        }
    }

    private WhimAnchors()
    {
    }

    public static Collection<ResourceLocation> types()
    {
        return List.copyOf(TYPES.keySet());
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

    // 先写入临时 buffer，再写 type + 长度 + 内容；未知类型可安全跳过
    public static void write(FriendlyByteBuf buf, WhimAnchor anchor)
    {
        FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.buffer());

        anchor.write(payload);

        buf.writeResourceLocation(anchor.type());
        buf.writeVarInt(payload.readableBytes());
        buf.writeBytes(payload);
    }

    // 未知锚类型跳过其字节并返回 null，保证兼容
    public static WhimAnchor read(FriendlyByteBuf buf)
    {
        ResourceLocation type = buf.readResourceLocation();
        int length = buf.readVarInt();
        AnchorType anchor = TYPES.get(type);

        if (anchor == null)
        {
            buf.skipBytes(length);
            GlimmerWhim.log("[Whim] unknown anchor type: {}", type);
            return null;
        }

        return anchor.reader().apply(new FriendlyByteBuf(buf.readBytes(length)));
    }
}
