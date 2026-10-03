package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.client.ClientWhimCache;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRemoveReason;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** 一条灵感消失。原因客户端暂时不用，但留着 —— 以后"它溜走了"要有话说。 */
public record WhimRemovePacket(ResourceKey<Level> dimension, UUID id, WhimRemoveReason reason)
{
    public static void encode(WhimRemovePacket packet, FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeUUID(packet.id);
        buf.writeEnum(packet.reason);
    }

    public static WhimRemovePacket decode(FriendlyByteBuf buf)
    {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        UUID id = buf.readUUID();
        WhimRemoveReason reason = buf.readEnum(WhimRemoveReason.class);

        return new WhimRemovePacket(dimension, id, reason);
    }

    public static void handle(WhimRemovePacket packet, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWhimCache.remove(packet)));
        context.get().setPacketHandled(true);
    }
}
