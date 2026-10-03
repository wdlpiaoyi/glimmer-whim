package com.wdlpiaoyi.glimmerwhim.whim;

/** 一条灵感为什么没了。 */
public enum WhimRemoveReason
{
    /** 到点了，自己没的。 */
    EXPIRED,
    /** 被用掉了。 */
    USED,
    /** 组合没命中，这次灵感浪费掉。 */
    MISMATCH,
    /** 拖到一半松手 —— 灵感是会溜走的。 */
    DROPPED,
    /** 所在的维度卸载了。 */
    LEVEL_UNLOAD,
    /** 保底，别的都归这儿。 */
    OTHERS
}
