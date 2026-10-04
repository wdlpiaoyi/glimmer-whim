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

// C2S：释放时提交整条链快照与命中目标；target/entity 均可空
public record WhimUsePacket(List<UUID> chain, WhimTarget target)
{
    // 解码上限，防对端伪造超长链导致大分配
    private static final int MAX_CHAIN = 64;

    public static void encode(WhimUsePacket packet, FriendlyByteBuf buf)
    {
        // 先写链长与逐个 UUID，再写 target/entity 存在位，最后写命中点
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
        // 负数或超限都夹到 [0, MAX_CHAIN]，与 encode 对称
        int size = buf.readVarInt();
        int count = Math.min(Math.max(size, 0), MAX_CHAIN);
        List<UUID> chain = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
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
