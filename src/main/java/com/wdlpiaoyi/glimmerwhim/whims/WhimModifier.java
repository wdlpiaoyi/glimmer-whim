package com.wdlpiaoyi.glimmerwhim.whims;

import com.wdlpiaoyi.glimmerwhim.engine.WhimDomain;

// 链修饰符输出：domain 是该修饰符声明的数值域（含定义），value 为数值
public record WhimModifier(WhimDomain domain, double value)
{
}
