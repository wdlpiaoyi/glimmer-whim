package com.wdlpiaoyi.glimmerwhim.net;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WhimNetwork
{
    // 协议版本；两端的准入谓词都要求相等，不匹配会直接拒绝连接
    private static final String VERSION = "1";

    // glimmerwhim:main；第2参为版本提供器，第3/4参分别是客户端、服务端准入谓词
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
        // 包 id 按注册顺序递增，客户端与服务端必须完全一致
        int id = 0;

        CHANNEL.registerMessage(id++, WhimSummonPacket.class, WhimSummonPacket::encode, WhimSummonPacket::decode, WhimSummonPacket::handle);
        CHANNEL.registerMessage(id++, WhimRemovePacket.class, WhimRemovePacket::encode, WhimRemovePacket::decode, WhimRemovePacket::handle);
        CHANNEL.registerMessage(id++, WhimAimPacket.class, WhimAimPacket::encode, WhimAimPacket::decode, WhimAimPacket::handle);
        CHANNEL.registerMessage(id++, WhimUsePacket.class, WhimUsePacket::encode, WhimUsePacket::decode, WhimUsePacket::handle);
        CHANNEL.registerMessage(id++, WhimHoldPacket.class, WhimHoldPacket::encode, WhimHoldPacket::decode, WhimHoldPacket::handle);
        CHANNEL.registerMessage(id++, WhimVoidPacket.class, WhimVoidPacket::encode, WhimVoidPacket::decode, WhimVoidPacket::handle);
    }

    // 只发给单个玩家，供 S2C 定向同步
    public static <MSG> void sendTo(ServerPlayer player, MSG packet)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
