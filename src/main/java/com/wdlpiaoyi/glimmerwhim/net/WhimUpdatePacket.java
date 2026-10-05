package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.client.ClientWhimCache;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// S2C：运行期数据变更；锚点与元素不变，可见性变更走重新召唤/移除
public record WhimUpdatePacket(ResourceKey<Level> dimension, UUID id, WhimData data)
{
    public static void encode(WhimUpdatePacket packet, FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeUUID(packet.id);
        packet.data.write(buf);
    }

    public static WhimUpdatePacket decode(FriendlyByteBuf buf)
    {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        UUID id = buf.readUUID();
        WhimData data = WhimData.read(buf);

        return new WhimUpdatePacket(dimension, id, data);
    }

    public static void handle(WhimUpdatePacket packet, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWhimCache.update(packet)));
        context.get().setPacketHandled(true);
    }
}
