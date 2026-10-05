package com.wdlpiaoyi.glimmerwhim.whims.dev.template;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.wdlpiaoyi.glimmerwhim.GlimmerWhim;
import com.wdlpiaoyi.glimmerwhim.anchor.PosAnchor;
import com.wdlpiaoyi.glimmerwhim.engine.Whim;
import com.wdlpiaoyi.glimmerwhim.engine.WhimData;
import com.wdlpiaoyi.glimmerwhim.engine.WhimDomain;
import com.wdlpiaoyi.glimmerwhim.engine.WhimEvent;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParam;
import com.wdlpiaoyi.glimmerwhim.engine.WhimParams;
import com.wdlpiaoyi.glimmerwhim.engine.WhimRemoveReason;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawn;
import com.wdlpiaoyi.glimmerwhim.engine.WhimSpawnContext;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTarget;
import com.wdlpiaoyi.glimmerwhim.engine.WhimTraces;
import com.wdlpiaoyi.glimmerwhim.whims.WhimModifier;
import com.wdlpiaoyi.glimmerwhim.whims.WhimRole;
import com.wdlpiaoyi.glimmerwhim.whims.WhimType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// 灵感扩展点模板：正式内容从这份骨架复制。
// 每个钩子都显式写出，即使只是默认行为；注释说明它管什么、什么时候需要改。
// 登记：WhimContent.register(INSTANCE)（本文件作为活示例已在 DevWhims 登记）；
// 客户端绘制与轨迹在 whims/dev/template/client/WhimTemplateRender 登记。
// 完整的一手经验（可配置默认值、区间随机、两段式状态机、冷却、延迟效果、定向音效）见 docs/template.md。
public final class WhimTemplate implements WhimType
{
    // 灵感 id；/glimmerwhim summon 用它，须全局唯一
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID,
            "whim_template");

    // 本灵感默认的链轨迹样式 id；绘制实现在 WhimTemplateRender 登记
    public static final ResourceLocation TRACE = ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID,
            "template_trace");

    // 单例：类型无状态，注册与查找共用同一个实例
    public static final WhimTemplate INSTANCE = new WhimTemplate();

    // 数值域：id + 中性值 + 折叠规则全部由读取方（本元素）定义；修饰符只交 id 与数字
    private static final WhimDomain SCALE = new WhimDomain(
            ResourceLocation.fromNamespaceAndPath(GlimmerWhim.MODID, "template_scale"), 1.0D, (a, b) -> a * b);

    private static final String SHAPE = "shape";
    private static final String SCALE_KEY = "scale";

    private static final int DEFAULT_LIFETIME = 200;

    private WhimTemplate()
    {
    }

    // 类型 id；与注册表、命令、网络都靠它对齐
    @Override
    public ResourceLocation id()
    {
        return ID;
    }

    // 自定义参数：choice 是枚举、positiveNumber 是正数；键只在这里声明，取值用 params().text/number
    @Override
    public WhimParams params()
    {
        return WhimParams.of(WhimParam.choice(SHAPE, "quad", "quad", "cube"),
                WhimParam.positiveNumber(SCALE_KEY, "1.0"));
    }

    // 类型参数 + 内置 lifetime/visibility/trace；默认实现已够用，只有要加自己的内置参数才重写
    @Override
    public WhimParams effectiveParams()
    {
        return WhimType.super.effectiveParams();
    }

    // 链的 element 轨迹样式；未登记绘制的 id 会回落到 line
    @Override
    public ResourceLocation elementTrace()
    {
        return TRACE;
    }

    // modifier 叠加样式；合成样式（如 hue）绘制为空、只染色
    @Override
    public ResourceLocation modifierTrace()
    {
        return WhimTraces.NONE;
    }

    // 默认寿命（tick）；-1 = 永久。玩家可再用 {lifetime:...} 覆盖
    @Override
    public int defaultLifetime()
    {
        return DEFAULT_LIFETIME;
    }

    // 角色集合：默认由 canRoot 与 modifier(data) 是否有值推导；要自定角色时才重写
    @Override
    public Set<WhimRole> roles(WhimData data)
    {
        return WhimType.super.roles(data);
    }

    // 作为链修饰符时输出的数值；这里交出 SCALE 域的数字，折叠规则由读它的元素定义
    @Override
    public Optional<WhimModifier> modifier(WhimData data)
    {
        return Optional.of(new WhimModifier(SCALE.id(), params().number(data, SCALE_KEY, 1.0D)));
    }

    // 根元素是否接受当前交互目标；返回 false 则本次使用被丢弃，可在 on(USE) 前就挡掉
    @Override
    public boolean acceptsTarget(WhimTarget target)
    {
        return true;
    }

    // 把目标解析成实体（默认认 entity 目标）；自定义目标种类时在这里解析
    @Override
    public Optional<Entity> resolveTarget(ServerPlayer player, WhimTarget target)
    {
        return WhimType.super.resolveTarget(player, target);
    }

    // 灵感中心是否须对玩家可见（无遮挡）才渲染；默认不检查
    @Override
    public boolean requiresLineOfSight()
    {
        return false;
    }

    // 是否参与方块遮挡判定；默认参与（配合 requiresLineOfSight 生效）
    @Override
    public boolean occludedByBlocks(WhimData data)
    {
        return true;
    }

    // 参与实体遮挡判定的实体；null = 实体不遮挡本灵感。返回谓词会开深度测试
    @Override
    public Predicate<Entity> entityOccluders(WhimData data)
    {
        return null;
    }

    // 实体遮挡用攻击箱还是渲染剔除框；默认 false = 攻击箱
    @Override
    public boolean entityOcclusionRenderBox(WhimData data)
    {
        return false;
    }

    // 绘制时是否开深度测试：开启后被方块与实体挡住的部分不画（深度缓冲区分不了两者）；
    // 默认沿用实体遮挡声明。想「被方块和实体都挡住」又不用实体遮挡判定，就显式返回 true
    @Override
    public boolean depthOcclusion(WhimData data)
    {
        return WhimType.super.depthOcclusion(data);
    }

    // 能否被准星瞄准/交互；渲染不受影响
    @Override
    public boolean interactable(WhimData data)
    {
        return true;
    }

    // 能否作为链根；false 则只能被别的元素带进链
    @Override
    public boolean canRoot()
    {
        return true;
    }

    // 能否入链；默认 = interactable
    @Override
    public boolean canChain(WhimData data)
    {
        return WhimType.super.canChain(data);
    }

    // 进链（被按住或作修饰）时是否暂停寿命倒计时；默认暂停
    @Override
    public boolean pausesInChain()
    {
        return true;
    }

    // 真则用完随链一并消耗；要「使用后转入下一状态」（strike 的蓄力体就是如此）就返回假，
    // 由 on(USE) 自己 update 数据 / freeze 寿命 / 排定时任务，最后自行移除
    @Override
    public boolean consumedOnUse(WhimData data)
    {
        return true;
    }

    // 是否每 tick 收到 TICK 事件；默认关闭。要收 TICK 就返回 true
    @Override
    public boolean ticks()
    {
        return false;
    }

    // 事件回调；按事件种类分派，可调用 event.remove() 请求移除
    @Override
    public void on(WhimEvent event)
    {
        switch (event.kind())
        {
            case SUMMON ->
            {
                // 收到 SUMMON 时灵感刚生成，做一次性初始化
            }
            case HIGHLIGHT ->
            {
                // 玩家开始瞄准本灵感（边沿触发），可播放音效/记录状态
            }
            case UNHIGHLIGHT ->
            {
                // 玩家不再瞄准，撤销 HIGHLIGHT 期间的效果
            }
            case TICK ->
            {
                // 每 tick 到来，需 ticks() 返回真；用于持续移动/计时
            }
            case HOLD ->
            {
                // 被按住、成为链根或修饰符；可读 event.chain() 快照
            }
            case USE ->
            {
                // 到这里目标已通过引擎校验；沿用 strike 的两段式写法（见 docs/template.md）
                // 可以先 WhimRegistry.update(...) 改数据与可见性、WhimRegistry.freeze(...) 冻结寿命、
                // WhimScheduler.schedule(...) 排延迟结算，再由那次结算收尾
                WhimTarget target = event.target().orElse(null);
                // 读回被修饰后的数值：链上没有 SCALE 域时取定义的中性值
                double scale = event.chain().map(chain -> chain.value(SCALE)).orElse(1.0D);
                ServerPlayer player = event.player();

                if (player != null)
                {
                    // player 在无召唤者的事件里可能为 null，回显前判空
                    player.sendSystemMessage(Component.literal("whim_template: scale=" + scale + " target="
                            + (target == null ? "无" : target.describe())));
                }

                // 目标有效则消耗自身，否则丢弃
                event.remove(target != null && acceptsTarget(target) ? WhimRemoveReason.USED
                        : WhimRemoveReason.DROPPED);
            }
            case REJECT ->
            {
                // 目标为空/不合法/不被接受，整链即将按浪费丢弃；只发给链根，
                // 适合在这里给玩家反馈（strike 会播火把熄灭音）
            }
            case EXPIRE ->
            {
                // 寿命耗尽、即将移除；做收尾，别在这里依赖灵感仍可交互
            }
            case REMOVE ->
            {
                // 已按 event.reason() 移除；清理类型持有的外部状态
            }
        }
    }

    // 注册时的一次性初始化；登记本类型的工具（如轨迹 id）放这里
    @Override
    public void bind()
    {
        // 注册只在服务端认识的轨迹 id（校验/补全）；绘制实现由客户端另行登记
        WhimTraces.register(TRACE);
    }

    // 无锚 summon 时决定落点与初始数据；返回 empty 表示必须显式给锚
    @Override
    public Optional<WhimSpawn> spawn(WhimSpawnContext context)
    {
        // 随机落在玩家周围；把数值写成区间（WhimData.range），成形时会各抽一个值，每次生成略有不同
        Vec3 at = context.randomAround(8.0D, 24.0D);
        WhimData data = WhimData
                .of(Whim.LIFETIME, WhimData.range((int) (DEFAULT_LIFETIME * 0.8D), (int) (DEFAULT_LIFETIME * 1.2D)))
                .with(SCALE_KEY, WhimData.range(0.85D, 1.15D));
        return Optional.of(new WhimSpawn(new PosAnchor(at), data));
    }

    // 自然生成后的表现：只有生成管线会调用它（指令召唤不走这里）；
    // 适合只给召唤者的生成音效等，定向发包写法见 strike
    @Override
    public void onGenerated(WhimSpawnContext context, WhimSpawn placement, Whim whim)
    {
    }

    // 消失后在客户端演多久的消散（毫秒）；画法登记在客户端（见 WhimTemplateRender.vanish）
    @Override
    public int vanishMillis(WhimData data)
    {
        return 400;
    }
}
