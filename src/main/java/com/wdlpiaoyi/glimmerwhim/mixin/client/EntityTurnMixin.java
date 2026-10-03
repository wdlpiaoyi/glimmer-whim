package com.wdlpiaoyi.glimmerwhim.mixin.client;

import com.wdlpiaoyi.glimmerwhim.client.FreeLook;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 自由视角开着的时候，鼠标转的是镜头，不是人。
 * <p>
 * 挑 {@code Entity.turn} 下手是因为它是最后一站：鼠标那一串（灵敏度、平滑镜头、0.15）在这里已经算完，
 * 数量和原版一模一样，我们照抄就能有同一份手感；掐在这儿，人就不会跟着转。
 * <p>
 * 人不动了，可"看着哪儿"还得有个准信：准星判定（{@code Entity.pick}）、方块描边、我们自己的瞄准
 * 都是问 {@code getViewVector} 要方向的，所以那块也得改成听镜头的 —— 不然画面正中间那根准星
 * 指着的东西，和真正选中的东西会是两样。
 */
@Mixin(Entity.class)
public abstract class EntityTurnMixin
{
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void glimmerwhim$freeLook(double yRot, double xRot, CallbackInfo ci)
    {
        if (FreeLook.turn(yRot, xRot))
        {
            ci.cancel();
        }
    }

    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void glimmerwhim$viewVector(float partialTick, CallbackInfoReturnable<Vec3> cir)
    {
        if (FreeLook.active() && (Object) this == Minecraft.getInstance().player)
        {
            cir.setReturnValue(Vec3.directionFromRotation(FreeLook.pitch(), FreeLook.yaw()));
        }
    }
}
