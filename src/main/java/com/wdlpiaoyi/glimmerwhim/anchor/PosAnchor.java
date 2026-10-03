package com.wdlpiaoyi.glimmerwhim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PosAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "pos");

    private static final String WRONG = "锚数据需要三个坐标: x y z；任一分量写 ~ 表示执行者的位置";

    private final Vec3 position;

    public PosAnchor(Vec3 position)
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

    public static PosAnchor read(FriendlyByteBuf buf)
    {
        return new PosAnchor(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static Collection<String> suggestData(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            return List.of("~ ~ ~");
        }

        Vec3 hit = player.pick(WhimConfig.commandSuggestReach(), 0.0F, false).getLocation();

        return List.of("~ ~ ~",
                String.format(Locale.ROOT, "%.3f %.3f %.3f", hit.x, hit.y, hit.z));
    }

    public static PosAnchor parse(CommandSourceStack source, String data)
    {
        if (data == null || data.isBlank())
        {
            return new PosAnchor(source.getPosition());
        }

        StringReader reader = new StringReader(data);

        try
        {
            Coordinates coordinates = Vec3Argument.vec3().parse(reader);

            if (reader.canRead())
            {
                throw new IllegalArgumentException(WRONG);
            }

            return new PosAnchor(coordinates.getPosition(source));
        }
        catch (CommandSyntaxException e)
        {
            throw new IllegalArgumentException(WRONG);
        }
    }
}
