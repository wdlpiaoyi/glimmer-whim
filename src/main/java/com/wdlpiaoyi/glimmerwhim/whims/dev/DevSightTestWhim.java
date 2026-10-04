package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.PosAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class DevSightTestWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_sighttest");

    public static final DevSightTestWhim INSTANCE = new DevSightTestWhim();

    private DevSightTestWhim()
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
        // blocks/entities 控制对应遮挡是否生效；geometry 选实体遮挡用碰撞箱还是渲染箱
        return WhimParams.of(WhimParam.choice("blocks", "true", "true", "false"),
                WhimParam.choice("entities", "false", "true", "false"),
                WhimParam.choice("geometry", "hitbox", "hitbox", "render"));
    }

    @Override
    public Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        // 固定生成在眼前 8 格；生成后位置不再跟随玩家
        Vec3 at = context.player().getEyePosition().add(context.player().getLookAngle().scale(8.0D));
        return Optional.of(new WhimSpawn(new PosAnchor(at), WhimData.EMPTY));
    }

    @Override
    public boolean requiresLineOfSight()
    {
        return true;
    }

    @Override
    public boolean occludedByBlocks(WhimData data)
    {
        return params().text(data, "blocks").equals("true");
    }

    @Override
    public boolean occludedByEntities(WhimData data)
    {
        return params().text(data, "entities").equals("true");
    }

    @Override
    public boolean entityOcclusionRenderBox(WhimData data)
    {
        return params().text(data, "geometry").equals("render");
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() != WhimEvent.Kind.USE)
        {
            return;
        }

        // 使用时把当前遮挡开关与瞄准目标打印回玩家，用于实测遮挡几何
        event.player().sendSystemMessage(Component.literal("dev_sighttest: blocks="
                + occludedByBlocks(event.whim().data()) + " entities=" + occludedByEntities(event.whim().data())
                + " geometry=" + (entityOcclusionRenderBox(event.whim().data()) ? "render" : "hitbox")
                + event.target().map(target -> " target=" + target.describe()).orElse("")));
        event.remove(WhimRemoveReason.USED);
    }
}
