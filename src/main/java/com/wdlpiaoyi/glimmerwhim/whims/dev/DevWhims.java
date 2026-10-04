package com.wdlpiaoyi.glimmerwhim.whims.dev;

import com.wdlpiaoyi.glimmerwhim.whims.Whims;

public final class DevWhims
{
    private DevWhims()
    {
    }

    public static void register()
    {
        Whims.register(DevWhim.INSTANCE);
        Whims.register(SpawnTestWhim.INSTANCE);
        Whims.register(DevMarkWhim.INSTANCE);
        Whims.register(DevEntityWhim.INSTANCE);
        Whims.register(DevCoordWhim.INSTANCE);
        Whims.register(DevRootWhim.INSTANCE);
        Whims.register(DevBoostWhim.INSTANCE);
        Whims.register(DevPowerWhim.INSTANCE);
        Whims.register(DevRangeWhim.INSTANCE);
        Whims.register(HighlightTestWhim.INSTANCE);
        Whims.register(TraceTestWhim.INSTANCE);
        Whims.register(ExpiresWhim.INSTANCE);
        Whims.register(RemoveOnHoldWhim.INSTANCE);
        Whims.register(RemoveOnReleaseWhim.INSTANCE);
        Whims.register(VoidTestWhim.INSTANCE);
    }
}
