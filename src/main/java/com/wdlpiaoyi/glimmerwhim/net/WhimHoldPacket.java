package com.wdlpiaoyi.glimmerwhim.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

// C2S：链路快照，索引 0 为根；链每有变化就重发，服务端据此按链暂停寿命
public record WhimHoldPacket(List<UUID> chain)
{
    // 解码上限，防对端伪造超长链导致大分配
    private static final int MAX_CHAIN = 64;

    public static void encode(WhimHoldPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.chain.size());

        for (UUID id : packet.chain)
        {
            buf.writeUUID(id);
        }
    }

    public static WhimHoldPacket decode(FriendlyByteBuf buf)
    {
        // 负数或超限都夹到 [0, MAX_CHAIN]，与 encode 对称
        int size = buf.readVarInt();
        int count = Math.min(Math.max(size, 0), MAX_CHAIN);
        List<UUID> chain = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            chain.add(buf.readUUID());
        }

        return new WhimHoldPacket(chain);
    }

    public static void handle(WhimHoldPacket packet, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();

        if (player != null)
        {
            ctx.enqueueWork(() -> WhimRegistry.hold(player, packet.chain()));
        }

        ctx.setPacketHandled(true);
    }
}
