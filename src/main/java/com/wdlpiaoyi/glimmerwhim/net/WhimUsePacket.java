package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record WhimUsePacket(UUID id, boolean success)
{
    public static void encode(WhimUsePacket packet, FriendlyByteBuf buf)
    {
        buf.writeUUID(packet.id);
        buf.writeBoolean(packet.success);
    }

    public static WhimUsePacket decode(FriendlyByteBuf buf)
    {
        return new WhimUsePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(WhimUsePacket packet, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();

        if (player != null)
        {
            ctx.enqueueWork(() -> WhimRegistry.use(player, packet.id, packet.success));
        }

        ctx.setPacketHandled(true);
    }
}
