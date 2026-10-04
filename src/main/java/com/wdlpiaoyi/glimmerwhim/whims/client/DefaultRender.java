package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class DefaultRender
{
    private DefaultRender()
    {
    }

    public static void draw(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        boolean cube = "cube".equals(params.text(data, "shape", "cube"));
        double half = params.number(data, "size", 1.0D) / 2.0D;
        int cells = WhimConfig.renderDevCheckerCells();
        float[] first = WhimConfig.renderDevColorElement();
        float[] second = WhimConfig.renderDevColorElementAlt();

        if (cube)
        {
            cube(pose, half, first, second, cells);
        }
        else
        {
            quad(pose, dir, half, first, second, cells);
        }
    }

    public static void outline(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        double outline = WhimConfig.renderOutlineWidth();

        if (outline > 0.0D)
        {
            boolean cube = "cube".equals(params.text(data, "shape", "cube"));
            double half = params.number(data, "size", 1.0D) / 2.0D;
            int cells = WhimConfig.renderDevCheckerCells();
            float[] aimedColor = WhimConfig.renderColorAimed();

            if (cube)
            {
                cube(pose, half + outline, aimedColor, aimedColor, cells);
            }
            else
            {
                quad(pose, dir, half + outline, aimedColor, aimedColor, cells);
            }
        }

        draw(pose, dir, data, params);
    }

    public static void hue(PoseStack pose, Vec3 dir, WhimData data, WhimParams params)
    {
        boolean cube = "cube".equals(params.text(data, "shape", "cube"));
        double half = params.number(data, "size", 1.0D) / 2.0D;
        int cells = WhimConfig.renderDevCheckerCells();
        float shift = (float) ((System.currentTimeMillis() % 5000L) / 5000.0D);
        float[] first = rotateHue(WhimConfig.renderDevColorElement(), shift);
        float[] second = rotateHue(WhimConfig.renderDevColorElementAlt(), shift);

        if (cube)
        {
            cube(pose, half, first, second, cells);
        }
        else
        {
            quad(pose, dir, half, first, second, cells);
        }
    }

    static float[] rotateHue(float[] color, float shift)
    {
        double angle = shift * 2.0D * Math.PI;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double r = color[0];
        double g = color[1];
        double b = color[2];

        double nr = (0.213D + cos * 0.787D - sin * 0.213D) * r
                + (0.715D - cos * 0.715D - sin * 0.715D) * g
                + (0.072D - cos * 0.072D + sin * 0.928D) * b;
        double ng = (0.213D - cos * 0.213D + sin * 0.143D) * r
                + (0.715D + cos * 0.285D + sin * 0.140D) * g
                + (0.072D - cos * 0.072D - sin * 0.283D) * b;
        double nb = (0.213D - cos * 0.213D - sin * 0.787D) * r
                + (0.715D - cos * 0.715D + sin * 0.715D) * g
                + (0.072D + cos * 0.928D + sin * 0.072D) * b;

        return new float[] { clamp(nr), clamp(ng), clamp(nb), color[3] };
    }

    private static float clamp(double value)
    {
        return (float) Math.max(0.0D, Math.min(1.0D, value));
    }

    public static double hit(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
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
