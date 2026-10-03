package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 世界里一个点，会一直待在那儿。
 * <p>
 * 调试用。
 */
public final class DevPosAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_pos");

    private static final String WRONG = "锚数据需要三个坐标: x y z，任一个写 ~ 就是执行者的位置";

    private final Vec3 position;

    public DevPosAnchor(Vec3 position)
    {
        this.position = position;
    }

    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeDouble(this.position.x);
        buf.writeDouble(this.position.y);
        buf.writeDouble(this.position.z);
    }

    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick)
    {
        return Optional.of(this.position);
    }

    public static DevPosAnchor read(FriendlyByteBuf buf)
    {
        return new DevPosAnchor(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    /** {@code anchordata} 的格式就是这三个坐标。补全给执行者站的地方，和他正看着的那个点。 */
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            return List.of("~ ~ ~");
        }

        Vec3 hit = player.pick(32.0D, 0.0F, false).getLocation();

        return List.of("~ ~ ~", String.format(Locale.ROOT, "%.3f %.3f %.3f", hit.x, hit.y, hit.z));
    }

    /**
     * 从命令参数造一个。
     *
     * @param data 三个坐标 {@code x y z}，用的就是 {@code /summon} 那套：{@code ~} 是执行者的位置，
     *             {@code ~5} 是执行者 +5，三个都是整数的绝对坐标取方块中心；
     *             不给就是执行者站的地方
     */
    public static DevPosAnchor parse(CommandSourceStack source, String data)
    {
        if (data == null || data.isBlank())
        {
            return new DevPosAnchor(source.getPosition());
        }

        StringReader reader = new StringReader(data);

        try
        {
            Coordinates coordinates = Vec3Argument.vec3().parse(reader);

            if (reader.canRead())
            {
                throw new IllegalArgumentException(WRONG);
            }

            return new DevPosAnchor(coordinates.getPosition(source));
        }
        catch (CommandSyntaxException e)
        {
            throw new IllegalArgumentException(WRONG);
        }
    }
}
