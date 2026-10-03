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
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WhimRenderer
{
    @FunctionalInterface
    public interface Drawer
    {
        void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, boolean aimed);
    }

    @FunctionalInterface
    public interface Hit
    {
        double test(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params);
    }

    private record Drawable(ClientWhimCache.WhimView whim, WhimParams params, Vec3 at)
    {
    }

    private static final Map<ResourceLocation, Drawer> DRAWERS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, Hit> HITS = new LinkedHashMap<>();

    static
    {
        register(Whim.DEV_ELEMENT, WhimRenderer::dev, WhimRenderer::devHit);
    }

    private WhimRenderer()
    {
    }

    public static void register(ResourceLocation element, Drawer drawer, Hit hit)
    {
        DRAWERS.put(element, drawer);
        HITS.put(element, hit);
    }

    public static Drawer drawer(ResourceLocation element)
    {
        return DRAWERS.getOrDefault(element, WhimRenderer::dev);
    }

    public static Hit hit(ResourceLocation element)
    {
        return HITS.getOrDefault(element, WhimRenderer::devHit);
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

        List<Drawable> drawable = new ArrayList<>();
        double reach = WhimReach.blocks();

        for (ClientWhimCache.WhimView whim : ClientWhimCache.all())
        {
            WhimAnchor anchor = whim.anchor();
            WhimParams params = WhimAnchors.params(anchor.type());
            Vec3 at = anchor.position(level, eye, partialTick, whim.data(), params).orElse(null);

            if (at == null || at.subtract(eye).lengthSqr() < 1.0E-8D)
            {
                continue;
            }

            if (at.distanceToSqr(eye) > reach * reach)
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

    public static void dev(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, boolean aimed)
    {
        boolean cube = "cube".equals(params.text(data, "shape", "cube"));
        double half = params.number(data, "size", 1.0D) / 2.0D;
        int cells = WhimConfig.renderDevCheckerCells();
        double outline = WhimConfig.renderOutlineWidth();
        float[] first = WhimConfig.renderDevColorElement();
        float[] second = WhimConfig.renderDevColorElementAlt();
        float[] aimedColor = WhimConfig.renderColorAimed();

        if (aimed && outline > 0.0D)
        {
            if (cube)
            {
                cube(pose, half + outline, aimedColor, aimedColor, cells);
            }
            else
            {
                quad(pose, dir, half + outline, aimedColor, aimedColor, cells);
            }
        }

        if (cube)
        {
            cube(pose, half, first, second, cells);
        }
        else
        {
            quad(pose, dir, half, first, second, cells);
        }
    }

    public static double devHit(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        boolean cube = "cube".equals(params.text(data, "shape", "cube"));
        double half = params.number(data, "size", 1.0D) / 2.0D;

        return cube ? hitBox(eye, look, at, half) : hitQuad(eye, look, at, half);
    }

    private static double hitBox(Vec3 eye, Vec3 look, Vec3 at, double half)
    {
        double[] origin = { eye.x, eye.y, eye.z };
        double[] direction = { look.x, look.y, look.z };
        double[] centre = { at.x, at.y, at.z };
        double near = 0.0D;
        double far = Double.POSITIVE_INFINITY;

        for (int axis = 0; axis < 3; axis++)
        {
            if (Math.abs(direction[axis]) < 1.0E-9D)
            {
                if (origin[axis] < centre[axis] - half || origin[axis] > centre[axis] + half)
                {
                    return -1.0D;
                }

                continue;
            }

            double first = (centre[axis] - half - origin[axis]) / direction[axis];
            double second = (centre[axis] + half - origin[axis]) / direction[axis];

            if (first > second)
            {
                double swap = first;
                first = second;
                second = swap;
            }

            near = Math.max(near, first);
            far = Math.min(far, second);

            if (near > far)
            {
                return -1.0D;
            }
        }

        return far < 0.0D ? -1.0D : near;
    }

    private static double hitQuad(Vec3 eye, Vec3 look, Vec3 at, double half)
    {
        Vec3 normal = at.subtract(eye).normalize();
        double facing = look.dot(normal);

        if (facing < 1.0E-6D)
        {
            return -1.0D;
        }

        double distance = at.subtract(eye).dot(normal) / facing;

        if (distance <= 0.0D)
        {
            return -1.0D;
        }

        Vec3 offset = eye.add(look.scale(distance)).subtract(at);
        Vec3 right = right(normal);
        Vec3 up = right.cross(normal).normalize();

        return Math.abs(offset.dot(right)) <= half && Math.abs(offset.dot(up)) <= half ? distance : -1.0D;
    }

    private static void quad(PoseStack pose, Vec3 dir, double half, float[] first, float[] second, int cells)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Vec3 right = right(dir);
        Vec3 up = right.cross(dir).normalize();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        face(builder, matrix, right.scale(-half).add(up.scale(-half)), right.scale(2.0D * half), up.scale(2.0D * half),
                first, second, 1.0D, cells);
        BufferUploader.drawWithShader(builder.end());
    }

    private static void cube(PoseStack pose, double half, float[] first, float[] second, int cells)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        List<Side> sides = new ArrayList<>(SIDES);
        sides.sort(Comparator.comparingDouble(side -> -side.centre().lengthSqr()));

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Side side : sides)
        {
            face(builder, matrix, side.origin().scale(half), side.u().scale(half), side.v().scale(half),
                    first, second, shade(side.facing()), cells);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private record Side(Vec3 origin, Vec3 u, Vec3 v, Direction facing)
    {
        Vec3 centre()
        {
            return this.origin.add(this.u.scale(0.5D)).add(this.v.scale(0.5D));
        }
    }

    private static final List<Side> SIDES = List.of(
            new Side(new Vec3(-1.0D, -1.0D, -1.0D), new Vec3(0.0D, 0.0D, 2.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.WEST),
            new Side(new Vec3(1.0D, -1.0D, 1.0D), new Vec3(0.0D, 0.0D, -2.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.EAST),
            new Side(new Vec3(-1.0D, -1.0D, 1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, -2.0D), Direction.DOWN),
            new Side(new Vec3(-1.0D, 1.0D, -1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 2.0D), Direction.UP),
            new Side(new Vec3(1.0D, -1.0D, -1.0D), new Vec3(-2.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.NORTH),
            new Side(new Vec3(-1.0D, -1.0D, 1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.SOUTH));

    private static double shade(Direction facing)
    {
        if (!WhimConfig.renderFaceShade())
        {
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

    private static void face(BufferBuilder builder, Matrix4f matrix, Vec3 origin, Vec3 u, Vec3 v, float[] first,
            float[] second, double shade, int cells)
    {
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
