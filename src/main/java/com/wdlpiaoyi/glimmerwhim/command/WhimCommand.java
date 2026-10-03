package com.wdlpiaoyi.glimmerwhim.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.wdlpiaoyi.glimmerwhim.whim.Whim;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.whim.WhimTypes;
import com.wdlpiaoyi.glimmerwhim.whim.anchor.RayAnchor;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 调试入口。这版没有自动刷新。 */
public final class WhimCommand
{
    private static final String DEFAULT_ANCHOR = RayAnchor.TYPE.getPath();

    private static final int DEFAULT_TICKS = 1200;

    private static final List<String> USAGE = List.of(
            "  /glimmerwhim list [维度]",
            "  /glimmerwhim spawn [tick=" + DEFAULT_TICKS + "｜-1=永久] [锚类型=" + DEFAULT_ANCHOR + "] [锚数据] {参数}",
            "    " + DEFAULT_ANCHOR + " 的锚数据 dx dy dz，任一个写 ~ 就取视线；"
                    + Whim.DEV_ELEMENT.getPath() + " 参数 " + paramUsage(),
            "  /glimmerwhim whim <uuid> get｜kill");

    private static final SimpleCommandExceptionType ERROR_PLAYER =
            new SimpleCommandExceptionType(Component.literal("只能由玩家执行"));

    private static final DynamicCommandExceptionType ERROR_UUID =
            new DynamicCommandExceptionType(id -> Component.literal("不像是 uuid: " + id));

    private static final DynamicCommandExceptionType ERROR_NOT_FOUND =
            new DynamicCommandExceptionType(id -> Component.literal("没有这条灵感: " + id));

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
                .executes(context -> spawn(context.getSource(), DEFAULT_TICKS, DEFAULT_ANCHOR, null))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(-1))
                        .executes(context -> spawn(context.getSource(), IntegerArgumentType.getInteger(context, "ticks"),
                                DEFAULT_ANCHOR, null))
                        .then(Commands.argument("anchortype", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        WhimAnchors.types().stream()
                                                .map(ResourceLocation::getPath)
                                                .collect(Collectors.toList()),
                                        builder))
                                .executes(context -> spawn(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "ticks"),
                                        StringArgumentType.getString(context, "anchortype"), null))
                                .then(Commands.argument("data", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> suggestTail(
                                                WhimAnchors.resolve(StringArgumentType.getString(context, "anchortype"))
                                                        .orElse(null),
                                                context.getSource(), builder))
                                        .executes(context -> spawn(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "ticks"),
                                                StringArgumentType.getString(context, "anchortype"),
                                                StringArgumentType.getString(context, "data")))))));

        root.then(Commands.literal("whim")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(aliveIds(context.getSource()), builder))
                        .then(Commands.literal("get")
                                .executes(context -> get(context.getSource(),
                                        uuid(context, "id"))))
                        .then(Commands.literal("kill")
                                .executes(context -> kill(context.getSource(),
                                        uuid(context, "id"))))));

        event.getDispatcher().register(root);
    }

    /** 先给正瞄着的那条，再给这个维度里其它活着的。 */
    private static List<String> aliveIds(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            return List.of();
        }

        UUID aimed = WhimRegistry.aimed(player);
        List<String> ids = new ArrayList<>();

        if (aimed != null)
        {
            ids.add(aimed.toString());
        }

        for (Whim whim : WhimRegistry.of(player.serverLevel()).all())
        {
            if (!whim.id().equals(aimed))
            {
                ids.add(whim.id().toString());
            }
        }

        return ids;
    }

    private static UUID uuid(CommandContext<CommandSourceStack> context, String name)
            throws CommandSyntaxException
    {
        String text = StringArgumentType.getString(context, name);

        try
        {
            return UUID.fromString(text);
        }
        catch (IllegalArgumentException e)
        {
            throw ERROR_UUID.create(text);
        }
    }

    private static String anchorNames()
    {
        return WhimAnchors.types().stream().map(ResourceLocation::getPath).collect(Collectors.joining(", "));
    }

    private static String paramUsage()
    {
        return WhimTypes.params(Whim.DEV_ELEMENT).stream()
                .map(param -> param.name() + "=" + param.hint() + "（默认 " + param.defaultValue() + "）")
                .collect(Collectors.joining("，"));
    }

    /** 锚数据和灵感参数共用这一段尾巴，补全时两样都摆上。 */
    private static CompletableFuture<Suggestions> suggestTail(ResourceLocation anchor, CommandSourceStack source,
            SuggestionsBuilder builder)
    {
        String remaining = builder.getRemaining();
        int space = remaining.lastIndexOf(' ');
        SuggestionsBuilder tail = builder.createOffset(builder.getStart() + (space < 0 ? 0 : space + 1));

        for (String group : WhimTypes.suggestions(Whim.DEV_ELEMENT))
        {
            tail.suggest(group);
        }

        // 尾巴还空着，顺手把锚数据也摆上
        if (remaining.isBlank())
        {
            for (String data : WhimAnchors.suggestData(anchor, source))
            {
                tail.suggest(data);
            }
        }

        return tail.buildFuture();
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

        // 只报数，details 点 uuid 去看。
        MutableComponent line = Component.literal(dimension.location() + " 存活的灵感 " + whimes.size() + " 条：");

        for (Whim whim : whimes)
        {
            line.append(" ").append(link(whim.id()));
        }

        source.sendSuccess(() -> line, false);
        return whimes.size();
    }

    /** 一条 uuid 的可点文本，点了就是看它的详情。 */
    private static MutableComponent link(UUID id)
    {
        String command = "/glimmerwhim whim " + id + " get";

        return Component.literal(id.toString()).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    private static String remaining(Whim whim)
    {
        return whim.permanent() ? "永久" : whim.lifetime() + "t";
    }

    private static int get(CommandSourceStack source, UUID id) throws CommandSyntaxException
    {
        WhimRegistry.Found found = WhimRegistry.find(id).orElse(null);

        if (found == null)
        {
            throw ERROR_NOT_FOUND.create(id);
        }

        Whim whim = found.whim();

        source.sendSuccess(() -> Component.literal(whim.id().toString()), false);
        source.sendSuccess(() -> Component.literal("  维度=" + found.dimension().location()), false);
        source.sendSuccess(() -> Component.literal("  锚=" + whim.anchor().type()), false);
        source.sendSuccess(() -> Component.literal("  元素=" + whim.element()), false);
        source.sendSuccess(() -> Component.literal("  剩余=" + remaining(whim)), false);

        if (!whim.data().isEmpty())
        {
            source.sendSuccess(() -> Component.literal("  参数=" + whim.data()), false);
        }

        return 1;
    }

    private static int kill(CommandSourceStack source, UUID id) throws CommandSyntaxException
    {
        if (!WhimRegistry.kill(id))
        {
            throw ERROR_NOT_FOUND.create(id);
        }

        source.sendSuccess(() -> Component.literal("已干掉灵感 " + id), true);
        return 1;
    }

    private static int spawn(CommandSourceStack source, int ticks, String anchorType, String tail)
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

        // 尾巴里的 {名字:值} 是灵感参数，抠掉之后剩下的才是锚数据。
        WhimData.Split split;
        WhimData data;

        try
        {
            split = WhimData.split(tail);
            data = WhimTypes.parse(Whim.DEV_ELEMENT, split.data());
        }
        catch (IllegalArgumentException e)
        {
            throw ERROR_DATA.create(e.getMessage());
        }

        WhimAnchor anchor;

        try
        {
            anchor = WhimAnchors.create(type, source, split.anchorData().isEmpty() ? null : split.anchorData());
        }
        catch (RuntimeException e)
        {
            // 参数是玩家手打的，解析失败一律当用法错误报回去。
            throw ERROR_DATA.create(e.getMessage());
        }

        // 直接就是 tick 数，-1 是永久。
        Whim whim = new Whim(UUID.randomUUID(), anchor, Whim.DEV_ELEMENT, data, ticks);
        WhimRegistry.of(player.serverLevel()).spawn(whim);

        source.sendSuccess(() -> Component.literal("已生成灵感 " + whim.id() + "，"
                + (whim.permanent() ? "永久" : "存活 " + whim.lifetime() + " tick")
                + (data.isEmpty() ? "" : "，参数 " + data)), true);
        return 1;
    }
}
