package com.wdlpiaoyi.glimmerwhim;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Glimmer Whim - 微光奇想.
 *
 * <p>Core premise: the player is not the owner of magic, but its channel. Inspiration is
 * supplied by the world, never stored on the player, so there is no mana bar and no cooldown.
 * See 设计纲要.md in the project root before adding any system here.</p>
 */
@Mod(GlimmerWhim.MODID)
public final class GlimmerWhim
{
    public static final String MODID = "glimmerwhim";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GlimmerWhim(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        LOGGER.info("Glimmer Whim loaded: 魔法流经你。");
    }
}
