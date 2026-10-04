package com.wdlpiaoyi.glimmerwhim.whims;

public final class WhimContent
{
    private WhimContent()
    {
    }

    // 批量注册并调用 bind()；须在使用前调用
    public static void register(WhimType... types)
    {
        for (WhimType type : types)
        {
            Whims.register(type);
            type.bind();
        }
    }
}
