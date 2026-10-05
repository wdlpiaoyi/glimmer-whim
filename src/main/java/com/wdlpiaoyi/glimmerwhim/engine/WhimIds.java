package com.wdlpiaoyi.glimmerwhim.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;

import net.minecraft.resources.ResourceLocation;

// 具名 id 清单：这里只登记名字（校验与补全用），实现由各自的注册处提供（多在客户端）
public final class WhimIds
{
    private final List<ResourceLocation> ids = new ArrayList<>();

    public void register(ResourceLocation id)
    {
        if (!this.ids.contains(id))
        {
            this.ids.add(id);
        }
    }

    public List<ResourceLocation> ids()
    {
        return Collections.unmodifiableList(this.ids);
    }

    public boolean contains(ResourceLocation id)
    {
        return this.ids.contains(id);
    }

    public List<String> names()
    {
        return this.ids.stream().map(ResourceLocation::toString).toList();
    }

    // 解析玩家写的 id：允许省略命名空间（只写路径时按本模组命名空间补全）；未登记返回空
    public Optional<ResourceLocation> resolve(String raw)
    {
        ResourceLocation id = raw == null ? null : ResourceLocation.tryParse(raw);

        if (id == null)
        {
            return Optional.empty();
        }

        if (contains(id))
        {
            return Optional.of(id);
        }

        ResourceLocation own = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, id.getPath());

        return contains(own) ? Optional.of(own) : Optional.empty();
    }
}
