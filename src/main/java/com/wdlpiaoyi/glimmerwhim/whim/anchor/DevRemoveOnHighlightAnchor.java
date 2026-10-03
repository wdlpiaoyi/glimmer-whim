package com.wdlpiaoyi.glimmerwhim.whim.anchor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.whim.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whim.WhimData;
import com.wdlpiaoyi.glimmerwhim.whim.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.whim.WhimParams;
import com.wdlpiaoyi.glimmerwhim.whim.WhimRemoveReason;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 就在 0 0 0 待着，谁瞄上它它就没了 —— 试"高亮即消失"用的。
 * <p>
 * 什么都不要：锚数据写不下（它没有位置可写），{@code {参数}} 一个都不认，
 * 画出来什么样由渲染那边定（参数表空着就按一个立方体、边长 1 画）。
 */
public final class DevRemoveOnHighlightAnchor implements WhimAnchor
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("glimmerwhim",
            "dev_removeonhighlight");

    private static final String WRONG = "这种锚不收锚数据，它就在 0 0 0";

    /** 什么都不收。 */
    public static DevRemoveOnHighlightAnchor parse(CommandSourceStack source, String data)
    {
        if (data != null && !data.isBlank())
        {
            throw new IllegalArgumentException(WRONG);
        }

        return new DevRemoveOnHighlightAnchor();
    }

    /** 没得可补。 */
    public static Collection<String> suggestData(CommandSourceStack source)
    {
        return List.of();
    }

    /** 身上什么都没带，读的时候也不用读。 */
    public static DevRemoveOnHighlightAnchor read(FriendlyByteBuf buf)
    {
        return new DevRemoveOnHighlightAnchor();
    }

    @Override
    public ResourceLocation type()
    {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        // 没有字段，什么都不用写。
    }

    @Override
    public Optional<Vec3> position(Level level, Vec3 eye, float partialTick, WhimData data, WhimParams params)
    {
        return Optional.of(Vec3.ZERO);
    }

    /** 被高亮就是要它走 —— 条件就这一句，写在自己身上。 */
    @Override
    public void on(WhimEvent event)
    {
        if (event.kind() == WhimEvent.Kind.HIGHLIGHT)
        {
            event.remove(WhimRemoveReason.USED);
        }
    }
}
