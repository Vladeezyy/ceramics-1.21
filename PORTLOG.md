# Ceramics 1.8.0 → NeoForge 1.21.1: port log

Original: [KnightMiner/Ceramics](https://github.com/KnightMiner/Ceramics), branch `1.20` (1.8.0, Forge 1.20.1, MIT,
commit `407a7c7`). The first commit of this repository is that code unchanged, so `git diff` against it shows the port.

## Method

- Java kept file by file with the original packages and class names. Code that only moved between APIs is changed in
  place; behavior changes are commented in the code.
- Library: Mantle `1.21.1-1.12.10` from the SlimeKnights maven. Mantle 1.21.1 has **no public release** (no CurseForge,
  Modrinth or GitHub release, only dev builds), so it ships inside the jar (Jar-in-Jar, MIT text in
  `licenses/LICENSE-MANTLE`). NeoForge keeps the newest Mantle if another mod brings one.
- Resources: assets of the original kept, datagen of the original ported and run (`./gradlew runData`): 282 recipes,
  282 advancements, 103 loot tables, the same counts as the 1.20 output.
- Tests are in a separate source set `src/selftest`, loaded only in dev runs (`runSelftest`, `runScene`), never in the jar.

## API changes (1.20.1 Forge → 1.21.1 NeoForge)

| 1.20.1 | 1.21.1 |
|---|---|
| bucket fluid/block in item NBT (`fluid`, `block`), cracks in NBT (`cracks`) | data components `ceramics:fluid`, `ceramics:block`, `ceramics:cracks` |
| `getCapability` + `LazyOptional` chains on block entities | `RegisterCapabilitiesEvent`, `BlockCapabilityCache` for neighbors |
| `initCapabilities` on items | `registerItem(Capabilities.FluidHandler.ITEM, ...)` |
| Mantle `NetworkWrapper` packets | `CustomPacketPayload` records with `StreamCodec` |
| `ArmorMaterials` enum | `ArmorMaterial` registry entry, durability on the item |
| `CopyNbtFunction` in loot | `copy_components` from the block entity (`collectImplicitComponents`) |
| Forge ingredient serializer `ceramics:no_nbt` | `ICustomIngredient` with the same id |
| `Block.use` | `useItemOn` / `useWithoutItem` |
| `forge:` tags, `forge:item/default` | `c:` / `neoforge:` tags, `neoforge:item/default` |
| `MissingMappingsEvent` (1.16 `gauge`) | registry alias `ceramics:gauge` → `ceramics:terracotta_gauge` |
| block entity tag `mantle:hides_gauge_amount` | block tag of the same name (Mantle 1.21) |

**Worlds from 1.20.** The world upgrade keeps unknown item NBT as `minecraft:custom_data`. Buckets and cracked blocks
read the old `fluid` / `block` / `cracks` keys from there and convert them to components on the next inventory tick
(checked by the self-test).

**Cisterns.** In 1.20 an extension exposed a lazy optional pointing at the base, invalidated through listeners. Now every
cistern exposes one handler object that forwards to the tank of the current base (found by walking down, cached and
dropped whenever a cistern of the column changes state), so neighbors never hold a stale handler.

## Bugs fixed (of the original or found during the port)

| Bug | Where it came from |
|---|---|
| Kiln recipes never show in JEI | 1.8.0 marked kiln recipes `isSpecial()` (for the recipe book) and the JEI plugin skips special recipes |
| Milk clay buckets have no fluid handler, can be filled but not drained | original issue [#88](https://github.com/KnightMiner/Ceramics/pull/88) |
| Creative tab lists the empty bucket twice, no solid (powder snow) buckets, no unfired bucket | `Registration.addTabItems` |
| Cracked empty bucket shows a raw translation key | item `cracked_empty_clay_bucket`, lang key `empty_cracked_clay_bucket` |
| Kiln screen: the hidden recipe book button can still be clicked | removed from renderables only |
| `/setblock` of a cistern gives a broken extension (log spam), of a channel a "powered" channel that stops flowing down | no default block state |
| Bar color of cracked block items divides by zero | items without durability use the default color |
| Channel side handler cleanup removed the wrong side's neighbor cache | `refreshNeighbor` removed `DOWN` instead of the side (moot with capability caches) |
| Fluids in cisterns, faucets and channels render a piece of the block atlas | Mantle 1.21.1-1.12.10 `FluidRenderer` passes 0–16 texture coordinates, 1.21 expects 0–1; own copy in `FluidCuboidRenderer` |
| Golden bricks drop when mined with a wooden pickaxe | `neoforge:needs_gold_tool` is not wired into the vanilla tiers in 1.21; added to `minecraft:incorrect_for_wooden_tool` |
| Typo in Russian (`бочкаn`) | original `ru_ru.json` |

Open issues of the original checked: #82 (Ex Nihilo barrels, 1.16 mod not on 1.21), #68 (More Cauldrons, 1.16 mod),
#58 / #26 (feature ideas), #86 (Supplementaries tags, not added), #84 / #87 (translations, PRs not merged upstream).

## Languages

en_us, ru_ru (completed, 20 keys were missing), uk_ua, de_de, es_es, pt_br (new, `tools/lang/gen_lang.py`, terms from
the vanilla 1.21.1 translations), ko_kr, tr_tr, zh_cn, zh_tw kept from the original. Checked with
`modding-kb/scripts/langcheck.py`.

## Verification

- `tools/selftest.sh`: dev server, fresh world, 44 checks — registries and the 1.16 alias, recipe counts, kiln and clay
  repair recipes, no_nbt ingredient, clay buckets with the fluid handler, milk handler, cauldron interactions, burn time,
  1.20 data conversion, armor values, tool tags, creative tab, cistern tower (fill, break the middle, merge back),
  faucet pouring, channel flowing down, cracking with lava, drops with cracks, save/load.
- `tools/scenetest.sh`: dev client, scene with screenshots (`run/screenshots/scene_*.png`) and checks of item models,
  names, faucet/channel sync and JEI kiln recipes.

## 1.8.0.1: Kilnwork instead of Mantle

Mantle has no builds for Minecraft after 1.21.1, so the parts Ceramics uses were rewritten as a separate library,
[Kilnwork](https://github.com/Vladeezyy/kilnwork) (LGPL-3.0-only, based on Mantle, MIT), shipped inside the jar.
- Registration: `BlockRegister`, `EnumObject`, `BuildingBlocks` instead of Mantle's deferred registers and objects.
- `SyncedBlockEntity`, `GaugeBlock`, gauge tooltip, `FluidTransferHelper` with data driven transfers, tags
  `kilnwork:gauges/*` and `kilnwork:hides_gauge_amount` (were `mantle:`).
- Fluid shapes: same JSON format, folders `kilnwork/block_fluids`, `kilnwork/faucet_fluid`, `kilnwork/channel_fluids`.
  They were written by Mantle datagen providers; now they are plain JSON in `src/main/resources` (no datagen needed).
- Fluid rendering on the vanilla translucent entity render type instead of Mantle's shader.
- Behavior unchanged: self-test 44/44, client scene 7/7.
