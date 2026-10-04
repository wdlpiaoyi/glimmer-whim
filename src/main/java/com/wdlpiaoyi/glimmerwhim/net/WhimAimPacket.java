package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

// C2S：id 可空，null 表示当前没有瞄准目标
public record WhimAimPacket(UUID id)
{
    public static void encode(WhimAimPacket packet, FriendlyByteBuf buf)
    {
        // 先写存在位，再按需写 UUID
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
            // 切回主线程执行，避免在网络线程改动注册表
            ctx.enqueueWork(() -> WhimRegistry.setAimed(player, packet.id));
        }

        ctx.setPacketHandled(true);
    }
}
