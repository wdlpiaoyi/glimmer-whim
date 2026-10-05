package com.wdlpiaoyi.glimmerwhim.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchors;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimLifecycle;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRegistry;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;
import com.wdlpiaoyi.glimmerwhim.whims.Whims;

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

public final class WhimCommand
{
    @FunctionalInterface
    private interface Action
    {
        int run(CommandSourceStack source, UUID target) throws CommandSyntaxException;
    }

    // 子动作表；命令树与 TAB 补全共用这里的键集合
    private static final Map<String, Action> ACTIONS = new LinkedHashMap<>();

    static
    {
        ACTIONS.put("get", WhimCommand::get);
        ACTIONS.put("kill", WhimCommand::kill);
    }

    private static final SimpleCommandExceptionType ERROR_PLAYER =
            new SimpleCommandExceptionType(Component.literal("该命令只能由玩家执行"));

    private static final DynamicCommandExceptionType ERROR_UUID =
            new DynamicCommandExceptionType(id -> Component.literal("无效的 uuid: " + id));

    private static final DynamicCommandExceptionType ERROR_NOT_FOUND =
            new DynamicCommandExceptionType(id -> Component.literal("未找到该灵感: " + id));

    private static final DynamicCommandExceptionType ERROR_DIMENSION =
            new DynamicCommandExceptionType(dimension -> Component.literal("未知维度: " + dimension));

    private static final DynamicCommandExceptionType ERROR_ANCHOR =
            new DynamicCommandExceptionType(type -> Component.literal("未知的锚类型: " + type + "（可用: " + anchorNames() + "）"));

    private static final DynamicCommandExceptionType ERROR_WHIM =
            new DynamicCommandExceptionType(type -> Component.literal("未知的灵感类型: " + type + "（可用: " + whimNames() + "）"));

    private static final DynamicCommandExceptionType ERROR_DATA =
            new DynamicCommandExceptionType(message -> Component.literal(String.valueOf(message)));

    private static final DynamicCommandExceptionType ERROR_PLACEMENT =
            new DynamicCommandExceptionType(type -> Component.literal("灵感 " + type + " 未声明生成位置，请指定锚类型"));

    private static final DynamicCommandExceptionType ERROR_ACTION =
            new DynamicCommandExceptionType(action -> Component.literal("无效的动作: " + action
                    + "（可用: " + String.join(", ", ACTIONS.keySet()) + "）"));

    private static final SimpleCommandExceptionType ERROR_NO_AIM =
            new SimpleCommandExceptionType(Component.literal("未瞄准任何灵感，请提供 uuid"));

    // 子命令说明；用法文本由命令树生成，这里只补一句人话
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "list", "列出该维度存活的灵感",
            "summon", "省略锚类型或写 default 时使用该灵感自身的生成规则；default 不接受锚数据；lifetime 默认取灵感自身，-1 = 永久；visibility 默认 all",
            "whim", "省略 uuid 时作用于当前瞄准的灵感");

    private WhimCommand()
    {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        // 根命令权限等级取自 COMMON 配置
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(GlimmerWhim.MODID)
                .requires(source -> source.hasPermission(WhimConfig.commandPermissionLevel()));

        root.then(Commands.literal("help")
                .executes(context -> help(context.getSource())));

        root.then(Commands.literal("list")
                .executes(context -> list(context.getSource(), context.getSource().getLevel().dimension()))
                .then(Commands.literal("all")
                        .executes(context -> listAll(context.getSource())))
                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                        // 补全服务器已加载的维度 id
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                context.getSource().getServer().levelKeys().stream()
                                        .map(key -> key.location().toString())
                                        .collect(Collectors.toList()),
                                builder))
                        .executes(context -> list(context.getSource(), ResourceKey.create(Registries.DIMENSION,
                                ResourceLocationArgument.getId(context, "dimension"))))));

        root.then(Commands.literal("summon")
                .then(Commands.argument("whim", StringArgumentType.word())
                        .suggests((context, builder) -> suggestTypes(builder))
                        .executes(context -> summon(context.getSource(),
                                StringArgumentType.getString(context, "whim"), null, null))
                        .then(Commands.argument("anchor", StringArgumentType.word())
                                .suggests((context, builder) -> suggestAnchors(builder))
                                .executes(context -> summon(context.getSource(),
                                        StringArgumentType.getString(context, "whim"),
                                        StringArgumentType.getString(context, "anchor"), null))
                                // data 为贪心字符串：锚数据与 {参数} 都包含在内
                                .then(Commands.argument("data", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> suggestTail(
                                                suggestionAnchor(StringArgumentType.getString(context, "anchor")),
                                                summonParams(StringArgumentType.getString(context, "whim")),
                                                summonDefaults(StringArgumentType.getString(context, "whim"),
                                                        context.getSource()),
                                                context.getSource(), builder))
                                        .executes(context -> summon(context.getSource(),
                                                StringArgumentType.getString(context, "whim"),
                                                StringArgumentType.getString(context, "anchor"),
                                                StringArgumentType.getString(context, "data")))))));

        root.then(Commands.literal("whim")
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) -> suggestWords(ACTIONS.keySet(), builder))
                        .executes(context -> act(context.getSource(), null,
                                StringArgumentType.getString(context, "action")))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(aliveIds(context.getSource()), builder))
                                .executes(context -> act(context.getSource(), uuid(context, "id"),
                                        StringArgumentType.getString(context, "action"))))));

        event.getDispatcher().register(root);
    }

    private static UUID aimed(CommandSourceStack source)
    {
        ServerPlayer player = source.getPlayer();
        UUID id = player == null ? null : WhimRegistry.aimed(player);

        // 瞄准目标须仍存活于注册表，否则视为无目标
        return id != null && WhimRegistry.find(id).isPresent() ? id : null;
    }

    private static List<String> aliveIds(CommandSourceStack source)
    {
        UUID id = aimed(source);

        return id == null ? List.of() : List.of(id.toString());
    }

    // 解析失败抛命令异常而非让命令崩溃
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

    private static Optional<ResourceLocation> resolve(String text)
    {
        try
        {
            // 无命名空间的 token 自动补 glimmerwhim: 前缀
            return Optional.of(text.indexOf(':') >= 0
                    ? ResourceLocation.parse(text)
                    : ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, text));
        }
        catch (RuntimeException e)
        {
            return Optional.empty();
        }
    }

    private static String anchorNames()
    {
        return WhimAnchors.types().stream().map(ResourceLocation::getPath).collect(Collectors.joining(", "));
    }

    private static String whimNames()
    {
        return Whims.ids().stream().map(ResourceLocation::getPath).collect(Collectors.joining(", "));
    }

    private static List<String> helpLines(CommandSourceStack source)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getCommands().getDispatcher();
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(GlimmerWhim.MODID);

        if (root == null)
        {
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        // 用法文本由命令树生成，新增或改名子命令不会与 help 脱节
        dispatcher.getSmartUsage(root, source)
                .forEach((node, usage) -> lines.add("  /" + usage + description(node.getName())));
        lines.add("  锚数据格式与 {参数} 可通过 TAB 查看");

        return lines;
    }

    private static String description(String name)
    {
        String text = DESCRIPTIONS.get(name);

        return text == null || text.isEmpty() ? "" : "（" + text + "）";
    }

    private static CompletableFuture<Suggestions> suggestTail(ResourceLocation anchor, WhimParams params,
            WhimData defaults, CommandSourceStack source, SuggestionsBuilder builder)
    {
        String remaining = builder.getRemaining();
        int open = remaining.lastIndexOf('{');

        // 花括号未闭合时补全 {参数}，否则补全锚数据
        if (open > remaining.lastIndexOf('}'))
        {
            String inside = remaining.substring(open + 1);
            int cut = Math.max(inside.lastIndexOf(','), inside.lastIndexOf(';')) + 1;

            return suggestParam(params, defaults, inside, inside.substring(cut),
                    builder.createOffset(builder.getStart() + open + 1 + cut));
        }

        int first = remaining.indexOf('{');
        String anchorPart = first < 0 ? remaining : remaining.substring(0, first);

        return suggestData(WhimAnchors.suggestData(anchor, source), anchorPart, builder);
    }

    private static boolean isWhim(String token)
    {
        ResourceLocation id = resolve(token).orElse(null);

        return id != null && Whims.contains(id);
    }

    private static boolean isAnchor(String token)
    {
        ResourceLocation id = resolve(token).orElse(null);

        return id != null && WhimAnchors.types().contains(id);
    }

    private static boolean isDefaultAnchor(String token)
    {
        return token != null && resolve(token).filter(WhimAnchors.DEFAULT::equals).isPresent();
    }

    private static WhimType resolveWhim(String token)
    {
        return Whims.get(resolve(token).orElse(null));
    }

    private static ResourceLocation suggestionAnchor(String second)
    {
        return second == null ? null : resolve(second).filter(WhimAnchors.types()::contains).orElse(null);
    }

    private static WhimParams summonParams(String first)
    {
        WhimType whim = first != null && isWhim(first) ? resolveWhim(first) : null;

        return whim == null ? WhimParams.NONE : whim.effectiveParams();
    }

    // 该灵感 spawn() 声明的初始数据；补全默认值与 summon 初始值同源
    private static WhimData summonDefaults(String first, CommandSourceStack source)
    {
        WhimType whim = first != null && isWhim(first) ? resolveWhim(first) : null;
        ServerPlayer player = source.getPlayer();

        if (whim == null || player == null)
        {
            return WhimData.EMPTY;
        }

        return whim.spawn(new WhimSpawnContext(player.serverLevel(), player, player.getRandom()))
                .map(WhimSpawn::data).orElse(WhimData.EMPTY);
    }

    private static CompletableFuture<Suggestions> suggestTypes(SuggestionsBuilder builder)
    {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (ResourceLocation type : Whims.ids())
        {
            if (type.getPath().toLowerCase(Locale.ROOT).startsWith(typed))
            {
                builder.suggest(type.getPath(), Component.literal("灵感"));
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestData(Collection<String> candidates, String typed,
            SuggestionsBuilder builder)
    {
        String[] tokens = typed.split(" ", -1);
        int index = tokens.length - 1;
        String partial = tokens[index];
        SuggestionsBuilder target = builder.createOffset(builder.getStart() + typed.length() - partial.length());
        Set<String> matched = new LinkedHashSet<>();

        for (String candidate : candidates)
        {
            String[] parts = candidate.split("\\s+");

            if (index < parts.length && parts[index].startsWith(partial))
            {
                // 逐段加长：第一次 TAB 补出当前段，之后的 TAB 依次续上后面的分量
                for (int end = index + 1; end <= parts.length; end++)
                {
                    matched.add(String.join(" ", Arrays.copyOfRange(parts, index, end)));
                }
            }
        }

        for (String part : matched)
        {
            target.suggest(part);
        }

        return target.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestParam(WhimParams params, WhimData defaults, String inside,
            String piece, SuggestionsBuilder builder)
    {
        // 分隔符规则与数据模型共用 WhimData
        int cut = WhimData.cut(piece);

        // 未写分隔符时提示参数名与默认值；已写则提示候选值
        if (cut < 0)
        {
            List<String> names = new ArrayList<>();

            for (WhimParam param : params.all())
            {
                if (!written(inside, param.name()))
                {
                    // 默认值优先取该灵感 spawn() 声明的初始数据
                    names.add(param.name() + ":" + defaults.get(param.name()).orElse(param.defaultValue()));
                }
            }

            return suggestText(names, builder);
        }

        WhimParam param = params.get(piece.substring(0, cut));

        return suggestText(param == null ? List.of() : param.choices(),
                builder.createOffset(builder.getStart() + cut + 1));
    }

    // 参数之间以逗号或分号分隔
    private static boolean written(String inside, String name)
    {
        for (String piece : WhimData.SEPARATOR.split(inside))
        {
            int cut = WhimData.cut(piece);
            String written = (cut < 0 ? piece : piece.substring(0, cut)).trim();

            if (written.equals(name))
            {
                return true;
            }
        }

        return false;
    }

    private static CompletableFuture<Suggestions> suggestAnchors(SuggestionsBuilder builder)
    {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (ResourceLocation type : WhimAnchors.types())
        {
            if (type.getPath().toLowerCase(Locale.ROOT).startsWith(typed))
            {
                builder.suggest(type.getPath(), Component.literal(WhimAnchors.hint(type)));
            }
        }

        return builder.buildFuture();
    }

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

    // 已完整输入某个候选时列出全部，否则只按前缀过滤
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

    private static int help(CommandSourceStack source)
    {
        for (String line : helpLines(source))
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
            source.sendSuccess(() -> Component.literal(dimension.location() + " 无存活的灵感"), false);
            return 0;
        }

        source.sendSuccess(() -> summary(dimension.location().toString(), whimes), false);
        return whimes.size();
    }

    private static int listAll(CommandSourceStack source)
    {
        Map<ResourceKey<Level>, Collection<Whim>> dimensions = WhimRegistry.allDimensions();
        int total = dimensions.values().stream().mapToInt(Collection::size).sum();

        if (total == 0)
        {
            source.sendSuccess(() -> Component.literal("所有维度均无存活的灵感"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("所有维度存活的灵感共 " + total + " 条："), false);

        for (Map.Entry<ResourceKey<Level>, Collection<Whim>> entry : dimensions.entrySet())
        {
            source.sendSuccess(() -> summary("  " + entry.getKey().location(), entry.getValue()), false);
        }

        return total;
    }

    private static MutableComponent summary(String name, Collection<Whim> whimes)
    {
        MutableComponent line = Component.literal(name + " 存活的灵感 " + whimes.size() + " 条：");

        for (Whim whim : whimes)
        {
            line.append(" ").append(link(whim.id())).append("(").append(whim.type().id().getPath()).append(")");
        }

        return line;
    }

    private static MutableComponent link(UUID id)
    {
        // 点击把命令填入聊天框，便于查看/操作该灵感
        String command = "/" + GlimmerWhim.MODID + " whim get " + id;

        return Component.literal(id.toString()).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(command + "\n点击填入聊天框"))));
    }

    private static String remaining(Whim whim, long now)
    {
        return whim.permanent() ? "永久" : whim.lifetime(now) + "t";
    }

    private static int act(CommandSourceStack source, UUID id, String action) throws CommandSyntaxException
    {
        // 省略 uuid 时作用于当前瞄准的灵感
        UUID target = id != null ? id : aimed(source);

        if (target == null)
        {
            throw ERROR_NO_AIM.create();
        }

        Action handler = ACTIONS.get(action);

        if (handler == null)
        {
            throw ERROR_ACTION.create(action);
        }

        return handler.run(source, target);
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
        source.sendSuccess(() -> Component.literal("  元素=" + whim.type().id()), false);
        source.sendSuccess(() -> Component.literal("  剩余=" + remaining(whim, source.getLevel().getGameTime())), false);

        String visibility = whim.visibility().describe();

        source.sendSuccess(() -> Component.literal("  可见性=" + visibility), false);

        WhimParams params = whim.type().params();

        source.sendSuccess(() -> Component.literal(paramsLine(params, whim.data())), false);

        return 1;
    }

    private static String paramsLine(WhimParams params, WhimData data)
    {
        if (params.all().isEmpty())
        {
            return "  参数=（该灵感不接受参数）";
        }

        // 未显式给出的参数回退默认值并标注（默认）
        return "  参数=" + params.all().stream()
                .map(param -> param.name() + "=" + params.text(data, param.name())
                        + (data.get(param.name()).isEmpty() ? "（默认）" : ""))
                .collect(Collectors.joining("，"));
    }

    private static int kill(CommandSourceStack source, UUID id) throws CommandSyntaxException
    {
        if (!WhimRegistry.kill(id))
        {
            throw ERROR_NOT_FOUND.create(id);
        }

        source.sendSuccess(() -> Component.literal("已移除灵感: " + id), true);
        return 1;
    }

    private static int summon(CommandSourceStack source, String first, String second, String tail)
            throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayer();

        if (player == null)
        {
            throw ERROR_PLAYER.create();
        }

        WhimType whim = null;

        if (first != null)
        {
            if (!isWhim(first))
            {
                throw ERROR_WHIM.create(first);
            }

            whim = resolveWhim(first);
        }

        if (whim == null)
        {
            throw ERROR_WHIM.create(first);
        }

        String rest = tail == null ? "" : tail;

        WhimData.Split split;
        WhimData userData;
        // 有效参数可能被灵感类型覆盖；解析 {参数} 时以它为准
        WhimParams params = whim.effectiveParams();

        try
        {
            split = WhimData.split(rest);
            userData = params.parse(split.data());
        }
        catch (IllegalArgumentException e)
        {
            throw ERROR_DATA.create(e.getMessage());
        }

        // 初始数据一律取自灵感自身的 spawn()，锚只决定位置
        WhimSpawn defaults = whim.spawn(new WhimSpawnContext(player.serverLevel(), player, player.getRandom()))
                .orElse(null);
        WhimData base = defaults == null ? WhimData.EMPTY : defaults.data();
        WhimSpawn placement;

        if (second == null || isDefaultAnchor(second))
        {
            // default 锚不接受锚数据
            if (second != null && !split.anchorData().isEmpty())
            {
                throw ERROR_DATA.create("default 锚不接受锚数据");
            }

            // 无 spawn() 规则时没有可用的生成位置
            if (defaults == null)
            {
                throw ERROR_PLACEMENT.create(whim.id().toString());
            }

            placement = new WhimSpawn(defaults.anchor(), base);
        }
        else
        {
            if (!isAnchor(second))
            {
                throw ERROR_ANCHOR.create(second);
            }

            try
            {
                // 显式锚类型：交给锚工厂按锚数据生成位置
                placement = new WhimSpawn(
                        WhimAnchors.create(resolve(second).orElse(null), source,
                                split.anchorData().isEmpty() ? null : split.anchorData()),
                        base);
            }
            catch (RuntimeException e)
            {
                throw ERROR_DATA.create(e.getMessage());
            }
        }

        Whim summoned;

        try
        {
            summoned = WhimLifecycle.summon(player.serverLevel(), player, whim, placement, userData);
        }
        catch (IllegalArgumentException e)
        {
            throw ERROR_DATA.create(e.getMessage());
        }

        source.sendSuccess(() -> Component.literal("已生成灵感 " + summoned.id() + "，"
                + (summoned.permanent() ? "永久" : "存活 " + summoned.lifetime(player.serverLevel().getGameTime()) + " tick")
                + (userData.isEmpty() ? "" : "，参数 " + userData)), true);
        return 1;
    }
}
