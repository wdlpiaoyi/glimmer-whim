package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// 交互目标种类登记表：种类 id + 提示 + 服务端校验 + payload 的位置/描述语义；新种类只需这里一行
public final class WhimTargets
{
    // 内置坐标 payload 的键；也供轨迹终点与射程校验读取
    static final String X = "x";
    static final String Y = "y";
    static final String Z = "z";
    // 内置种类各自的标识键
    static final String ENTITY_KEY = "entity";
    static final String WHIM_KEY = "whim";

    public record Kind(ResourceLocation id, String hint, BiPredicate<ServerPlayer, WhimData> valid,
            Function<WhimData, Optional<Vec3>> position, Function<WhimData, String> describe)
    {
    }

    private static final Map<ResourceLocation, Kind> KINDS = new LinkedHashMap<>();

    public static final Kind POINT = register("point", "视线命中的位置", (player, data) -> true);

    public static final Kind ENTITY = register("entity", "视线命中的实体",
            (player, data) -> entity(data).map(id -> player.serverLevel().getEntities().get(id) != null).orElse(false));

    public static final Kind WHIM = register("whim", "视线命中的灵感",
            (player, data) -> whim(data).map(id -> WhimRegistry.visible(player, id)).orElse(false));

    // 常用情形：payload 就是 x/y/z 命中点
    public static Kind register(String name, String hint, BiPredicate<ServerPlayer, WhimData> valid)
    {
        return register(ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, name), hint, valid);
    }

    public static Kind register(ResourceLocation id, String hint, BiPredicate<ServerPlayer, WhimData> valid)
    {
        return register(id, hint, valid, WhimTargets::position);
    }

    // 自定义位置语义即可，描述自动是「种类 @ 坐标」
    public static Kind register(ResourceLocation id, String hint, BiPredicate<ServerPlayer, WhimData> valid,
            Function<WhimData, Optional<Vec3>> position)
    {
        return register(id, hint, valid, position,
                data -> id.getPath() + " @ " + position.apply(data).map(WhimTargets::format).orElse("?"));
    }

    public static Kind register(ResourceLocation id, String hint, BiPredicate<ServerPlayer, WhimData> valid,
            Function<WhimData, Optional<Vec3>> position, Function<WhimData, String> describe)
    {
        KINDS.putIfAbsent(id, new Kind(id, hint, valid, position, describe));

        return KINDS.get(id);
    }

    public static Kind get(ResourceLocation id)
    {
        return KINDS.get(id);
    }

    public static Collection<Kind> kinds()
    {
        return KINDS.values();
    }

    static WhimData position(Vec3 point)
    {
        return WhimData.of(X, Double.toString(point.x))
                .with(Y, Double.toString(point.y))
                .with(Z, Double.toString(point.z));
    }

    public static Optional<Vec3> position(WhimData data)
    {
        Optional<Double> x = number(data, X);
        Optional<Double> y = number(data, Y);
        Optional<Double> z = number(data, Z);

        return x.isPresent() && y.isPresent() && z.isPresent()
                ? Optional.of(new Vec3(x.get(), y.get(), z.get()))
                : Optional.empty();
    }

    public static Optional<UUID> entity(WhimData data)
    {
        return uuid(data, ENTITY_KEY);
    }

    public static Optional<UUID> whim(WhimData data)
    {
        return uuid(data, WHIM_KEY);
    }

    private static String format(Vec3 at)
    {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", at.x, at.y, at.z);
    }

    private static Optional<UUID> uuid(WhimData data, String key)
    {
        try
        {
            return data.get(key).map(UUID::fromString);
        }
        catch (IllegalArgumentException exception)
        {
            return Optional.empty();
        }
    }

    private static Optional<Double> number(WhimData data, String key)
    {
        try
        {
            return data.get(key).map(Double::parseDouble);
        }
        catch (NumberFormatException exception)
        {
            return Optional.empty();
        }
    }

    private WhimTargets()
    {
    }
}
