package com.wdlpiaoyi.glimmerwhim.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.whim.Whim;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 开发者占位。紫黑棋盘，能看见就算完。 */
public final class WhimRenderer
{
    /** 画一条灵感。pose 已经挪到那条灵感的位置；params 是它那种锚认的参数（没写的取默认值要用）。 */
    @FunctionalInterface
    public interface Drawer
    {
        void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, boolean aimed);
    }

    /** 这一帧要画的一条：位置和参数表都算好了。 */
    private record Drawable(ClientWhimCache.WhimView whim, WhimParams params, Vec3 at)
    {
    }

    private static final Map<ResourceLocation, Drawer> DRAWERS = new LinkedHashMap<>();

    static
    {
        register(Whim.DEV_ELEMENT, WhimRenderer::dev);
    }

    private WhimRenderer()
    {
    }

    public static void register(ResourceLocation element, Drawer drawer)
    {
        DRAWERS.put(element, drawer);
    }

    /** 没登记过的元素先按 dev 画。 */
    public static Drawer drawer(ResourceLocation element)
    {
        return DRAWERS.getOrDefault(element, WhimRenderer::dev);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null || ClientWhimCache.all().isEmpty())
        {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        // "瞄"问的是镜头看向哪儿：自由视角开着的时候高亮跟着镜头走（准星在屏幕正中间），
        // 没开的时候和原版的 getViewVector 等价。挖、放、打那些动作不归这里管。
        Vec3 look = FreeLook.viewVector(minecraft.player, partialTick);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        UUID aimed = WhimAim.update(level, eye, look, partialTick);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 深度测试是关的，前后关系全靠画家算法：远的先画。不排的话后画的远面会盖住近面。
        List<Drawable> drawable = new ArrayList<>();
        double maxDistance = WhimConfig.renderMaxDistance();

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            WhimAnchor anchor = whim.anchor();
            WhimParams params = WhimAnchors.params(anchor.type());
            Vec3 at = anchor.position(level, eye, partialTick, whim.data(), params).orElse(null);

            if (at == null || at.subtract(eye).lengthSqr() < 1.0E-8D)
            {
                continue;
            }

            // 比 [render] maxDistance 更远的灵感就不画了。
            if (at.distanceToSqr(eye) > maxDistance * maxDistance)
            {
                continue;
            }

            drawable.add(new Drawable(whim, params, at));
        }

        drawable.sort(Comparator.comparingDouble(entry -> -entry.at().distanceToSqr(eye)));

        for (Drawable entry : drawable)
        {
            ClientWhimCache.WhimView whim = entry.whim();
            Vec3 dir = entry.at().subtract(eye).normalize();
            Vec3 position = entry.at().subtract(camera);

            pose.pushPose();
            pose.translate(position.x, position.y, position.z);

            drawer(whim.element()).draw(pose, dir, whim.data(), entry.params(), whim.id().equals(aimed));

            pose.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** dev：紫黑棋盘。{@code shape} 挑方片还是立方体，{@code size} 是边长 —— 1 就是一格。 */
    public static void dev(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, boolean aimed)
    {
        // 有的锚一个参数都不收（表是空的），那就按立方体、边长 1 画。
        boolean cube = "cube".equals(params.text(data, "shape", "cube"));
        // 画的时候要的是半边长。
        double half = params.number(data, "size", 1.0D) / 2.0D;
        // 长什么样、宽多少、什么颜色，全在 config 的 [render] 里，改完当场生效。
        double outline = WhimConfig.renderOutlineWidth();
        float[] first = WhimConfig.renderColorElement();
        float[] second = WhimConfig.renderColorElementAlt();
        float[] aimedColor = WhimConfig.renderColorAimed();

        if (aimed && outline > 0.0D)
        {
            if (cube)
            {
                cube(pose, half + outline, aimedColor, aimedColor);
            }
            else
            {
                quad(pose, dir, half + outline, aimedColor, aimedColor);
            }
        }

        if (cube)
        {
            cube(pose, half, first, second);
        }
        else
        {
            quad(pose, dir, half, first, second);
        }
    }

    /** 正对玩家的一张方片。{@code half} 是半边长。 */
    private static void quad(PoseStack pose, Vec3 dir, double half, float[] first, float[] second)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Vec3 right = right(dir);
        Vec3 up = right.cross(dir).normalize();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        face(builder, matrix, right.scale(-half).add(up.scale(-half)), right.scale(2.0D * half), up.scale(2.0D * half),
                first, second, 1.0D);
        BufferUploader.drawWithShader(builder.end());
    }

    /** 六个面。{@code half} 是半边长。 */
    private static void cube(PoseStack pose, double half, float[] first, float[] second)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        // 相机就在 pose 的原点上（位置已经减过相机了），所以按面心离原点多远排：远的先画。
        List<Side> sides = new ArrayList<>(SIDES);
        sides.sort(Comparator.comparingDouble(side -> -side.centre().lengthSqr()));

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Side side : sides)
        {
            face(builder, matrix, side.origin().scale(half), side.u().scale(half), side.v().scale(half),
                    first, second, shade(side.facing()));
        }

        BufferUploader.drawWithShader(builder.end());
    }

    /** 单位立方体的一个面，画的时候乘 half。{@code facing} 只用来取明暗。 */
    private record Side(Vec3 origin, Vec3 u, Vec3 v, Direction facing)
    {
        Vec3 centre()
        {
            return this.origin.add(this.u.scale(0.5D)).add(this.v.scale(0.5D));
        }
    }

    /** 单位立方体的六个面。 */
    private static final List<Side> SIDES = List.of(
            new Side(new Vec3(-1.0D, -1.0D, -1.0D), new Vec3(0.0D, 0.0D, 2.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.WEST),
            new Side(new Vec3(1.0D, -1.0D, 1.0D), new Vec3(0.0D, 0.0D, -2.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.EAST),
            new Side(new Vec3(-1.0D, -1.0D, 1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, -2.0D), Direction.DOWN),
            new Side(new Vec3(-1.0D, 1.0D, -1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 2.0D), Direction.UP),
            new Side(new Vec3(1.0D, -1.0D, -1.0D), new Vec3(-2.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.NORTH),
            new Side(new Vec3(-1.0D, -1.0D, 1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.SOUTH));

    /** 原版方块六面的明暗：上 1.0、南北 0.8、东西 0.6、下 0.5。少了这个看着就还是一张纸。 */
    private static double shade(Direction facing)
    {
        if (!WhimConfig.renderFaceShade())
        {
            // 配置里关掉了就六面一样亮。
            return 1.0D;
        }

        return switch (facing)
        {
            case UP -> 1.0D;
            case DOWN -> 0.5D;
            case NORTH, SOUTH -> 0.8D;
            case WEST, EAST -> 0.6D;
        };
    }

    /** 一面切成几格棋盘（config 的 {@code render.checkerCells}，默认 2x2 —— 就是原版贴图丢了那副样子）。{@code shade} 是这一面该压多暗。 */
    private static void face(BufferBuilder builder, Matrix4f matrix, Vec3 origin, Vec3 u, Vec3 v, float[] first,
            float[] second, double shade)
    {
        int cells = WhimConfig.renderCheckerCells();

        for (int i = 0; i < cells; i++)
        {
            for (int j = 0; j < cells; j++)
            {
                double i0 = i / (double) cells;
                double i1 = (i + 1) / (double) cells;
                double j0 = j / (double) cells;
                double j1 = (j + 1) / (double) cells;
                float[] color = tint(((i + j) & 1) == 0 ? first : second, shade);

                corner(builder, matrix, origin.add(u.scale(i0)).add(v.scale(j0)), color);
                corner(builder, matrix, origin.add(u.scale(i1)).add(v.scale(j0)), color);
                corner(builder, matrix, origin.add(u.scale(i1)).add(v.scale(j1)), color);
                corner(builder, matrix, origin.add(u.scale(i0)).add(v.scale(j1)), color);
            }
        }
    }

    /** 按明暗把颜色压一下；黑的地方压完还是黑的。 */
    private static float[] tint(float[] color, double shade)
    {
        return new float[] { (float) (color[0] * shade), (float) (color[1] * shade), (float) (color[2] * shade),
                color[3] };
    }

    private static Vec3 right(Vec3 dir)
    {
        Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (right.lengthSqr() < 1.0E-6D)
        {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }

        return right.normalize();
    }

    private static void corner(BufferBuilder builder, Matrix4f matrix, Vec3 position, float[] color)
    {
        builder.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
    }
}
