package com.wdlpiaoyi.glimmerwhim.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.wdlpiaoyi.glimmerwhim.whim.WhimParam;
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

    /** uuid 后面那两个动作。写成 argument 而不是 literal —— literal 补完就没了，TAB 翻不到下一个。 */
    private static final List<String> ACTIONS = List.of("get", "kill");

    private static final List<String> USAGE = List.of(
            "  /glimmerwhim list [维度|all]",
            "  /glimmerwhim spawn [tick=" + DEFAULT_TICKS + "｜-1=永久] [锚类型=" + DEFAULT_ANCHOR + "] [锚数据] {参数}",
            "    " + DEFAULT_ANCHOR + " 的锚数据 dx dy dz，任一个写 ~ 就取视线；"
                    + Whim.DEV_ELEMENT.getPath() + " 参数 " + paramUsage(),
            "  /glimmerwhim whim <uuid> " + String.join("｜", ACTIONS));

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

    private static final DynamicCommandExceptionType ERROR_ACTION =
            new DynamicCommandExceptionType(action -> Component.literal("不像是动作: " + action
                    + "（可用: " + String.join(", ", ACTIONS) + "）"));

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
                .then(Commands.literal("all")
                        .executes(context -> listAll(context.getSource())))
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
                        .then(Commands.argument("action", StringArgumentType.word())
                                .suggests((context, builder) -> suggestWords(ACTIONS, builder))
                                .executes(context -> act(context.getSource(), uuid(context, "id"),
                                        StringArgumentType.getString(context, "action"))))));

        event.getDispatcher().register(root);
    }

    /** 只给正瞄着的那条。别的一条都不列 —— list 里那些 uuid 点一下就能填进聊天框。 */
    private static List<String> aliveIds(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            return List.of();
        }

        UUID aimed = WhimRegistry.aimed(player);

        // 瞄着的那条可能已经走了，AIMED 里还留着旧 id，所以得真查一遍。
        if (aimed == null || WhimRegistry.find(aimed).isEmpty())
        {
            return List.of();
        }

        return List.of(aimed.toString());
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

    /**
     * 尾巴是"锚数据（可选）+ 一串 {参数}"。补全只看光标在不在花括号里：
     * 在里头就补这一组参数，不在就只补锚数据 —— 不许在人家写锚数据的时候把 {shape:cube} 摆过来。
     * <p>
     * 锚数据只在还没动笔的时候给一遍：写过了就不再冒，不然 ~ ~ ~ 写完一按 TAB 还是它。
     */
    private static CompletableFuture<Suggestions> suggestTail(ResourceLocation anchor, CommandSourceStack source,
            SuggestionsBuilder builder)
    {
        String remaining = builder.getRemaining();
        int open = remaining.lastIndexOf('{');

        if (open > remaining.lastIndexOf('}'))
        {
            String inside = remaining.substring(open + 1);
            int cut = Math.max(inside.lastIndexOf(','), inside.lastIndexOf(';')) + 1;

            return suggestParam(inside, inside.substring(cut),
                    builder.createOffset(builder.getStart() + open + 1 + cut));
        }

        // 还没进花括号：锚数据只在还没动笔的时候给一遍，写了就不再冒。
        int first = remaining.indexOf('{');
        String anchorPart = first < 0 ? remaining : remaining.substring(0, first);

        if (!anchorPart.isBlank())
        {
            return Suggestions.empty();
        }

        // 插在光标这儿（尾巴末尾），别拿整段尾巴去顶，不然会把已经写的吃掉。
        return suggestText(WhimAnchors.suggestData(anchor, source),
                builder.createOffset(builder.getStart() + remaining.length()));
    }

    /**
     * 花括号里的那一段：还没写冒号就是在挑名字，写了就是在挑值。
     * <p>
     * 挑名字的时候直接给一整对（名:默认值）：只给 shape: 的话，补出来就停在冒号上，得再按一次 TAB
     * 才看得见值 —— 而且那一次表里还是 shape: 和 size:，看着就像卡住了。
     */
    private static CompletableFuture<Suggestions> suggestParam(String inside, String piece, SuggestionsBuilder builder)
    {
        int cut = Math.max(piece.indexOf(':'), piece.indexOf('='));

        if (cut < 0)
        {
            List<String> names = new ArrayList<>();

            for (WhimParam param : WhimTypes.params(Whim.DEV_ELEMENT))
            {
                if (!written(inside, param.name()))
                {
                    names.add(param.name() + ":" + param.defaultValue());
                }
            }

            return suggestText(names, builder);
        }

        WhimParam param = WhimTypes.param(Whim.DEV_ELEMENT, piece.substring(0, cut));

        return suggestText(param == null ? List.of() : param.choices(),
                builder.createOffset(builder.getStart() + cut + 1));
    }

    /** 这一组里已经写过这个名字没有。 */
    private static boolean written(String inside, String name)
    {
        for (String piece : inside.split("[,;]"))
        {
            int cut = Math.max(piece.indexOf(':'), piece.indexOf('='));
            String written = (cut < 0 ? piece : piece.substring(0, cut)).trim();

            if (written.equals(name))
            {
                return true;
            }
        }

        return false;
    }

    /** 按已经打出来的字过一遍，跟原版一个脾气。 */
    private static CompletableFuture<Suggestions> suggestText(Collection<String> candidates, SuggestionsBuilder builder)
    {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (String candidate : candidates)
        {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(typed))
            {
                builder.suggest(candidate);
            }
        }

        return builder.buildFuture();
    }

    /**
     * 跟 {@link #suggestText} 一样，但已经打得一模一样的那个会留在表里。
     * <p>
     * 名字补全完就只剩一个候选，那一按 TAB 就把表关了 —— 想翻到另一个（get 换 kill）就没路了。
     */
    private static CompletableFuture<Suggestions> suggestWords(Collection<String> candidates, SuggestionsBuilder builder)
    {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        boolean exact = false;

        for (String candidate : candidates)
        {
            if (candidate.toLowerCase(Locale.ROOT).equals(typed))
            {
                exact = true;
                break;
            }
        }

        for (String candidate : candidates)
        {
            if (exact || candidate.toLowerCase(Locale.ROOT).startsWith(typed))
            {
                builder.suggest(candidate);
            }
        }

        return builder.buildFuture();
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
        source.sendSuccess(() -> summary(dimension.location().toString(), whimes), false);
        return whimes.size();
    }

    /** 所有有表的维度，一起看。 */
    private static int listAll(CommandSourceStack source)
    {
        Map<ResourceKey<Level>, Collection<Whim>> dimensions = WhimRegistry.allDimensions();
        int total = dimensions.values().stream().mapToInt(Collection::size).sum();

        if (total == 0)
        {
            source.sendSuccess(() -> Component.literal("哪个维度都没有存活的灵感"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("所有维度存活的灵感 " + total + " 条："), false);

        for (Map.Entry<ResourceKey<Level>, Collection<Whim>> entry : dimensions.entrySet())
        {
            source.sendSuccess(() -> summary("  " + entry.getKey().location(), entry.getValue()), false);
        }

        return total;
    }

    /** 一行：维度 + 条数 + 一串可点的 uuid。 */
    private static MutableComponent summary(String name, Collection<Whim> whimes)
    {
        MutableComponent line = Component.literal(name + " 存活的灵感 " + whimes.size() + " 条：");

        for (Whim whim : whimes)
        {
            line.append(" ").append(link(whim.id()));
        }

        return line;
    }

    /** 一条 uuid 的可点文本：点一下把命令填进聊天框，不是直接跑 —— 想 kill 就把 get 改掉。 */
    private static MutableComponent link(UUID id)
    {
        String command = "/glimmerwhim whim " + id + " get";

        return Component.literal(id.toString()).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(command + "\n点一下填进聊天框"))));
    }

    private static String remaining(Whim whim)
    {
        return whim.permanent() ? "永久" : whim.lifetime() + "t";
    }

    /** uuid 后面那个动作。 */
    private static int act(CommandSourceStack source, UUID id, String action) throws CommandSyntaxException
    {
        return switch (action)
        {
            case "get" -> get(source, id);
            case "kill" -> kill(source, id);
            default -> throw ERROR_ACTION.create(action);
        };
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

        source.sendSuccess(() -> Component.literal("已移除灵感 " + id), true);
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
