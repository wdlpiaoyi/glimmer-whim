package com.wdlpiaoyi.glimmerwhim.mixin.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin
{
    private static final Style GLIMMERWHIM_PARAM = Style.EMPTY.withColor(ChatFormatting.GOLD);

    @Redirect(method = "formatText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/FormattedCharSequence;forward(Ljava/lang/String;Lnet/minecraft/network/chat/Style;)Lnet/minecraft/util/FormattedCharSequence;"))
    private static FormattedCharSequence glimmerwhim$paramColor(String text, Style style)
    {
        int brace = text.indexOf('{');

        if (brace <= 0)
        {
            return FormattedCharSequence.forward(text, style);
        }

        return FormattedCharSequence.composite(List.of(
                FormattedCharSequence.forward(text.substring(0, brace), style),
                FormattedCharSequence.forward(text.substring(brace), GLIMMERWHIM_PARAM)));
    }
}
