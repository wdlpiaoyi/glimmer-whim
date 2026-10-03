package com.wdlpiaoyi.glimmerwhim.command;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.wdlpiaoyi.glimmerwhim.whim.Whim;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.RayAnchor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 调试入口。这不是玩法 —— 刷新条件是你的事，这版没有自动刷新。
 */
public final class WhimCommand
{
    private static final String DEFAULT_ANCHOR = RayAnchor.TYPE.getPath();

    private static final List<String> USAGE = List.of(
            "用法:",
            "  /glimmerwhim list [dimension] —— 列出该维度中存活的灵感",
            "  /glimmerwhim spawn [seconds] [anchortype] [anchordata] —— 生成一条灵感",
            "参数:",
            "  seconds —— 存活秒数，默认 60",
            "  anchortype —— 锚类型，默认 " + DEFAULT_ANCHOR,
            "  anchordata —— 锚的数据；" + DEFAULT_ANCHOR + " 的格式是 dx dy dz，省略则取执行者视线方向");

    private static final SimpleCommandExceptionType ERROR_PLAYER =
            new SimpleCommandExceptionType(Component.literal("只能由玩家执行"));

    private static final DynamicCommandExceptionType ERROR_DIMENSION =
            new DynamicCommandExceptionType(dimension -> Component.literal("未知维度: " + dimension));

    private static final DynamicCommandExceptionType ERROR_ANCHOR =
            new DynamicCommandExceptionType(type -> Component.literal("未知的锚类型: " + type + "（可用: " + anchorNames() + "）"));

    private static final DynamicCommandExceptionType ERROR_DATA =
            new DynamicCommandExceptionType(message -> Component.literal(String.valueOf(message)));

    private WhimCommand()
    {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("glimmerwhim")
                .requires(source -> source.hasPermission(2))
                .executes(context -> usage(context.getSource()));

        root.then(Commands.literal("list")
                .executes(context -> list(context.getSource(), context.getSource().getLevel().dimension()))
                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().levelKeys().stream()
                                        .map(key -> key.location().toString())
                                        .collect(Collectors.toList()),
                                builder))
                        .executes(context -> list(context.getSource(), ResourceKey.create(Registries.DIMENSION,
                                ResourceLocationArgument.getId(context, "dimension"))))));

        root.then(Commands.literal("spawn")
                .executes(context -> spawn(context.getSource(), 60.0D, DEFAULT_ANCHOR, null))
                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.0D))
                        .executes(context -> spawn(context.getSource(), DoubleArgumentType.getDouble(context, "seconds"),
                                DEFAULT_ANCHOR, null))
                        .then(Commands.argument("anchortype", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        WhimAnchors.types().stream()
                                                .map(ResourceLocation::getPath)
                                                .collect(Collectors.toList()),
                                        builder))
                                .executes(context -> spawn(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "seconds"),
                                        StringArgumentType.getString(context, "anchortype"), null))
                                .then(Commands.argument("anchordata", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                WhimAnchors.suggestData(
                                                        WhimAnchors.resolve(StringArgumentType.getString(context, "anchortype"))
                                                                .orElse(null),
                                                        context.getSource()),
                                                builder))
                                        .executes(context -> spawn(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "seconds"),
                                                StringArgumentType.getString(context, "anchortype"),
                                                StringArgumentType.getString(context, "anchordata")))))));

        event.getDispatcher().register(root);
    }

    private static String anchorNames()
    {
        return WhimAnchors.types().stream().map(ResourceLocation::getPath).collect(Collectors.joining(", "));
    }

    private static String formatSeconds(double seconds)
    {
        return seconds == Math.rint(seconds)
                ? Long.toString((long) seconds)
                : String.format(Locale.ROOT, "%.1f", seconds);
    }

    private static int usage(CommandSourceStack source)
    {
        for (String line : USAGE)
        {
            source.sendSuccess(() -> Component.literal(line), false);
        }

        return 0;
    }

    private static int list(CommandSourceStack source, ResourceKey<Level> dimension) throws CommandSyntaxException
    {
        ServerLevel level = source.getServer().getLevel(dimension);

        if (level == null)
        {
            throw ERROR_DIMENSION.create(dimension.location());
        }

        Collection<Whim> whimes = WhimRegistry.of(level).all();

        if (whimes.isEmpty())
        {
            source.sendSuccess(() -> Component.literal(dimension.location() + " 没有存活的灵感"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(dimension.location() + " 存活的灵感 " + whimes.size() + " 条"), false);

        for (Whim whim : whimes)
        {
            String line = "  " + whim.id() + "  锚=" + whim.anchor().type() + "  元素=" + whim.element()
                    + "  剩余=" + (whim.lifetime() / 20) + "s";
            source.sendSuccess(() -> Component.literal(line), false);
        }

        return whimes.size();
    }

    private static int spawn(CommandSourceStack source, double seconds, String anchorType, String anchorData)
            throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            throw ERROR_PLAYER.create();
        }

        ResourceLocation type = WhimAnchors.resolve(anchorType).orElse(null);

        if (type == null || !WhimAnchors.types().contains(type))
        {
            throw ERROR_ANCHOR.create(anchorType);
        }

        WhimAnchor anchor;

        try
        {
            anchor = WhimAnchors.create(type, source, anchorData);
        }
        catch (RuntimeException e)
        {
            // 参数是玩家手打的，解析失败一律当用法错误报回去。
            throw ERROR_DATA.create(e.getMessage());
        }

        int lifetime = (int) Math.round(seconds * 20.0D);
        Whim whim = new Whim(UUID.randomUUID(), anchor, Whim.DEV_ELEMENT, lifetime);
        WhimRegistry.of(player.serverLevel()).spawn(whim);

        source.sendSuccess(() -> Component.literal("已生成灵感 " + whim.id() + "，存活 " + formatSeconds(seconds) + " 秒"), true);
        return 1;
    }
}
