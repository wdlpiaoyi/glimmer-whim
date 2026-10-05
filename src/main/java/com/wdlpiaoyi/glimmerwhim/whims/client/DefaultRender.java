package com.wdlpiaoyi.glimmerwhim.whims.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimShapes;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

// 默认外观与默认命中体积；{size} 是这里与 Glow 共用的尺寸约定
public final class DefaultRender
{
    private DefaultRender()
    {
    }

    // ---- 具名外观（WhimRenderer.Drawer）----

    public static void quad(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        quadGeometry(pose, dir, half(data, params), WhimConfig.renderDefaultColorElement(),
                WhimConfig.renderDefaultColorElementAlt(), WhimConfig.renderDefaultCheckerCells());
    }

    public static void cube(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        cubeGeometry(pose, half(data, params), WhimConfig.renderDefaultColorElement(),
                WhimConfig.renderDefaultColorElementAlt(), WhimConfig.renderDefaultCheckerCells());
    }

    // ---- 具名命中体积（WhimRenderer.Hit）；返回射线命中距离，未命中为 -1 ----

    public static double hitQuad(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        return quadTest(eye, look, at, hitHalf(data, params));
    }

    public static double hitBox(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        return boxTest(eye, look, at, hitHalf(data, params));
    }

    public static double hitSphere(Vec3 eye, Vec3 look, Vec3 at, WhimData data, WhimParams params)
    {
        return sphere(eye, look, at, hitHalf(data, params));
    }

    // 球形命中，按给定半径；从球内起算时返回 0
    public static double sphere(Vec3 eye, Vec3 look, Vec3 at, double radius)
    {
        Vec3 offset = eye.subtract(at);
        double b = 2.0D * offset.dot(look);
        double discriminant = b * b - 4.0D * (offset.lengthSqr() - radius * radius);

        if (discriminant < 0.0D)
        {
            return -1.0D;
        }

        double root = Math.sqrt(discriminant);
        double near = (-b - root) / 2.0D;

        return (-b + root) / 2.0D < 0.0D ? -1.0D : Math.max(near, 0.0D);
    }

    // ---- 默认瞄准高亮：先用瞄准色画外扩 outline 宽的壳，再画本体；自定义外观请自备 highlight ----

    public static void outline(PoseStack pose, Vec3 dir, WhimData data, WhimParams params, UUID id)
    {
        boolean cube = WhimShapes.CUBE.toString().equals(params.text(data, Whim.SHAPE));
        double outline = WhimConfig.renderOutlineWidth();
        double half = half(data, params);

        if (outline > 0.0D)
        {
            int cells = WhimConfig.renderDefaultCheckerCells();
            float[] aimedColor = WhimConfig.renderColorAimed();

            if (cube)
            {
                cubeGeometry(pose, half + outline, aimedColor, aimedColor, cells);
            }
            else
            {
                quadGeometry(pose, dir, half + outline, aimedColor, aimedColor, cells);
            }
        }

        if (cube)
        {
            cube(pose, dir, data, params, id);
        }
        else
        {
            quad(pose, dir, data, params, id);
        }
    }

    // 保亮度的色相旋转（RGB→YIQ 旋转→RGB），shift 取 [0,1)
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

    private static double half(WhimData data, WhimParams params)
    {
        return params.number(data, "size", 1.0D) / 2.0D;
    }

    // 判定用的半边长：本体大小按 {hit_scale} 缩放，画法不受它影响
    private static double hitHalf(WhimData data, WhimParams params)
    {
        return half(data, params) * params.number(data, Whim.HIT_SCALE, 1.0D);
    }

    // slab 法求射线与 AABB 的最近交点
    private static double boxTest(Vec3 eye, Vec3 look, Vec3 at, double half)
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

    // 面片正对视线；背向、距离非正或偏离面心超半宽即未命中
    private static double quadTest(Vec3 eye, Vec3 look, Vec3 at, double half)
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

    // 以视线为法线构建面向玩家的四边形面片
    private static void quadGeometry(PoseStack pose, Vec3 dir, double half, float[] first, float[] second, int cells)
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

    private static void cubeGeometry(PoseStack pose, double half, float[] first, float[] second, int cells)
    {
        Matrix4f matrix = pose.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        // 按面心到原点距离降序绘制（画家算法，由远及近）
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

    // 单位立方体一面：origin/u/v 描述面，facing 供明暗
    private record Side(Vec3 origin, Vec3 u, Vec3 v, Direction facing)
    {
        Vec3 centre()
        {
            return this.origin.add(this.u.scale(0.5D)).add(this.v.scale(0.5D));
        }
    }

    // 六个面按原版朝向给明暗
    private static final List<Side> SIDES = List.of(
            new Side(new Vec3(-1.0D, -1.0D, -1.0D), new Vec3(0.0D, 0.0D, 2.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.WEST),
            new Side(new Vec3(1.0D, -1.0D, 1.0D), new Vec3(0.0D, 0.0D, -2.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.EAST),
            new Side(new Vec3(-1.0D, -1.0D, 1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, -2.0D), Direction.DOWN),
            new Side(new Vec3(-1.0D, 1.0D, -1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 2.0D), Direction.UP),
            new Side(new Vec3(1.0D, -1.0D, -1.0D), new Vec3(-2.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.NORTH),
            new Side(new Vec3(-1.0D, -1.0D, 1.0D), new Vec3(2.0D, 0.0D, 0.0D), new Vec3(0.0D, 2.0D, 0.0D), Direction.SOUTH));

    // 复刻原版面明暗：上 1.0、下 0.5、南北 0.8、东西 0.6
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
        // 每面切 cells×cells 小格，(i+j) 奇偶交替两色形成棋盘
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

    // 仅缩放 RGB，保留 alpha
    private static float[] tint(float[] color, double shade)
    {
        return new float[] { (float) (color[0] * shade), (float) (color[1] * shade), (float) (color[2] * shade),
                color[3] };
    }

    // 视线与世界上方向叉乘得右向；视线竖直时兜底 +X
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
