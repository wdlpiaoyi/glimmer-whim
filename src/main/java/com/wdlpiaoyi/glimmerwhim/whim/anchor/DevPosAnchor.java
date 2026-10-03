package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;

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
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params)
    {
        return Optional.of(this.position);
    }

    public static DevPosAnchor read(FriendlyByteBuf buf)
    {
        return new DevPosAnchor(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    /** {@code anchordata} 的格式就是这三个坐标。补全给执行者站的地方、他正看着的那个点，和那个点所在方块的整数坐标。 */
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            return List.of("~ ~ ~");
        }

        Vec3 hit = player.pick(WhimConfig.commandSuggestReach(), 0.0F, false).getLocation();

        return List.of("~ ~ ~",
                String.format(Locale.ROOT, "%.3f %.3f %.3f", hit.x, hit.y, hit.z),
                String.format(Locale.ROOT, "%d %d %d",
                        (int) Math.floor(hit.x), (int) Math.floor(hit.y), (int) Math.floor(hit.z)));
    }

    /**
     * 从命令参数造一个。
     *
     * @param data 三个坐标 {@code x y z}，跟 {@code /summon} 同一套写法：{@code ~} 是执行者的位置、
     *             {@code ~5} 是执行者 +5；写成整数的绝对坐标取它所在方块的中心；
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
            Coordinates coordinates = Vec3Argument.vec3(false).parse(reader);

            if (reader.canRead())
            {
                throw new IllegalArgumentException(WRONG);
            }

            return new DevPosAnchor(centre(coordinates.getPosition(source), data));
        }
        catch (CommandSyntaxException e)
        {
            throw new IllegalArgumentException(WRONG);
        }
    }

    /**
     * 写成整数的绝对坐标，取它所在方块的中心。
     * <p>
     * 原版那套只这么对待 x 和 z，y 留在整数上 —— 那样一个方块高的东西会矮半格，所以这里三个轴一起对齐。
     * {@code ~}、{@code ~5}、带小数点的都原样留着。
     */
    private static Vec3 centre(Vec3 position, String data)
    {
        String[] parts = data.trim().split("\\s+");

        if (parts.length != 3)
        {
            return position;
        }

        double[] values = {position.x, position.y, position.z};

        for (int i = 0; i < 3; i++)
        {
            if (isWhole(parts[i]))
            {
                values[i] = Math.floor(values[i]) + 0.5D;
            }
        }

        return new Vec3(values[0], values[1], values[2]);
    }

    private static boolean isWhole(String token)
    {
        return !token.startsWith("~") && !token.startsWith("^") && token.indexOf('.') < 0;
    }
}
