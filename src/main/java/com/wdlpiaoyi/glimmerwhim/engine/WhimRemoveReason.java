package com.wdlpiaoyi.glimmerwhim.engine;

public enum WhimRemoveReason
{
    EXPIRED,      // 寿命耗尽
    USED,         // 被成功使用
    DROPPED,      // 使用条件不满足被丢弃（无有效目标等）
    OUT_OF_RANGE, // 超出所有玩家可见范围
    OTHERS        // 命令/外部强制清除
}
