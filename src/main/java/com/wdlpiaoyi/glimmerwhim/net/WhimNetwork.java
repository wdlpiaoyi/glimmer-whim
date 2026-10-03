package com.wdlpiaoyi.glimmerwhim.net;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WhimNetwork
{
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private WhimNetwork()
    {
    }

    public static void register()
    {
        int id = 0;

        CHANNEL.registerMessage(id++, WhimSummonPacket.class, WhimSummonPacket::encode, WhimSummonPacket::decode, WhimSummonPacket::handle);
        CHANNEL.registerMessage(id++, WhimRemovePacket.class, WhimRemovePacket::encode, WhimRemovePacket::decode, WhimRemovePacket::handle);
        CHANNEL.registerMessage(id++, WhimAimPacket.class, WhimAimPacket::encode, WhimAimPacket::decode, WhimAimPacket::handle);
        CHANNEL.registerMessage(id++, WhimUsePacket.class, WhimUsePacket::encode, WhimUsePacket::decode, WhimUsePacket::handle);
    }

    public static <MSG> void sendTo(ServerPlayer player, MSG packet)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
