package com.wdlpiaoyi.glimmerwhim;

import com.mojang.logging.LogUtils;
import com.wdlpiaoyi.glimmerwhim.client.FreeLookHandler;
import com.wdlpiaoyi.glimmerwhim.client.WhimInteractHandler;
import com.wdlpiaoyi.glimmerwhim.command.WhimCommand;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.whims.WhimContent;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(GlimmerWhim.MODID)
public final class GlimmerWhim
{
    public static final String MODID = "glimmerwhim";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GlimmerWhim(FMLJavaModLoadingContext context)
    {
        WhimNetwork.register();
        context.getModEventBus().addListener(this::onLoadComplete);
        // WhimRegistry/WhimCommand 挂 Forge 事件总线（非 mod 总线），接收游戏事件
        MinecraftForge.EVENT_BUS.register(WhimRegistry.class);
        MinecraftForge.EVENT_BUS.register(WhimCommand.class);

        // COMMON 两端都加载；CLIENT 仅在物理客户端注册
        context.registerConfig(ModConfig.Type.COMMON, WhimConfig.COMMON_SPEC);

        // 客户端专用类只在客户端加载，专用服务器不触碰
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            context.registerConfig(ModConfig.Type.CLIENT, WhimConfig.CLIENT_SPEC);
            // 键位注册走 mod 总线，运行时事件走 Forge 总线
            context.getModEventBus().addListener(FreeLookHandler::onRegisterKeys);
            MinecraftForge.EVENT_BUS.register(FreeLookHandler.class);
            context.getModEventBus().addListener(WhimInteractHandler::onRegisterKeys);
            MinecraftForge.EVENT_BUS.register(WhimInteractHandler.class);
        }

        LOGGER.info("Glimmer Whim loaded");
    }

    // 各模组注册完内容后才校验禁用名单，否则注册表还不完整；只报告问题，不拦截
    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        for (String raw : WhimConfig.disabledWhimNames())
        {
            ResourceLocation id = WhimConfig.resolveWhimId(raw);

            if (id == null)
            {
                LOGGER.warn("配置里禁用的灵感 id 无法解析: {}", raw);
                continue;
            }

            if (!WhimContent.known(id))
            {
                LOGGER.warn("配置里禁用的灵感 {} 不是已知内容：{}", id, unknownReason(id.getNamespace()));
            }
        }
    }

    // 命名空间即模组 id：已加载说明是 id 写错，否则多半是那个模组没装
    private static String unknownReason(String namespace)
    {
        if (MODID.equals(namespace))
        {
            return "id 可能拼错，或该内容已被移除";
        }

        return ModList.get().isLoaded(namespace) ? "该命名空间已加载，但没有登记这个内容" : "对应模组可能未安装";
    }

    // verboseLog 开启时记 info 级别，否则降为 debug 级别
    public static void log(String format, Object... args)
    {
        if (WhimConfig.verboseLog())
        {
            LOGGER.info(format, args);
        }
        else
        {
            LOGGER.debug(format, args);
        }
    }
}
