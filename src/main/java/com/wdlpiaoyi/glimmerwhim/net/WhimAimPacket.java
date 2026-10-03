package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.whim.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record WhimAimPacket(UUID id)
{
    public static void encode(WhimAimPacket packet, FriendlyByteBuf buf)
    {
        buf.writeBoolean(packet.id != null);

        if (packet.id != null)
        {
            buf.writeUUID(packet.id);
        }
    }

    public static WhimAimPacket decode(FriendlyByteBuf buf)
    {
        return new WhimAimPacket(buf.readBoolean() ? buf.readUUID() : null);
    }

    public static void handle(WhimAimPacket packet, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();

        if (player != null)
        {
            ctx.enqueueWork(() -> WhimRegistry.setAimed(player, packet.id));
        }

        ctx.setPacketHandled(true);
    }
}
