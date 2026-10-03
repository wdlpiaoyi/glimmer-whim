package com.wdlpiaoyi.glimmerwhim;

import com.mojang.logging.LogUtils;
import com.wdlpiaoyi.glimmerwhim.command.WhimCommand;
import com.wdlpiaoyi.glimmerwhim.net.WhimNetwork;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRegistry;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Glimmer Whim - 微光奇想。
 *
 * <p>动手加任何系统之前，先读项目根目录的 设计纲要.md。</p>
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
        LOGGER.info("Glimmer Whim loaded: 魔法流经你。");
    }
}
