package com.wdlpiaoyi.glimmerwhim.net;

import java.util.UUID;
import java.util.function.Supplier;

import com.wdlpiaoyi.glimmerwhim.client.ClientWhimCache;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

// S2C：召唤时同步维度、id、锚点、元素与数据
public record WhimSummonPacket(ResourceKey<Level> dimension, UUID id, WhimAnchor anchor, ResourceLocation element,
        WhimData data)
{
    // 由服务端 Whim 构造；元素用 type().id()
    public static WhimSummonPacket of(ResourceKey<Level> dimension, Whim whim)
    {
        return new WhimSummonPacket(dimension, whim.id(), whim.anchor(), whim.type().id(), whim.data());
    }

    public static void encode(WhimSummonPacket packet, FriendlyByteBuf buf)
    {
        // 字段顺序：dimension(RL)、id、anchor、element(RL)、data，需与 decode 严格对称
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeUUID(packet.id);
        WhimAnchors.write(buf, packet.anchor);
        buf.writeResourceLocation(packet.element);
        packet.data.write(buf);
    }

    public static WhimSummonPacket decode(FriendlyByteBuf buf)
    {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        UUID id = buf.readUUID();
        WhimAnchor anchor = WhimAnchors.read(buf);
        ResourceLocation element = buf.readResourceLocation();
        WhimData data = WhimData.read(buf);

        return new WhimSummonPacket(dimension, id, anchor, element, data);
    }

    public static void handle(WhimSummonPacket packet, Supplier<NetworkEvent.Context> context)
    {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWhimCache.accept(packet)));
        context.get().setPacketHandled(true);
    }
}
