package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.client.ClientWhimCache;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record WhimSummonPacket(ResourceKey<Level> dimension, UUID id, WhimAnchor anchor, ResourceLocation element,
        WhimData data, int lifetime)
{
    public static WhimSummonPacket of(ResourceKey<Level> dimension, Whim whim)
    {
        return new WhimSummonPacket(dimension, whim.id(), whim.anchor(), whim.element(), whim.data(), whim.lifetime());
    }

    public static void encode(WhimSummonPacket packet, FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeUUID(packet.id);
        WhimAnchors.write(buf, packet.anchor);
        buf.writeResourceLocation(packet.element);
        packet.data.write(buf);
        buf.writeVarInt(packet.lifetime);
    }

    public static WhimSummonPacket decode(FriendlyByteBuf buf)
    {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        UUID id = buf.readUUID();
        WhimAnchor anchor = WhimAnchors.read(buf);
        ResourceLocation element = buf.readResourceLocation();
        WhimData data = WhimData.read(buf);
        int lifetime = buf.readVarInt();

        return new WhimSummonPacket(dimension, id, anchor, element, data, lifetime);
    }

    public static void handle(WhimSummonPacket packet, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWhimCache.accept(packet)));
        context.get().setPacketHandled(true);
    }
}
