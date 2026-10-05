package com.wdlpiaoyi.glimmerwhim.whims.content.client;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.glimmerwhim.client.ClientWhimCache;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
import com.wdlpiaoyi.glimmerwhim.whims.client.Glow;
import com.wdlpiaoyi.glimmerwhim.whims.content.StrikeWhim;

import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

// 落雷本体：不用贴图，画加色混合的光核与光环。{damage_ratio} 越大越多越亮，剩余寿命越少脉动越快，蓄力时自转加速变亮，被瞄准时整体放大
public final class StrikeRender
{
    // 本体停在最远 128 格的空中，按原尺寸瞄太苛刻：命中体积用球，半径对齐可见光核
    private static final float HIGHLIGHT_SCALE = 1.25F;
    private static final double HIGHLIGHT_BRIGHTNESS = 1.2D;

    private static final double IDLE_BRIGHTNESS = 0.85D;
    private static final double CHARGE_BRIGHTNESS = 1.35D;
    private static final double CHARGE_SCALE = 1.3D;
    private static final double CHARGE_SPIN = 3.0D;

    // 核心半径取 Glow 的通用约定：{size} 的一半再乘距离补偿，锚越远几何越大，屏幕上看起来差不多
    private static final double GLOW_RATIO = 2.6D;
    // 命中球半径 = {size} 的一半 × 该比例，与可见光核同一约定
    private static final double HIT_RADIUS_RATIO = GLOW_RATIO;
    private static final double RING_BASE_RATIO = 1.55D;
    private static final double RING_STEP_RATIO = 0.42D;
    private static final double RING_WIDTH_RATIO = 0.08D;
    private static final int RING_SEGMENTS = 64;
    // 4 段就是正四边形，用来拼晶体方框
    private static final int FRAME_SEGMENTS = 4;
    private static final int FRAMES = 3;
    // 立方体自转轴相对视线的倾角
    private static final double FRAME_TILT_DEGREES = 30.0D;
    private static final double FRAME_RATIO = 0.95D;
    // 光环：每个环的倾角与进动速度都不同，否则所有平面会固化成同一个姿态（看着像个 X）
    private static final double RING_TILT_BASE = 20.0D;
    private static final double RING_TILT_STEP = 51.0D;
    private static final double RING_RATE_BASE = 1.5D;
    private static final double RING_RATE_STEP = 0.25D;

    // 脉动幅度 ±7%（蓄力 ×1.3）；频率随剩余寿命从 1.2 Hz 升到 6.5 Hz
    private static final double PULSE_AMPLITUDE = 0.07D;
    private static final double PULSE_CHARGING = 1.3D;
    private static final double PULSE_MIN_HZ = 1.2D;
    private static final double PULSE_RANGE_HZ = 5.3D;
    // 弧度每秒；原实现是 0.35 弧度每刻的 0.75 倍
    private static final double SPIN_SPEED = 5.25D;

    private static final float[] CORE_COLOR = { 0.95F, 0.86F, 1.0F, 1.0F };
    private static final float[] GLOW_COLOR = { 0.62F, 0.26F, 0.98F, 1.0F };
    private static final float[] RING_COLOR = { 0.78F, 0.55F, 1.0F, 1.0F };
    // 瞄准指示环：比光环亮、带紫调，别用近白色，免得看着像一条默认描边
    private static final float[] AIM_COLOR = { 0.93F, 0.74F, 1.0F, 1.0F };

    private StrikeRender()
    {
    }

    public static void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        render(pose, dir, data, params, id, 1.0D, 1.0D, false);
    }

    // 被瞄准时放大加亮，并多一圈正对相机的亮环
    public static void highlight(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        render(pose, dir, data, params, id, HIGHLIGHT_SCALE, HIGHLIGHT_BRIGHTNESS, true);
    }

    // 命中体积是球，半径对齐可见光核（见 HIT_RADIUS_RATIO），再按 {hit_scale} 缩放
    public static double hit(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        return DefaultRender.sphere(eye, look, at, params.number(data, StrikeWhim.SIZE, 1.0D) * 0.5D * HIT_RADIUS_RATIO
                * params.number(data, Whim.HIT_SCALE, 1.0D));
    }

    private static void render(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id, double scale,
            double brightnessScale, boolean aimed)
    {
        boolean charging = !data.get(StrikeWhim.CHARGING).isEmpty();
        double ratio = params.number(data, StrikeWhim.DAMAGE_RATIO, 1.0D);
        double core = Glow.coreRadius(pose, params, data) * scale * (charging ? CHARGE_SCALE : 1.0D)
                * beat(id, data, charging);
        double brightness = (0.65D + 0.9D * StrikeWhim.intensity(ratio)) * brightnessScale
                * (charging ? CHARGE_BRIGHTNESS : IDLE_BRIGHTNESS) * beat(id, data, charging);
        int rings = 1 + (int) Math.round(3.0D * StrikeWhim.intensity(ratio));
        double spin = time() * SPIN_SPEED * (charging ? CHARGE_SPIN : 1.0D);

        Glow.additive();

        // 外层辉光与内核；两层叠出「光」而不是「面」
        Glow.disc(pose, dir, core * GLOW_RATIO, GLOW_COLOR, 0.45D * brightness);
        Glow.disc(pose, dir, core, CORE_COLOR, 0.95D * brightness);

        // 立方体骨架：三面互相垂直、共用一个半径，整体绕自身轴自转
        Vec3 cubeAxis = Glow.rotate(dir, Glow.perpendicular(dir), Math.toRadians(FRAME_TILT_DEGREES));
        Vec3 cubeFirst = Glow.rotate(Glow.perpendicular(cubeAxis), cubeAxis, spin * 0.6D);
        Vec3 cubeSecond = cubeAxis.cross(cubeFirst).normalize();
        Vec3[] frameNormals = { cubeAxis, cubeFirst, cubeSecond };
        double frameRadius = core * FRAME_RATIO;

        for (int i = 0; i < FRAMES; i++)
        {
            Glow.ring(pose, frameNormals[i], frameRadius, frameRadius * RING_WIDTH_RATIO, 0.0D, FRAME_SEGMENTS,
                    CORE_COLOR, 0.6D * brightness);
        }

        // 光环：各自以不同速度、交替方向进动，环之间才有相对旋转
        for (int i = 0; i < rings; i++)
        {
            double rate = (i % 2 == 0 ? 1.0D : -1.0D) * (RING_RATE_BASE - RING_RATE_STEP * i);
            Vec3 normal = tilt(dir, spin * rate + Math.PI * 2.0D * i / rings, RING_TILT_BASE + RING_TILT_STEP * i);
            double radius = core * (RING_BASE_RATIO + RING_STEP_RATIO * i);
            Glow.ring(pose, normal, radius, radius * RING_WIDTH_RATIO, spin * 0.5D + i, RING_SEGMENTS, RING_COLOR,
                    0.75D * brightness);
        }

        if (aimed)
        {
            double radius = core * (RING_BASE_RATIO + RING_STEP_RATIO * rings + 0.25D);
            Glow.ring(pose, dir, radius, radius * RING_WIDTH_RATIO, 0.0D, RING_SEGMENTS, AIM_COLOR, 0.9D * brightness);
        }

        Glow.restore();
    }

    // 法线从视线方向绕 axis 倾斜 tilt 度；axis 由 phase 绕视线旋转，使倾斜方向随时间滚动
    private static Vec3 tilt(Vec3 dir, double phase, double tiltDegrees)
    {
        Vec3 axis = Glow.rotate(Glow.perpendicular(dir), dir, phase);
        return Glow.rotate(dir, axis, Math.toRadians(tiltDegrees));
    }

    // 脉动：剩余寿命越少越快；倒计时是客户端本地的，只影响外观
    private static double beat(UUID id, WhimData data, boolean charging)
    {
        double remaining = ClientWhimCache.remainingFraction(id, data);
        double hertz = PULSE_MIN_HZ + PULSE_RANGE_HZ * (1.0D - remaining);
        double amplitude = PULSE_AMPLITUDE * (charging ? PULSE_CHARGING : 1.0D);
        // 同一个游戏刻里不同灵感错开，免得整批一起呼吸
        double offset = (id == null ? 0.0D : (id.hashCode() & 0xFF) / 255.0D) * Math.PI * 2.0D;

        return 1.0D + amplitude * Math.sin(time() * hertz * Math.PI * 2.0D + offset);
    }

    // 表现用时间：渲染相关的时长一律毫秒，这里转成秒
    private static double time()
    {
        return Util.getMillis() / 1000.0D;
    }
}
