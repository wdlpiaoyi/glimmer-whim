package com.wdlpiaoyi.glimmerwhim.mixin.client;

import java.util.List;

import com.mojang.brigadier.ParseResults;
import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin
{
    // 命令提示里 {…} 参数段用金色标出
    private static final Style GLIMMERWHIM_PARAM = Style.EMPTY.withColor(ChatFormatting.GOLD);

    // 正在格式化的整条命令文本；只有本模组的命令才改色
    private static String glimmerwhim$command = "";

    @Inject(method = "formatText", at = @At("HEAD"))
    private static void glimmerwhim$capture(ParseResults<SharedSuggestionProvider> parse, String text, int cursor,
            CallbackInfoReturnable<FormattedCharSequence> callback)
    {
        glimmerwhim$command = text;
    }

    @Redirect(method = "formatText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/FormattedCharSequence;forward(Ljava/lang/String;Lnet/minecraft/network/chat/Style;)Lnet/minecraft/util/FormattedCharSequence;"))
    // 重定向 formatText 里的 forward，按 '{' 把文本拆成正常段与参数段
    private static FormattedCharSequence glimmerwhim$paramColor(String text, Style style)
    {
        // 无 '{' 或不是本模组的命令时不改色
        int brace = text.indexOf('{');
        String command = glimmerwhim$command.startsWith("/") ? glimmerwhim$command.substring(1) : glimmerwhim$command;

        if (brace < 0 || !command.startsWith(GlimmerWhim.MODID))
        {
            return FormattedCharSequence.forward(text, style);
        }

        return FormattedCharSequence.composite(List.of(
                FormattedCharSequence.forward(text.substring(0, brace), style),
                FormattedCharSequence.forward(text.substring(brace), GLIMMERWHIM_PARAM)));
    }
}
