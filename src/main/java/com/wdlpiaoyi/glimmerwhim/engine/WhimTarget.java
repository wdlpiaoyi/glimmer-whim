package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.Locale;
import java.util.UUID;

import net.minecraft.world.phys.Vec3;

public record WhimTarget(UUID entity, Vec3 point)
{
    public String describe()
    {
        String position = this.point == null
                ? "?"
                : String.format(Locale.ROOT, "%.2f %.2f %.2f", this.point.x, this.point.y, this.point.z);

        return this.entity == null
                ? "pos " + position
                : "entity " + this.entity + " @ " + position;
    }
}
