package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.client.ClientWhimCache;
import com.wdlpiaoyi.glimmerwhim.whim.Whim;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchors;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 一条灵感出生。
 * <p>
 * 带上维度是为了让客户端知道"这还是不是同一个世界" —— 收到别的维度的包就先把缓存清了。
 * 不带 visibility：能不能用是发包那一刻的事，用不了的人根本收不到。
 */
public record WhimSpawnPacket(ResourceKey<Level> dimension, UUID id, WhimAnchor anchor, ResourceLocation element, int lifetime)
{
    public static WhimSpawnPacket of(ResourceKey<Level> dimension, Whim whim)
    {
        return new WhimSpawnPacket(dimension, whim.id(), whim.anchor(), whim.element(), whim.lifetime());
    }

    public static void encode(WhimSpawnPacket packet, FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeUUID(packet.id);
        WhimAnchors.write(buf, packet.anchor);
        buf.writeResourceLocation(packet.element);
        buf.writeVarInt(packet.lifetime);
    }

    public static WhimSpawnPacket decode(FriendlyByteBuf buf)
    {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        UUID id = buf.readUUID();
        WhimAnchor anchor = WhimAnchors.read(buf);
        ResourceLocation element = buf.readResourceLocation();
        int lifetime = buf.readVarInt();

        return new WhimSpawnPacket(dimension, id, anchor, element, lifetime);
    }

    public static void handle(WhimSpawnPacket packet, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWhimCache.accept(packet)));
        context.get().setPacketHandled(true);
    }
}
