package com.wdlpiaoyi.glimmerwhim.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public record WhimUsePacket(List<UUID> chain, WhimTarget target)
{
    public static void encode(WhimUsePacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.chain.size());

        for (UUID id : packet.chain)
        {
            buf.writeUUID(id);
        }

        buf.writeBoolean(packet.target != null);

        if (packet.target != null)
        {
            buf.writeBoolean(packet.target.entity() != null);

            if (packet.target.entity() != null)
            {
                buf.writeUUID(packet.target.entity());
            }

            buf.writeDouble(packet.target.point().x);
            buf.writeDouble(packet.target.point().y);
            buf.writeDouble(packet.target.point().z);
        }
    }

    public static WhimUsePacket decode(FriendlyByteBuf buf)
    {
        int size = buf.readVarInt();
        List<UUID> chain = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
        {
            chain.add(buf.readUUID());
        }

        if (!buf.readBoolean())
        {
            return new WhimUsePacket(chain, null);
        }

        UUID entity = buf.readBoolean() ? buf.readUUID() : null;
        Vec3 point = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());

        return new WhimUsePacket(chain, new WhimTarget(entity, point));
    }

    public static void handle(WhimUsePacket packet, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();

        if (player != null)
        {
            ctx.enqueueWork(() -> WhimRegistry.use(player, packet.chain, packet.target));
        }

        ctx.setPacketHandled(true);
    }
}
