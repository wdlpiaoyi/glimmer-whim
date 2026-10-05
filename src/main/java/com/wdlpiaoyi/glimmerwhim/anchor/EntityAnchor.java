package com.wdlpiaoyi.glimmerwhim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 跟随一个实体的锚点：每次解析都取该实体当前的碰撞箱中心
// 客户端只能用实体 id 检索（uuid 是权威身份，两者都要对上），故实体换 id 后无法解析
public final class EntityAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "entity");

    private static final String WRONG = "锚数据需要一个实体选择器，例如 @e[type=pig,limit=1] 或 @p";

    private final UUID entity;

    private final int id;

    public EntityAnchor(UUID entity, int id)
    {
        this.entity = entity;
        this.id = id;
    }

    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeUUID(this.entity);
        buf.writeVarInt(this.id);
    }

    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick)
    {
        Entity found = level.getEntity(this.id);

        if (found == null || !found.getUUID().equals(this.entity))
        {
            return Optional.empty();
        }

        return Optional.of(found.getBoundingBox().getCenter());
    }

    public static EntityAnchor read(FriendlyByteBuf buf)
    {
        return new EntityAnchor(buf.readUUID(), buf.readVarInt());
    }

    // 建议最近的实体与限量的选择器
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of("@p", "@e[limit=1]");
    }

    // 空数据取最近的实体；选择器必须在解析时唯一确定一个实体
    public static EntityAnchor parse(CommandSourceStack source, String data)
    {
        String text = data == null || data.isBlank() ? "@p" : data.trim();
        StringReader reader = new StringReader(text);

        try
        {
            EntitySelector selector = EntityArgument.entity().parse(reader);

            if (reader.canRead())
            {
                throw new IllegalArgumentException(WRONG);
            }

            Entity entity = selector.findSingleEntity(source);

            return new EntityAnchor(entity.getUUID(), entity.getId());
        }
        catch (CommandSyntaxException e)
        {
            throw new IllegalArgumentException(WRONG);
        }
    }
}
