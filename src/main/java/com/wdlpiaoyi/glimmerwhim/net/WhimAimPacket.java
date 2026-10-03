package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.whim.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端唯一会往上说的那句话：我现在瞄着哪条。
 * <p>
 * 瞄是客户端算的（方向可以没有距离，只有客户端能算），但命令补全在服务端，所以要过个话。
 * {@code id} 是 null 表示现在没瞄。
 */
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
