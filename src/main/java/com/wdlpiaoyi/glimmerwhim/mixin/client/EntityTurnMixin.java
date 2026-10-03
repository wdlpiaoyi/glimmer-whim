package com.wdlpiaoyi.glimmerwhim.mixin.client;

import com.wdlpiaoyi.glimmerwhim.client.FreeLook;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 自由视角开着的时候，鼠标转的是镜头，不是人。
 * <p>
 * 挑 {@code Entity.turn} 下手是因为它是最后一站：鼠标那一串（灵敏度、平滑镜头）到这儿已经算完，
 * 只剩原版自己的 {@code * 0.15F} —— 那份手感由 {@link FreeLook#turn} 照抄，掐在这儿人就不会跟着转。
 * <p>
 * 只掐本地玩家这一下：{@code Entity.turn} 是 {@code Entity} 上的，单人档里服务端那些 mob 也走这条路，
 * 全都掐了它们就不转头了。别的实体该转还转。
 * <p>
 * 人不动了，"你正看着哪儿"这件事只管镜头（画面）—— 准星判定、挖掘、放置、攻击、拉弓、还有
 * 我们自己的瞄准，全都照人的朝向算，一点没碰，这样自由视角就只是"换个方向看"。
 */
@Mixin(Entity.class)
public abstract class EntityTurnMixin
{
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void glimmerwhim$freeLook(double yRot, double xRot, CallbackInfo ci)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.player == null || (Object) this != minecraft.player)
        {
            return;
        }

        if (FreeLook.turn(minecraft.player, yRot, xRot))
        {
            ci.cancel();
        }
    }
}
