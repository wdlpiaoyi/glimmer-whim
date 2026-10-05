package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.Optional;
import java.util.function.Predicate;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        // 两个 occlusion 开关控制对应遮挡是否生效；entity_geometry 选实体遮挡用碰撞箱还是渲染剔除框
        return WhimParams.of(WhimParam.choice("block_occlusion", "true", "true", "false"),
                WhimParam.choice("entity_occlusion", "false", "true", "false"),
                WhimParam.choice("entity_geometry", "hitbox", "hitbox", "render"));
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
        return params().text(data, "block_occlusion").equals("true");
    }

    @Override
    public Predicate<Entity> entityOccluders(WhimData data)
    {
        return params().text(data, "entity_occlusion").equals("true") ? entity -> entity instanceof LivingEntity : null;
    }

    @Override
    public boolean entityOcclusionRenderBox(WhimData data)
    {
        return params().text(data, "entity_geometry").equals("render");
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() != WhimEvent.Kind.USE)
        {
            return;
        }

        // 使用时把当前遮挡开关与瞄准目标打印回玩家，用于实测遮挡几何
        event.player().sendSystemMessage(Component.literal("dev_sighttest: block_occlusion="
                + occludedByBlocks(event.whim().data()) + " entity_occlusion=" + (entityOccluders(event.whim().data()) != null)
                + " entity_geometry=" + (entityOcclusionRenderBox(event.whim().data()) ? "render" : "hitbox")
                + event.target().map(target -> " target=" + target.describe()).orElse("")));
        event.remove(WhimRemoveReason.USED);
    }
}
