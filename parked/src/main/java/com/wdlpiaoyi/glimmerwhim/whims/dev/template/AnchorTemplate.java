package com.wdlpiaoyi.glimmerwhim.whims.dev.template;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 锚扩展点模板：服务端权威锚，负责网络编解码并解析出世界坐标。
// 复制到 anchor/ 后，在 anchor/WhimAnchors.java 的 ANCHORS 列表加一行（含 reader/parser/suggestions/hint）：
//   new AnchorType(AnchorTemplate.TYPE, AnchorTemplate::read, AnchorTemplate::parse,
//           AnchorTemplate::suggestData, "dx dy dz；相对眼位的偏移")
// 本类自身不注册。
public final class AnchorTemplate implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID,
            "template_offset");

    private static final String WRONG = "锚数据需要三个偏移: dx dy dz（可含小数，用空格分隔）";

    // 相对眼位的固定偏移（格）
    private final Vec3 offset;

    public AnchorTemplate(Vec3 offset)
    {
        this.offset = offset;
    }

    // 锚类型 id，须与 WhimAnchors 登记的一致
    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    // 网络编码：只写解析位置所需的最小数据
    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeDouble(this.offset.x);
        buf.writeDouble(this.offset.y);
        buf.writeDouble(this.offset.z);
    }

    // position 由写进 buffer 的数据还原；partialTick 用于插值，固定偏移用不到
    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick)
    {
        return Optional.of(eye.add(this.offset));
    }

    public static AnchorTemplate read(FriendlyByteBuf buf)
    {
        return new AnchorTemplate(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    // 命令解析：dx dy dz 三个分量；非法时报清楚的中文错误
    public static AnchorTemplate parse(CommandSourceStack source, String data)
    {
        if (data == null || data.isBlank())
        {
            throw new IllegalArgumentException(WRONG);
        }

        String[] parts = data.trim().split("\\s+");

        if (parts.length != 3)
        {
            throw new IllegalArgumentException(WRONG);
        }

        try
        {
            return new AnchorTemplate(new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])));
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException(WRONG);
        }
    }

    // TAB 补全候选；可像 PosAnchor 那样结合 source 给出实时坐标
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of("0 0 8", "0 2 4");
    }
}
