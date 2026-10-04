package com.wdlpiaoyi.glimmerwhim.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record WhimVoidPacket(List<UUID> chain)
{
    private static final int MAX_CHAIN = 64;

    public static void encode(WhimVoidPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.chain.size());

        for (UUID id : packet.chain)
        {
            buf.writeUUID(id);
        }
    }

    public static WhimVoidPacket decode(FriendlyByteBuf buf)
    {
        int size = buf.readVarInt();
        List<UUID> chain = new ArrayList<>(Math.min(Math.max(size, 0), MAX_CHAIN));

        for (int i = 0; i < size; i++)
        {
            chain.add(buf.readUUID());
        }

        return new WhimVoidPacket(chain);
    }

    public static void handle(WhimVoidPacket packet, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();

        if (player != null)
        {
            ctx.enqueueWork(() -> WhimRegistry.voidChain(player, packet.chain));
        }

        ctx.setPacketHandled(true);
    }
}
