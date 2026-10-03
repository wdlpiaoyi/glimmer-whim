package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 一个世界方向，没有距离。
 * <p>
 * 调试用。
 */
public final class RayAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "ray");

    private final Vec3 direction;

    public RayAnchor(Vec3 direction)
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
    public Optional<Vec3> direction(Level level, Vec3 eye, float partialTick)
    {
        return Optional.of(this.direction);
    }

    public static RayAnchor read(FriendlyByteBuf buf)
    {
        return new RayAnchor(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
    }

    /** {@code anchordata} 的格式就是这三个数。补全直接给执行者当前视线，省得自己算。 */
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        Vec3 look = player == null ? new Vec3(0.0D, 0.0D, 1.0D) : player.getLookAngle();

        return List.of(String.format(Locale.ROOT, "%.3f %.3f %.3f", look.x, look.y, look.z));
    }

    /**
     * 从命令参数造一个。
     *
     * @param data 三个数 {@code dx dy dz}；不给就是执行者正前方
     */
    public static RayAnchor parse(CommandSourceStack source, String data)
    {
        if (data == null || data.isBlank())
        {
            ServerPlayer player = source.getPlayer();

            if (player == null)
            {
                throw new IllegalArgumentException("锚数据省略时取执行者视线，当前执行者不是玩家");
            }

            return new RayAnchor(player.getLookAngle());
        }

        String[] parts = data.trim().split("\\s+");

        if (parts.length != 3)
        {
            throw new IllegalArgumentException("锚数据需要三个数: dx dy dz");
        }

        Vec3 direction;

        try
        {
            direction = new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("锚数据需要三个数: dx dy dz");
        }

        if (direction.lengthSqr() < 1.0E-8D)
        {
            throw new IllegalArgumentException("方向不能是零向量");
        }

        return new RayAnchor(direction);
    }
}
