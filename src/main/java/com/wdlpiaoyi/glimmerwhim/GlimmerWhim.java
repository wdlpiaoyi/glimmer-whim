package com.wdlpiaoyi.glimmerwhim;

import com.mojang.logging.LogUtils;
import com.wdlpiaoyi.glimmerwhim.client.FreeLookHandler;
import com.wdlpiaoyi.glimmerwhim.command.WhimCommand;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Glimmer Whim - 微光奇想。
 */
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

        // 通用配置服务端也要读（日志、权限、summon 的默认值），所以放在 dist 判断外面。
        context.registerConfig(ModConfig.Type.COMMON, WhimConfig.COMMON_SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            context.registerConfig(ModConfig.Type.CLIENT, WhimConfig.CLIENT_SPEC);
            context.getModEventBus().addListener(FreeLookHandler::onRegisterKeys);
            MinecraftForge.EVENT_BUS.register(FreeLookHandler.class);
        }

        LOGGER.info("Glimmer Whim loaded: 魔法流经你。");
    }

    /**
     * 调试信息。配置里 {@code [debug] verboseLog} 开着就打 info，关掉以后只进 debug，不刷屏。
     * <p>
     * 只读 COMMON 里的键 —— 服务端也在用。
     */
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
