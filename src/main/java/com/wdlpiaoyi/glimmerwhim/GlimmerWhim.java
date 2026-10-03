package com.wdlpiaoyi.glimmerwhim;

import com.mojang.logging.LogUtils;
import com.wdlpiaoyi.glimmerwhim.client.FreeLookHandler;
import com.wdlpiaoyi.glimmerwhim.client.WhimInteractHandler;
import com.wdlpiaoyi.glimmerwhim.command.WhimCommand;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        MinecraftForge.EVENT_BUS.register(WhimRegistry.class);
        MinecraftForge.EVENT_BUS.register(WhimCommand.class);

        context.registerConfig(ModConfig.Type.COMMON, WhimConfig.COMMON_SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            context.registerConfig(ModConfig.Type.CLIENT, WhimConfig.CLIENT_SPEC);
            context.getModEventBus().addListener(FreeLookHandler::onRegisterKeys);
            MinecraftForge.EVENT_BUS.register(FreeLookHandler.class);
            context.getModEventBus().addListener(WhimInteractHandler::onRegisterKeys);
            MinecraftForge.EVENT_BUS.register(WhimInteractHandler.class);
        }

        LOGGER.info("Glimmer Whim loaded");
    }

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
