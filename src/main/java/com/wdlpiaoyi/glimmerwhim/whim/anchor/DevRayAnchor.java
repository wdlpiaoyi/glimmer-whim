package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DevRayAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_ray");

    private final Vec3 direction;

    public DevRayAnchor(Vec3 direction)
    {
        this.direction = direction.normalize();
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
    }

    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params)
    {
        return Optional.of(eye.add(this.direction.scale(params.number(data, "distance"))));
    }

    public static DevRayAnchor read(FriendlyByteBuf buf)
    {
        return new DevRayAnchor(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
    }

    public static Collection<String> suggestData(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        Vec3 look = player == null ? new Vec3(0.0D, 0.0D, 1.0D) : player.getLookAngle();

        return List.of("~ ~ ~", String.format(Locale.ROOT, "%.3f %.3f %.3f", look.x, look.y, look.z));
    }

    public static DevRayAnchor parse(CommandSourceStack source, String data)
    {
        if (data == null || data.isBlank())
        {
            return new DevRayAnchor(look(source));
        }

        String[] parts = data.trim().split("\\s+");

        if (parts.length != 3)
        {
            throw new IllegalArgumentException("锚数据需要三个数: dx dy dz，或者 ~ ~ ~");
        }

        Vec3 look = null;
        double[] values = new double[3];

        for (int i = 0; i < 3; i++)
        {
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
                throw new IllegalArgumentException("锚数据需要三个数: dx dy dz，或者 ~ ~ ~");
            }
        }

        Vec3 direction = new Vec3(values[0], values[1], values[2]);

        if (direction.lengthSqr() < 1.0E-8D)
        {
            throw new IllegalArgumentException("方向不能是零向量");
        }

        return new DevRayAnchor(direction);
    }

    private static Vec3 look(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            throw new IllegalArgumentException("取视线需要一个玩家来执行");
        }

        return player.getLookAngle();
    }
}
