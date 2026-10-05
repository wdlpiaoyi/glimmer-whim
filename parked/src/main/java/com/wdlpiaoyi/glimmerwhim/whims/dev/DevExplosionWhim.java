package com.wdlpiaoyi.glimmerwhim.whims.dev;

import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.PosAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.WhimChain;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DevExplosionWhim implements WhimType
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "dev_explosion");

    public static final DevExplosionWhim INSTANCE = new DevExplosionWhim();

    private DevExplosionWhim()
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
        // radius 为基础爆炸半径（受 RANGE 加成），damage 为基础伤害（受 POWER 倍率）
        return WhimParams.of(WhimParam.positiveNumber("radius", "4"), WhimParam.positiveNumber("damage", "8"));
    }

    @Override
    public Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        Vec3 at = context.player().getEyePosition().add(context.player().getLookAngle().scale(8.0D));
        return Optional.of(new WhimSpawn(new PosAnchor(at), WhimData.EMPTY));
    }

    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() != WhimEvent.Kind.USE)
        {
            return;
        }

        WhimTarget target = event.target().orElse(null);
        Vec3 at = target == null ? null
                : resolveTarget(event.player(), target).map(Entity::position).orElse(target.point().orElse(null));

        if (at == null)
        {
            event.remove(WhimRemoveReason.DROPPED);
            return;
        }

        WhimData data = event.whim().data();
        WhimChain chain = event.chain().orElse(null);
        double radius = params().number(data, "radius") + (chain == null ? 0.0D : chain.value(DevDomains.RANGE));
        double damage = params().number(data, "damage") * (chain == null ? 1.0D : chain.value(DevDomains.POWER));

        ServerLevel level = event.level();
        Explosion explosion = level.explode(event.player(), at.x, at.y, at.z, (float) radius,
                Level.ExplosionInteraction.TNT);

        // power 域折算成原版爆炸伤害之外对半径内生物的追加伤害
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius)))
        {
            living.hurt(level.damageSources().explosion(explosion), (float) damage);
        }

        event.remove(WhimRemoveReason.USED);
    }
}
