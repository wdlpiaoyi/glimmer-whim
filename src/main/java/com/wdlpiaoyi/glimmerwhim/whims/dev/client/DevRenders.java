package com.wdlpiaoyi.glimmerwhim.whims.dev.client;

import com.wdlpiaoyi.glimmerwhim.client.WhimRenderer;
import com.wdlpiaoyi.glimmerwhim.whims.client.DefaultRender;
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
        WhimRenderer.register(DevWhim.INSTANCE, DefaultRender::draw, DefaultRender::hit, DefaultRender::outline, null, null);
        WhimRenderer.register(HighlightTestWhim.INSTANCE, DefaultRender::draw, DefaultRender::hit, DefaultRender::hue, null, null);
        WhimRenderer.register(TraceTestWhim.INSTANCE, DefaultRender::draw, DefaultRender::hit, DefaultRender::hue, Traces::hue,
                Traces::glow);
    }
}
