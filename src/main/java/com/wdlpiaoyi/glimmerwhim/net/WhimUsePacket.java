package com.wdlpiaoyi.glimmerwhim.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTargets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

// C2S：释放时提交整条链快照与命中目标；目标编码为 种类 id + payload
public record WhimUsePacket(List<UUID> chain, WhimTarget target)
{
    // 解码上限，防对端伪造超长链导致大分配
    private static final int MAX_CHAIN = 64;

    public static void encode(WhimUsePacket packet, FriendlyByteBuf buf)
    {
        // 先写链长与逐个 UUID，再写目标存在位、种类与 payload
        buf.writeVarInt(packet.chain.size());

        for (UUID id : packet.chain)
        {
            buf.writeUUID(id);
        }

        buf.writeBoolean(packet.target != null);

        if (packet.target != null)
        {
            buf.writeResourceLocation(packet.target.kind().id());
            packet.target.data().write(buf);
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

        ResourceLocation id = buf.readResourceLocation();
        WhimData data = WhimData.read(buf);
        WhimTargets.Kind kind = WhimTargets.get(id);

        // 未登记的种类按无目标处理，使用时会按 DROPPED 丢弃
        return new WhimUsePacket(chain, kind == null ? null : new WhimTarget(kind, data));
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
