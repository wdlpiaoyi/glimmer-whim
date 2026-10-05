package com.wdlpiaoyi.glimmerwhim.engine;

import com.wdlpiaoyi.glimmerwhim.anchor.WhimAnchor;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import java.util.UUID;

public final class Whim
{
    // 参数键：lifetime 存续 tick（-1 永久）；visibility 可见性；playtime 客户端播放 tick；*_trace 轨迹样式 id
    public static final String LIFETIME = "lifetime";
    public static final String VISIBILITY = "visibility";
    public static final String PLAYTIME = "playtime";
    public static final String ELEMENT_TRACE = "element_trace";
    public static final String MODIFIER_TRACE = "modifier_trace";
    // 本体的外观与命中体积，取值来自 WhimShapes / WhimHits
    public static final String SHAPE = "shape";
    public static final String HIT = "hit";
    // 命中体积相对本体大小的倍率；只影响判定，不影响外观
    public static final String HIT_SCALE = "hit_scale";

    private final UUID id;
    private final WhimAnchor anchor;
    private final WhimType type;
    private WhimData data;
    private WhimVisibility visibility;
    private long deadline; // 到期游戏刻；负值表示永久
    private long frozenRemaining = -1L; // 非负表示被按住暂停，保存剩余 tick

    public Whim(UUID id, WhimAnchor anchor, WhimType type, WhimData data, WhimVisibility visibility, long now)
    {
        this.id = id;
        this.anchor = anchor;
        this.type = type;
        this.data = data;
        this.visibility = visibility;

        int ticks = lifetime(data);
        this.deadline = ticks < 0 ? -1L : now + ticks;
    }

    // 解析 lifetime 参数；缺失或非法一律按永久处理
    private static int lifetime(WhimData data)
    {
        try
        {
            return Integer.parseInt(data.get(LIFETIME).orElse("-1"));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    public UUID id()
    {
        return this.id;
    }

    public WhimAnchor anchor()
    {
        return this.anchor;
    }

    public WhimType type()
    {
        return this.type;
    }

    public WhimData data()
    {
        return this.data;
    }

    public WhimVisibility visibility()
    {
        return this.visibility;
    }

    // 运行期改数据；改后由登记表广播给已收到的玩家
    public void setData(WhimData data)
    {
        this.data = data;
    }

    // 运行期改可见性；改后由登记表重新对账
    public void setVisibility(WhimVisibility visibility)
    {
        this.visibility = visibility;
    }

    // 剩余 tick；永久返回 -1，暂停态返回冻结时的剩余
    public int lifetime(long now)
    {
        if (this.deadline < 0)
        {
            return -1;
        }

        if (this.frozenRemaining >= 0)
        {
            return (int) this.frozenRemaining;
        }

        return (int) Math.max(0L, this.deadline - now);
    }

    public boolean permanent()
    {
        return this.deadline < 0;
    }

    // 按住暂停：记下当前剩余并停表
    public void freeze(long now)
    {
        if (this.deadline >= 0 && this.frozenRemaining < 0)
        {
            this.frozenRemaining = Math.max(0L, this.deadline - now);
        }
    }

    // 松开恢复：以冻结的剩余重新起算
    public void resume(long now)
    {
        if (this.frozenRemaining >= 0)
        {
            this.deadline = now + this.frozenRemaining;
            this.frozenRemaining = -1L;
        }
    }
}
