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

public final class DevVoidTestAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim", "dev_voidtest");

    private static final String WRONG = "该锚不接受锚数据，固定于 0 0 0";

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

    public static DevVoidTestAnchor parse(CommandSourceStack source, String data)
    {
        if (data != null && !data.isBlank())
        {
            throw new IllegalArgumentException(WRONG);
        }

        return new DevVoidTestAnchor();
    }

    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of();
    }

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
