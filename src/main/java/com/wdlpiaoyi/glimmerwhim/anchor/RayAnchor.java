package com.wdlpiaoyi.glimmerwhim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RayAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "ray");

    // 默认距离 8 格
    private static final double DEFAULT_DISTANCE = 8.0D;

    private final Vec3 direction;

    private final double distance;

    // 方向归一化，distance 为方块数
    public RayAnchor(Vec3 direction, double distance)
    {
        this.direction = direction.normalize();
        this.distance = distance;
    }

    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeFloat((float) this.direction.x);
        buf.writeFloat((float) this.direction.y);
        buf.writeFloat((float) this.direction.z);
        buf.writeDouble(this.distance);
    }

    // eye + 方向×距离；锚点随玩家视线实时移动
    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick)
    {
        return Optional.of(eye.add(this.direction.scale(this.distance)));
    }

    public static RayAnchor read(FriendlyByteBuf buf)
    {
        return new RayAnchor(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()), buf.readDouble());
    }

    // 建议 ~ ~ ~、当前视线向量与「视线 + 默认距离」
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        Vec3 look = player == null ? new Vec3(0.0D, 0.0D, 1.0D) : player.getLookAngle();

        return List.of("~ ~ ~",
                String.format(Locale.ROOT, "%.3f %.3f %.3f", look.x, look.y, look.z),
                "~ ~ ~ " + (int) DEFAULT_DISTANCE);
    }

    // 空数据 = 视线方向 + 默认距离；分量 ~ 取视线方向而非位置
    public static RayAnchor parse(CommandSourceStack source, String data)
    {
        if (data == null || data.isBlank())
        {
            return new RayAnchor(look(source), DEFAULT_DISTANCE);
        }

        String[] parts = data.trim().split("\\s+");

        if (parts.length != 3 && parts.length != 4)
        {
            throw new IllegalArgumentException("锚数据需要三个分量与可选距离: dx dy dz [distance] 或 ~ ~ ~");
        }

        Vec3 look = null;
        double[] values = new double[3];

        for (int i = 0; i < 3; i++)
        {
            // 分量的 ~ 取视线方向对应轴
            if (parts[i].equals("~"))
            {
                if (look == null)
                {
                    look = look(source);
                }

                values[i] = switch (i)
                {
                    case 0 -> look.x;
                    case 1 -> look.y;
                    default -> look.z;
                };

                continue;
            }

            try
            {
                values[i] = Double.parseDouble(parts[i]);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("锚数据需要三个分量与可选距离: dx dy dz [distance] 或 ~ ~ ~");
            }
        }

        double distance = parts.length == 4 ? distance(parts[3]) : DEFAULT_DISTANCE;
        Vec3 direction = new Vec3(values[0], values[1], values[2]);

        if (direction.lengthSqr() < 1.0E-8D)
        {
            throw new IllegalArgumentException("方向向量不能为零");
        }

        return new RayAnchor(direction, distance);
    }

    // 距离必须为正数
    private static double distance(String text)
    {
        double value;

        try
        {
            value = Double.parseDouble(text);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("距离需要是正数: " + text);
        }

        if (value <= 0.0D)
        {
            throw new IllegalArgumentException("距离需要是正数: " + text);
        }

        return value;
    }

    private static Vec3 look(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            throw new IllegalArgumentException("获取视线方向需要玩家上下文");
        }

        return player.getLookAngle();
    }
}
