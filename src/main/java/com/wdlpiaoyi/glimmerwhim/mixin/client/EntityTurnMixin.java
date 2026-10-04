package com.wdlpiaoyi.glimmerwhim.mixin.client;

import com.wdlpiaoyi.glimmerwhim.client.FreeLook;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin
{
    // HEAD 拦截；自由视角激活时消费输入并取消原版转向
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
