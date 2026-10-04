package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.client.DevRender;
import com.wdlpiaoyi.glimmerwhim.whims.client.Traces;
import com.wdlpiaoyi.glimmerwhim.whims.dev.DevWhim;
import com.wdlpiaoyi.glimmerwhim.whims.dev.HighlightTestWhim;
import com.wdlpiaoyi.glimmerwhim.whims.dev.TraceTestWhim;

public final class DevRenders
{
    private DevRenders()
    {
    }

    public static void register()
    {
        WhimRenderer.register(DevWhim.INSTANCE, DevRender::draw, DevRender::hit, DevRender::outline, null, null);
        WhimRenderer.register(HighlightTestWhim.INSTANCE, DevRender::draw, DevRender::hit, DevRender::hue, null, null);
        WhimRenderer.register(TraceTestWhim.INSTANCE, DevRender::draw, DevRender::hit, DevRender::hue, Traces::hue,
                Traces::glow);
    }
}
