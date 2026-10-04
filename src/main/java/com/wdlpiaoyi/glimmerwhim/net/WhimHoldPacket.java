package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

// C2S：按下边沿即时锁定链根，id 非空
public record WhimHoldPacket(UUID id)
{
    public static void encode(WhimHoldPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUUID(packet.id);
    }

    public static WhimHoldPacket decode(FriendlyByteBuf buf)
    {
        return new WhimHoldPacket(buf.readUUID());
    }

    public static void handle(WhimHoldPacket packet, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();

        if (player != null)
        {
            ctx.enqueueWork(() -> WhimRegistry.hold(player, packet.id));
        }

        ctx.setPacketHandled(true);
    }
}
