# AGENTS.md

Minecraft Forge 1.20.1 mod, modid `glimmerwhim`, package `com.wdlpiaoyi.glimmerwhim`, version `0.1.0`.
Currently early development: engine, interaction, chaining and rendering exist; official content has just started — `whims/content/` holds `strike` (the only official whim so far), and `whims/dev/` holds the remaining dev/test content (`dev_entity`, `whim_template`) plus `whims/dev/template/` skeletons.

## Build & run

- Build **offline**: `.\gradlew.bat compileJava --offline` (POSIX: `./gradlew ...`). Dependencies are cached locally; prefer `--offline`.
- Java **17** toolchain is required by ForgeGradle. If the Gradle home is missing, point `GRADLE_USER_HOME` at the repo's sandbox `.gradle-home/` and make a local JDK 17 available (the machine has HMCL JDKs; see the gitignored `环境提示.md`).
- `gradle.properties` sets `org.gradle.daemon=false`, so every build is a cold JVM; `runClient` takes minutes.
- Full build: `.\gradlew.bat build --offline` → `build/libs/`. Dev client: `.\gradlew.bat runClient --offline`; working dir is `run/`, log is `run/logs/latest.log`.
- **There is no test suite.** Verification = `compileJava` exiting clean plus a manual `runClient` session.

## Architecture

Server-authoritative core lives in `engine/`:
- `engine/WhimRegistry.java` — per-dimension registries; visibility sync (`sent` set, `SYNC_INTERVAL=20`), aim/hold/use (the hold packet carries the whole chain snapshot; every in-chain whim freezes its lifetime unless the type overrides `pausesInChain()` to false), removal (a chain member is removed only when its `WhimType.consumedOnUse(data)` is true, default true; `WhimRegistry.publish(...)` re-syncs a whim to a new `WhimVisibility`).
- `engine/WhimLifecycle.java` — the **single** spawn entry (`summon(...)`); all `new Whim(...)` goes through it. Natural generation goes through `generate(...)`: it asks the type for `spawn(...)`, rejects a spawn point the viewer cannot see, summons, then lets the type present itself via `onGenerated(...)` — command summon skips that hook on purpose.
- `engine/Whim.java` / `WhimData.java` / `WhimParams.java` / `WhimEvent.java` — data model, `{name:value}` parsing, typed params, event kinds (`SUMMON`/`HIGHLIGHT`/`UNHIGHLIGHT`/`TICK`/`HOLD`/`USE`/`REJECT`/`EXPIRE`/`REMOVE`; `TICK` only when `WhimType.ticks()`; `REJECT` fires on the chain root when a use is dropped for a bad target).
- `engine/WhimScheduler.java` / `WhimTask.java` — per-dimension delayed/repeating tasks; a task is cancelled when its owning whim is removed. Whim expiry is one such task, so the main loop no longer scans lifetimes per tick.
- `engine/WhimSight.java` — occlusion (center ray; blocks via `canOcclude`; entities via bounding box, hit-box or render-culling box). `WhimType.depthOcclusion(data)` is the separate render-side switch: a type that returns true is drawn with the depth test on, so blocks and entities can hide it for real (partial occlusion is possible; the depth buffer cannot tell blocks from entities).
- `engine/WhimPlayerState.java` — per-player transient marks (key → expiry tick, counted from the server-wide `MinecraftServer.getTickCount()` so it is dimension-independent); not persisted, dropped on logout. Logic-side timings are ticks; render-side timings are milliseconds.
- `engine/WhimTarget.java` / `engine/WhimTargets.java` — an interaction target is a `(kind, WhimData payload)` pair; `WhimTargets` is the kind registry (id + hint + server-side `valid` predicate + the payload's position/describe semantics, so a kind without `x`/`y`/`z` skips the reach check and relies on its own predicate; built-ins `point`/`entity`/`whim`, payload carries `x`/`y`/`z` plus the entity/whim UUID). Targets are produced client-side by `client/WhimTargeters` (priority-ordered producers; built-ins live in `client/WhimTargeting`). A whim becomes the target when the crosshair enters it after the hold started (the chain root itself only counts after leaving and coming back to it).
- `engine/WhimChain.java` / `engine/WhimDomain.java` — chain aggregation: a modifier's `WhimType.modifier(WhimData)` hands out `WhimModifier(id, value)` (domain id + number) and defines no rule; the `WhimDomain` (id + identity + fold rule) is owned by the **reader** — an element holds its own definition and folds that id's numbers via `chain.value(domain)` (identity when unused).

Other packages:
- `anchor/` — placement only; `anchor/WhimAnchors.ANCHORS` is the anchor registry (type + `read`/`parse`/`suggestData` + hint).
- `whims/` — `WhimType` implementations; `whims/WhimContent.register(...)` registers each type and hooks it onto the Forge bus (instance + static `@SubscribeEvent`); `whims/client/` holds draw code (`Glow` = additive-blend light primitives, `ScatterVanish` = the named dissolve style) and the client-side registration entry `WhimRenders` (`Traces` is the named trace registry: draw styles `line`/`glow`/`curve`/`none` via `register(id, impl)` and composite styles `hue` via `Traces.tint(id, impl)` — composite styles draw nothing and only dye whatever is drawn after them); trace styles are chosen per chain via `{element_trace:...}` / `{modifier_trace:...}`; `whims/dev/` holds dev/test content, and `whims/dev/template/` holds skeleton templates for each extension point (reference only; the template whim is itself registered as dev content). Official content lives in `whims/content/` (server registration `ContentWhims`, client-side `ContentRenders`).
- `client/` render/aim/interact/freelook, `net/` packets, `command/` the `/glimmerwhim` tree, `config/` config, `mixin/` client mixins.

## Registration & test content

- Registration is centralized by design: a new whim = implement `whims/WhimType`, `WhimContent.register(...)` (the engine hooks `@SubscribeEvent` handlers automatically), and register client rendering in a **client-side** class via `whims/client/WhimRenders`. `bind()` is an escape hatch for third-party buses. Official content registers unconditionally from `whims/content/ContentWhims` (server) and `whims/content/client/ContentRenders` (client). A new anchor = one row in `anchor/WhimAnchors.ANCHORS`. A new trace style = one id in `engine/WhimTraces` (`register` works at runtime too) plus one client `WhimRenders.register(traceId, impl)` (draw) or `WhimRenders.tint(traceId, impl)` (composite, e.g. `hue` — composite styles receive a stack count so each defines its own stacking rule; `hue` shortens its cycle per extra layer); any whim then selects it via `{element_trace:...}` / `{modifier_trace:...}`. A new numeric domain = a `WhimDomain` built where it is read (by the element) plus a plain `WhimModifier(id, value)` from each modifier; nothing else to register. A new target kind = one row in `engine/WhimTargets` (id + hint + server-side `valid` predicate + payload semantics) plus one client `WhimTargeters.register(priority, producer)`. A new vanish style = a named client class (e.g. `whims/client/ScatterVanish`) attached per type via `RenderSpec.vanish(...)`; `WhimType.vanishMillis(data)` (milliseconds, 0 = no dissolve) declares how long it plays after the whim disappears.
- Dev content activates by presence: `whims/dev/` self-registers via `@Mod.EventBusSubscriber`; there is no runtime toggle. Moving files out of `whims/dev/` disables them.
- `parked/` mirrors the source package but is **not compiled**. Restoring a parked file requires moving it back into `whims/dev/` and re-registering it in `DevWhims`/`DevRenders`.

## Conventions

- Comments, docs and in-game strings are **Simplified Chinese**. Commit messages are a single concise Chinese line (no `feat:`/type prefix).
- Code style: Allman braces (opening brace on its own line), 4-space indent, one top-level class per file.
- Comments should be sparse and point out non-obvious mechanics/invariants/magic numbers only — not narration.
- 时间单位：逻辑相关的（游戏内时间）一律用 **tick**，渲染相关的用 **ms**。
- 保持通用性、扩展性和可配置性：新行为优先做成登记表/数据/配置项，不要把数值与分支写死在调用点。
- Never commit `环境提示.md`, `设计纲要.md`, `.toolchain/`, `.gradle-home/` (gitignored), nor any absolute local paths.

## Gotchas

- Mixins: after editing `mixin/`, confirm the refmap regenerated and targets mapped (e.g. `CommandSuggestions.formatText` → `m_93892_`, `FormattedCharSequence.forward` → `m_13714_`) in `build/tmp/compileJava/compileJava-refmap.json`. `CommandSuggestionsMixin` only recolors `{…}` for commands starting with the modid.
- Client render registration must live in a `Dist.CLIENT`-only class; render classes cannot load on the server.
- A whim without a registered drawer renders with the `[render.default]` look (`WhimRenders` logs such types at load); `render.default` is the fallback for all content, not a dev-only section.
- `/glimmerwhim summon <type> [anchor] [anchorData] {data}`: the anchor is not implicit — omit it (or write `default`) only when the whim declares its own `spawn()`; otherwise the command errors. Anchor data is positional and outside `{}`; `{...}` holds `name:value` pairs separated by `,` or `;`. A value written as `min..max` is rolled to a random value inside the range when the whim is created (`WhimData.roll(RandomSource)` in `WhimLifecycle.summon`).
- `lifetime` is in **ticks** (`-1` = permanent); client trace durations are in **milliseconds**.
