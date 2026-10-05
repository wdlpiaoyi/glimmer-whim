package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

// 消散样式「碎片散开」：本体先亮一下，随后十几片光片沿各自方向飞出，边飞边缩小变暗并自转
// 尺寸照 {size} 走，任何声明了 {size} 的灵感都能挂；碎片是加色光片，不用贴图
public final class ScatterVanish
{
    private static final int SHARDS = 14;

    // 起始闪光占进度的比例，以及闪光扩散的倍率
    private static final double FLASH_END = 0.18D;
    private static final double FLASH_SPREAD = 0.6D;

    // 飞散距离相对核心半径；每片碎片的距离在 SPREAD_MIN..1 之间错开
    private static final double SPREAD_RATIO = 3.2D;
    private static final double SPREAD_MIN = 0.35D;

    private static final double SHARD_RATIO = 0.28D;
    private static final double SHARD_SPIN = 9.0D;

    private static final float[] SHARD_COLOR = { 0.78F, 0.55F, 1.0F, 1.0F };
    private static final float[] FLASH_COLOR = { 0.95F, 0.86F, 1.0F, 1.0F };

    private ScatterVanish()
    {
    }

    public static void draw(PoseStack pose, Vec3 dir, float progress, WhimData data, WhimParams params, UUID id)
    {
        double core = Glow.coreRadius(pose, params, data);
        double fade = 1.0D - progress;

        Glow.additive();

        if (progress < FLASH_END)
        {
            double flash = 1.0D - progress / FLASH_END;
            Glow.disc(pose, dir, core * (1.0D + FLASH_SPREAD * flash), FLASH_COLOR, 1.2D * flash);
        }

        // 缓出：一上来飞得最快，随后越来越慢
        double spread = core * SPREAD_RATIO * (1.0D - fade * fade);
        double time = Util.getMillis() / 1000.0D;

        for (int i = 0; i < SHARDS; i++)
        {
            double distance = spread * (SPREAD_MIN + (1.0D - SPREAD_MIN) * noise(id, i, 0));
            double size = core * SHARD_RATIO * fade * (0.55D + 0.9D * noise(id, i, 1));
            double spin = time * SHARD_SPIN * (noise(id, i, 2) * 2.0D - 1.0D) + i;
            float brightness = (float) (fade * (0.6D + 0.4D * noise(id, i, 3)));
            Vec3 offset = direction(id, i).scale(distance);

            pose.pushPose();
            pose.translate(offset.x, offset.y, offset.z);
            Glow.shard(pose, dir, size, spin, SHARD_COLOR, brightness);
            pose.popPose();
        }

        Glow.restore();
    }

    // 第 i 片碎片的飞散方向：球面上均匀取点，由 id 与序号决定，每帧稳定
    private static Vec3 direction(UUID id, int index)
    {
        double vertical = noise(id, index, 10) * 2.0D - 1.0D;
        double angle = noise(id, index, 11) * Math.PI * 2.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));

        return new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
    }

    // 同一片碎片每帧取到同一个 0..1 的伪随机数
    private static double noise(UUID id, int index, int salt)
    {
        long value = (id == null ? 0L : id.getMostSignificantBits() * 31L + id.getLeastSignificantBits());
        value = value * 0x9E3779B97F4A7C15L + index * 0x100000001B3L + salt;
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;

        return (value >>> 11) / (double) (1L << 53);
    }
}
