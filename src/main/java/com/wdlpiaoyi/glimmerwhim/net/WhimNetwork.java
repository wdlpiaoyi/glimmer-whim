package com.wdlpiaoyi.glimmerwhim.net;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** 灵感怎么过线。只有两个包：出生、消失。 */
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

        CHANNEL.registerMessage(id++, WhimSpawnPacket.class, WhimSpawnPacket::encode, WhimSpawnPacket::decode, WhimSpawnPacket::handle);
        CHANNEL.registerMessage(id++, WhimRemovePacket.class, WhimRemovePacket::encode, WhimRemovePacket::decode, WhimRemovePacket::handle);
    }

    public static <MSG> void sendTo(ServerPlayer player, MSG packet)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
