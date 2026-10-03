package com.wdlpiaoyi.glimmerwhim.mixin.client;

import com.wdlpiaoyi.glimmerwhim.client.FreeLook;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 自由视角开着的时候，"你正看着哪儿"这个问题得由镜头回答。
 * <p>
 * 挑 {@code LocalPlayer.getViewXRot / getViewYRot} 下手，是因为它们就是这个问题在代码里的出海口：
 * 准星判定（{@code Entity.pick} → {@code Entity.getViewVector} → 问问这两个）、
 * 手里的东西怎么摆、我们自己的瞄准，问的都是这两个。
 * <p>
 * 人自己的朝向（{@code getYRot} / {@code getXRot}）一点没动，所以"人没转、走路方向也没变"这条还成立，
 * 松手以后镜头退回人的朝向，这几个地方也就跟着一起退。
 * <p>
 * 只管 {@code LocalPlayer}：别的实体、还有服务端那边的人，都还是原版。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerLookMixin
{
    /** 手里的东西"甩"到哪儿了：原版每 tick 拿 0.5 往里追，追的是人的朝向。 */
    @Shadow public float xBob;
    @Shadow public float yBob;
    @Shadow public float xBobO;
    @Shadow public float yBobO;

    @Inject(method = "getViewXRot(F)F", at = @At("HEAD"), cancellable = true)
    private void glimmerwhim$viewPitch(float partialTick, CallbackInfoReturnable<Float> cir)
    {
        if (FreeLook.overriding())
        {
            cir.setReturnValue(FreeLook.viewPitch((LocalPlayer) (Object) this, partialTick));
        }
    }

    @Inject(method = "getViewYRot(F)F", at = @At("HEAD"), cancellable = true)
    private void glimmerwhim$viewYaw(float partialTick, CallbackInfoReturnable<Float> cir)
    {
        if (FreeLook.overriding())
        {
            cir.setReturnValue(FreeLook.viewYaw((LocalPlayer) (Object) this, partialTick));
        }
    }

    /**
     * 手和持物一点都别跟镜头转 —— 把那个"甩"的差值按住不动，手就稳稳待在右下角。
     * <p>
     * {@code ItemInHandRenderer} 摆手的角度是 {@code (getViewXRot(partialTick) - xBob) * 0.1F}
     * 与 {@code (getViewYRot(partialTick) - yBob) * 0.1F}：原版这个差值是"镜头比人多了多少"，
     * 一直很小；上面那两刀让 {@code getView*} 改成镜头以后，差值能到 180 度，手就歪到 18 度。
     * <p>
     * 原版那两行是每 tick 拿 0.5 往里追人的朝向，这里把追的目标换成镜头朝向、而且一步到位，
     * 差值就恒为 0 —— 手不跟镜头转，跟"没做这件事"一个样（准星和挖的方块还是跟镜头）。
     */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void glimmerwhim$steadyBob(CallbackInfo ci)
    {
        if (!FreeLook.overriding())
        {
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;

        this.xBob = FreeLook.viewPitch(self, 1.0F);
        this.yBob = FreeLook.viewYaw(self, 1.0F);
    }
}
