package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.config.WhimConfig;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class VoidTestWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_voidtest");

    public static final VoidTestWhim INSTANCE = new VoidTestWhim();

    private enum Step
    {
        IDLE,
        SEEN,
        WAITING,
        HOLDING
    }

    private Step step = Step.IDLE;

    private UUID player;

    private int held;

    private VoidTestWhim()
    {
    }

    @Override
    public ResourceLocation id()
    {
        return ID;
    }

    @Override
    public WhimParams params()
    {
        return WhimParams.NONE;
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

    private void unhighlight()
    {
        if (this.step == Step.SEEN || this.step == Step.HOLDING)
        {
            this.step = Step.WAITING;
        }

        this.held = 0;
    }

    private void hold(WhimEvent event)
    {
        if (this.step != Step.HOLDING || ++this.held < WhimConfig.voidTestHoldTicks())
        {
            return;
        }

        this.step = Step.IDLE;
        this.held = 0;

        ServerPlayer target = event.level().getServer().getPlayerList().getPlayer(this.player);

        if (target != null)
        {
            target.sendSystemMessage(Component.literal("test"));
            target.hurt(target.damageSources().fellOutOfWorld(), (float) WhimConfig.voidTestDamage());
        }
    }
}
