package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 就在 0 0 0 待着，谁挨够一串高亮才还手 —— 试"多步条件 + 反应不止是移除"用的。
 * <p>
 * 条件（一串都得同一个人做）：高亮 → 取消高亮 → 再高亮 → 连着保持
 * {@code [anchor.dev_voidtest] holdTicks} 那么多 tick（默认 40，20 tick = 1 秒）。
 * 够了就往聊天栏发一条 test，顺手给他 {@code [anchor.dev_voidtest] damage} 点虚空伤害（默认 10 点 = 5 颗心），
 * 然后把进度清回原样，可以接着再试。
 * <p>
 * 状态全在这一条锚自己身上（一条灵感一个锚实例），引擎只负责在高亮、取消高亮、过 tick 的时候叫它一声。
 */
public final class DevVoidTestAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_voidtest");

    private static final String WRONG = "这种锚不收锚数据，它就在 0 0 0";

    /** 这串条件的进度。 */
    private enum Step
    {
        /** 还没人瞄过。 */
        IDLE,
        /** 瞄过一次了，就差"不瞄了"。 */
        SEEN,
        /** 中途松过眼，等再瞄上。 */
        WAITING,
        /** 又瞄上了，正在数 tick。 */
        HOLDING
    }

    private Step step = Step.IDLE;

    /** 正在做这串条件的玩家，整串必须同一个人。 */
    private UUID player;

    /** 这次高亮已经保持了多少 tick。 */
    private int held;

    /** 什么都不收。 */
    public static DevVoidTestAnchor parse(CommandSourceStack source, String data)
    {
        if (data != null && !data.isBlank())
        {
            throw new IllegalArgumentException(WRONG);
        }

        return new DevVoidTestAnchor();
    }

    /** 没得可补。 */
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of();
    }

    /**
     * 进包的那一份是给客户端算位置用的，进度不该跟着过去 —— 所以状态不写进包，读出来永远是刚出生那会儿。
     * <p>
     * 服务端这条实例是 {@code parse} 造出来的那个，进度就活在它身上。
     */
    public static DevVoidTestAnchor read(FriendlyByteBuf buf)
    {
        return new DevVoidTestAnchor();
    }

    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        // 进度不进包，所以没有字段要写。
    }

    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params)
    {
        return Optional.of(Vec3.ZERO);
    }

    @Override
    public void on(WhimEvent event)
    {
        switch (event.kind())
        {
            case HIGHLIGHT -> this.highlight(event.player());
            case UNHIGHLIGHT -> this.unhighlight();
            case TICK -> this.hold(event);
        }
    }

    /** 瞄上：接着上次那个人的"再瞄上"才算数，换了个人就从"瞄过一次"重头数。 */
    private void highlight(ServerPlayer who)
    {
        if (this.step == Step.WAITING && who.getUUID().equals(this.player))
        {
            this.step = Step.HOLDING;
        }
        else
        {
            this.step = Step.SEEN;
            this.player = who.getUUID();
        }

        this.held = 0;
    }

    /** 松眼：已经瞄过、正在数 tick 的，都退回去等再瞄上。 */
    private void unhighlight()
    {
        if (this.step == Step.SEEN || this.step == Step.HOLDING)
        {
            this.step = Step.WAITING;
        }

        this.held = 0;
    }

    /** 一秒 20 下 TICK，数够配置里 {@code [anchor.dev_voidtest] holdTicks} 下就还手。 */
    private void hold(WhimEvent event)
    {
        if (this.step != Step.HOLDING || ++this.held < WhimConfig.voidTestHoldTicks())
        {
            return;
        }

        this.step = Step.IDLE;
        this.held = 0;

        // 按 uuid 现取，别攥着玩家对象不放（中间他退了，这串也就不作数了）。
        ServerPlayer target = event.level().getServer().getPlayerList().getPlayer(this.player);

        if (target != null)
        {
            target.sendSystemMessage(Component.literal("test"));
            target.hurt(target.damageSources().fellOutOfWorld(), (float) WhimConfig.voidTestDamage());
        }
    }
}
