<div align="center">

# Ceramics — NeoForge and Fabric 1.21.1

**Clay buckets, cisterns, faucets, channels, kilns and porcelain — now on Minecraft 1.21.1, for NeoForge and Fabric.**

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-3c8527)](https://www.minecraft.net)
[![NeoForge 21.1.197+](https://img.shields.io/badge/NeoForge-21.1.197%2B-e0682b)](https://neoforged.net)
[![Fabric Loader 0.16.14+](https://img.shields.io/badge/Fabric-0.16.14%2B-dbd0b4)](https://fabricmc.net)
[![Kilnwork bundled](https://img.shields.io/badge/Kilnwork-bundled-8a5cf6)](https://github.com/Vladeezyy/kilnwork)
[![License LGPL-3.0](https://img.shields.io/badge/license-LGPL--3.0-blue)](LICENSE)

An unofficial port of **[Ceramics](https://www.curseforge.com/minecraft/mc-mods/ceramics)** 1.8.0 by **KnightMiner**
from Minecraft 1.20.1 (Forge) to **NeoForge and Fabric 1.21.1**, with all of its content.

<img src="docs/images/faucet.jpg" width="49%" alt="A porcelain faucet pouring lava into a porcelain cistern">
<img src="docs/images/cracks.jpg" width="49%" alt="Terracotta cisterns at every crack stage">
<img src="docs/images/channels.jpg" width="49%" alt="Terracotta and porcelain channels carrying water">
<img src="docs/images/buckets.jpg" width="49%" alt="Clay buckets with water, lava, milk and powder snow">
<img src="docs/images/jei_kiln.jpg" width="49%" alt="Kiln recipes in JEI">
<img src="docs/images/creative_tab.jpg" width="49%" alt="The Ceramics creative tab">

</div>

<details>
<summary><b>🇬🇧 English — click to open</b></summary>

## Download

| Version | Minecraft | NeoForge | Fabric | Java |
| --- | --- | --- | --- | --- |
| 1.8.0.2+26.3 | 26.3 | 26.3.0.83 or newer | Loader 0.19.5 or newer + Fabric API 0.163.0+26.3 or newer | 25 |
| 1.8.0.2+26.2 | 26.2 | 26.2.0.93 or newer | Loader 0.19.5 or newer + Fabric API 0.161.0+26.2 or newer | 25 |
| 1.8.0.2+26.1.2 | 26.1.2 | 26.1.2.121 or newer | Loader 0.19.5 or newer + Fabric API 0.155.3+26.1.2 or newer | 25 |
| 1.8.0.2+1.21.11 | 1.21.11 | 21.11.42 or newer | Loader 0.19.5 or newer + Fabric API 0.141.6+1.21.11 or newer | 21 |
| 1.8.0.2+1.21.1 | 1.21.1 | 21.1.197 or newer | Loader 0.16.14 or newer + Fabric API 0.116.17+1.21.1 or newer | 21 |
| 1.8.0.1+26.3 | 26.3 | 26.3.0.83 or newer | — | 25 |
| 1.8.0.1+26.2 | 26.2 | 26.2.0.93 or newer | — | 25 |
| 1.8.0.1+26.1.2 | 26.1.2 | 26.1.2.121 or newer | — | 25 |
| 1.8.0.1+1.21.11 | 1.21.11 | 21.11.42 or newer | — | 21 |
| 1.8.0.1+1.21.1 | 1.21.1 | 21.1.197 or newer | — | 21 |
| 1.8.0+1.21.1 | 1.21.1 | 21.1.197 or newer (Mantle inside) | — | 21 |

One jar per loader: `Ceramics-neoforge-…` or `Ceramics-fabric-…`. Releases: [GitHub releases](../../releases).

## Installation

- **NeoForge:** put `Ceramics-neoforge-1.8.0.2+1.21.1.jar` into `mods/` of a NeoForge 21.1.197 (or newer) instance.
- **Fabric:** put `Ceramics-fabric-1.8.0.2+1.21.1.jar` and [Fabric API](https://modrinth.com/mod/fabric-api)
  0.116.17+1.21.1 (or newer) into `mods/` of a Fabric Loader 0.16.14 (or newer) instance.
- **Server:** the same jar on the server **and** on every client.
- **Nothing else to install.** The original Ceramics needs the library Mantle by SlimeKnights, which has no release
  for new Minecraft versions. This port uses [Kilnwork](https://github.com/Vladeezyy/kilnwork) instead, a library
  built from the parts of Mantle that Ceramics needs, and it ships inside the jar on both loaders.
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) is optional on both loaders: it shows the kiln recipes.
- On Fabric, fluid blocks of Ceramics (cisterns, faucets, channels, clay buckets) work with the Fabric transfer API, so
  pipes and tanks of other Fabric mods connect to them.

## Features

- **Clay buckets:** an early-game bucket made of clay. Water, milk, powder snow and every modded fluid; hot fluids such
  as lava crack the bucket and it breaks when emptied.
- **Fluid handling:** cisterns (stack them to grow the capacity), faucets that pour from a tank into the block below,
  channels that carry fluid sideways and down, gauges that show the fluid level.
- **Terracotta and porcelain:** terracotta cisterns, faucets and channels slowly crack while they hold hot fluid
  (repair them with clay), porcelain ones are heat resistant.
- **Kiln:** a furnace for clay, bricks, terracotta and porcelain that fires twice as fast as a furnace.
- **Decoration:** porcelain in 16 colors, rainbow porcelain, dark, lava, dragon, porcelain, monochrome, golden, marine
  and rainbow bricks with stairs, slabs and walls.
- **Clay armor:** a cheap early armor made of fired clay plates.
- **Languages:** English, Russian, Ukrainian, German, Spanish, Brazilian Portuguese, plus Czech, Korean, Turkish and
  Chinese (simplified and traditional) from the original. Ukrainian, German, Spanish and Portuguese are machine
  translations, corrections are welcome.

## What this port fixes

| Problem | In the original |
| --- | --- |
| Kiln recipes did not show in JEI | 1.8.0 |
| Milk clay buckets could be filled by pipes and tanks but not drained | [#88](https://github.com/KnightMiner/Ceramics/pull/88) |
| The creative tab listed the empty bucket twice and no powder snow or unfired buckets | 1.8.0 |
| The cracked empty bucket showed a raw name (`item.ceramics.cracked_empty_clay_bucket`) | 1.8.0 |
| The hidden recipe book button of the kiln could still be clicked | 1.8.0 |
| `/setblock` made a broken cistern (error spam) or a channel that would not flow down | 1.8.0 |
| Wrong durability bar color on cracked cisterns, faucets and channels | 1.8.0 |
| A Russian typo; Czech translation and Korean updates from pull requests that were never merged | [#87](https://github.com/KnightMiner/Ceramics/pull/87), [#84](https://github.com/KnightMiner/Ceramics/pull/84) |

Found during the port and fixed: fluids in cisterns, faucets and channels drew a piece of the texture atlas
(a bug of Mantle 1.21.1; Kilnwork renders them correctly), and golden bricks could be mined with a wooden pickaxe.

**Worlds from 1.20.1:** clay buckets keep their fluid or block and cracked blocks keep their cracks when the world is
opened in 1.21.1.

Details for each problem: [PORTLOG.md](PORTLOG.md).

## Testing

The same tests on NeoForge and on Fabric:

- Functional test on a dev server, 44 checks: recipes, buckets, cauldrons, a cistern tower that is broken in the
  middle and merged back, faucets, channels, cracking with lava, drops, saving, data from 1.20.
- Client scene with screenshots (above): fluid rendering, crack stages, bucket models, creative tab, kiln recipes in
  JEI.
- The release jars started on real servers with a clean log: NeoForge 21.1.197 and 21.1.259; Fabric Loader 0.16.14 with Fabric API 0.116.17.

## Reporting problems

Problems of this port go to the [issues of this repository](../../issues), not to KnightMiner.

## Credits and license

- **Ceramics** by [KnightMiner](https://github.com/KnightMiner/Ceramics), MIT
  ([licenses/LICENSE-MIT](licenses/LICENSE-MIT)).
- **Kilnwork** ([Vladeezyy/kilnwork](https://github.com/Vladeezyy/kilnwork), LGPL-3.0-only), included in the jar,
  based on parts of **Mantle** by [SlimeKnights](https://github.com/SlimeKnights/Mantle) (MIT).
- Czech translation: RomanPlayer22. Korean: mindy15963 and the original translators.
- Port to NeoForge and Fabric 1.21.1, 1.21.11 and 26.x: Vladeezyy.

This port is released under **LGPL-3.0-only** ([LICENSE](LICENSE), with the GPL text in
[licenses/COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy. Forks are welcome: they stay open, under the
same license, with credit to the authors. Please download the mod only from the official pages of this project.

</details>

<details>
<summary><b>🇷🇺 Русский — нажмите, чтобы открыть</b></summary>

## Скачать

| Версия | Minecraft | NeoForge | Fabric | Java |
| --- | --- | --- | --- | --- |
| 1.8.0.2+26.3 | 26.3 | 26.3.0.83 или новее | Loader 0.19.5 или новее + Fabric API 0.163.0+26.3 или новее | 25 |
| 1.8.0.2+26.2 | 26.2 | 26.2.0.93 или новее | Loader 0.19.5 или новее + Fabric API 0.161.0+26.2 или новее | 25 |
| 1.8.0.2+26.1.2 | 26.1.2 | 26.1.2.121 или новее | Loader 0.19.5 или новее + Fabric API 0.155.3+26.1.2 или новее | 25 |
| 1.8.0.2+1.21.11 | 1.21.11 | 21.11.42 или новее | Loader 0.19.5 или новее + Fabric API 0.141.6+1.21.11 или новее | 21 |
| 1.8.0.2+1.21.1 | 1.21.1 | 21.1.197 или новее | Loader 0.16.14 или новее + Fabric API 0.116.17+1.21.1 или новее | 21 |
| 1.8.0.1+26.3 | 26.3 | 26.3.0.83 или новее | — | 25 |
| 1.8.0.1+26.2 | 26.2 | 26.2.0.93 или новее | — | 25 |
| 1.8.0.1+26.1.2 | 26.1.2 | 26.1.2.121 или новее | — | 25 |
| 1.8.0.1+1.21.11 | 1.21.11 | 21.11.42 или новее | — | 21 |
| 1.8.0.1+1.21.1 | 1.21.1 | 21.1.197 или новее | — | 21 |
| 1.8.0+1.21.1 | 1.21.1 | 21.1.197 или новее (Mantle внутри) | — | 21 |

Свой jar на каждый загрузчик: `Ceramics-neoforge-…` или `Ceramics-fabric-…`. Релизы: [GitHub releases](../../releases).

## Установка

- **NeoForge:** положите `Ceramics-neoforge-1.8.0.2+1.21.1.jar` в `mods/` сборки NeoForge 21.1.197 или новее.
- **Fabric:** положите `Ceramics-fabric-1.8.0.2+1.21.1.jar` и [Fabric API](https://modrinth.com/mod/fabric-api)
  0.116.17+1.21.1 или новее в `mods/` сборки Fabric Loader 0.16.14 или новее.
- **Сервер:** тот же jar на сервере **и** у каждого клиента.
- **Больше ничего ставить не нужно.** Оригиналу нужна библиотека Mantle от SlimeKnights, у которой нет версий под
  новые Minecraft. Порт использует вместо неё [Kilnwork](https://github.com/Vladeezyy/kilnwork) — библиотеку из тех
  частей Mantle, которые нужны Ceramics, и она встроена в jar на обоих загрузчиках.
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) по желанию, на обоих загрузчиках: в нём видны рецепты печи для
  обжига.
- На Fabric жидкостные блоки Ceramics (бочки, краны, жёлоба, глиняные вёдра) работают через transfer API Fabric, к ним
  подключаются трубы и баки других Fabric-модов.

## Возможности

- **Глиняные вёдра:** ведро для начала игры. Вода, молоко, рыхлый снег и жидкости других модов; горячие жидкости вроде
  лавы трескают ведро, и при выливании оно разбивается.
- **Работа с жидкостями:** бочки (ставьте друг на друга, чтобы увеличить объём), краны, переливающие из резервуара в
  блок ниже, жёлоба, переносящие жидкость в стороны и вниз, датчики уровня жидкости.
- **Терракота и фарфор:** терракотовые бочки, краны и жёлоба постепенно трескаются от горячей жидкости (чинятся
  глиной), фарфоровые жаростойкие.
- **Печь для обжига:** обжигает глину, кирпичи, терракоту и фарфор вдвое быстрее обычной печи.
- **Декор:** фарфор 16 цветов, радужный фарфор, тёмные, лавовые, драконьи, фарфоровые, монохромные, золотые, морские и
  радужные кирпичи со ступеньками, плитами и оградами.
- **Глиняная броня:** дешёвая броня из обожжённых пластин.
- **Языки:** английский, русский, украинский, немецкий, испанский, бразильский португальский, а также чешский,
  корейский, турецкий и китайский (упрощённый и традиционный) из оригинала. Украинский, немецкий, испанский и
  португальский — машинный перевод, исправления приветствуются.

## Что исправлено в порте

| Проблема | В оригинале |
| --- | --- |
| Рецепты печи для обжига не показывались в JEI | 1.8.0 |
| Глиняное ведро с молоком можно было наполнить трубами и резервуарами, но нельзя слить | [#88](https://github.com/KnightMiner/Ceramics/pull/88) |
| В творческой вкладке пустое ведро дважды, а вёдер с рыхлым снегом и необожжённого нет | 1.8.0 |
| Треснувшее пустое ведро показывало сырое название (`item.ceramics.cracked_empty_clay_bucket`) | 1.8.0 |
| Скрытая кнопка книги рецептов в печи для обжига всё ещё нажималась | 1.8.0 |
| `/setblock` ставил сломанную бочку (спам ошибками) или жёлоб, не сливающий вниз | 1.8.0 |
| Неверный цвет полоски прочности у треснувших бочек, кранов и жёлобов | 1.8.0 |
| Опечатка в русском; чешский перевод и обновление корейского из так и не принятых pull request | [#87](https://github.com/KnightMiner/Ceramics/pull/87), [#84](https://github.com/KnightMiner/Ceramics/pull/84) |

Найдено при порте и исправлено: жидкости в бочках, кранах и жёлобах рисовались куском атласа текстур (баг Mantle
1.21.1; Kilnwork рисует их правильно), а золотые кирпичи добывались деревянной киркой.

**Миры из 1.20.1:** глиняные вёдра сохраняют жидкость или блок, а треснувшие блоки — трещины при открытии мира в
1.21.1.

Подробно по каждой проблеме: [PORTLOG.md](PORTLOG.md).

## Тестирование

Одни и те же тесты на NeoForge и на Fabric:

- Функциональный тест на dev-сервере, 44 проверки: рецепты, вёдра, котлы, башня из бочек, которую ломают
  посередине и собирают обратно, краны, жёлоба, растрескивание от лавы, дроп, сохранение, данные из 1.20.
- Клиентская сцена со скриншотами (выше): рендер жидкостей, стадии трещин, модели вёдер, творческая вкладка, рецепты
  печи в JEI.
- Релизные jar запущены на настоящих серверах, лог чистый: NeoForge 21.1.197 и 21.1.259; Fabric Loader 0.16.14 с Fabric API 0.116.17.

## Сообщить о проблеме

О проблемах этого порта пишите в [issues этого репозитория](../../issues), а не KnightMiner.

## Авторы и лицензия

- **Ceramics** — [KnightMiner](https://github.com/KnightMiner/Ceramics), MIT ([licenses/LICENSE-MIT](licenses/LICENSE-MIT)).
- **Kilnwork** ([Vladeezyy/kilnwork](https://github.com/Vladeezyy/kilnwork), LGPL-3.0-only), встроена в jar, основана
  на частях **Mantle** от [SlimeKnights](https://github.com/SlimeKnights/Mantle) (MIT).
- Чешский перевод: RomanPlayer22. Корейский: mindy15963 и переводчики оригинала.
- Порт на NeoForge и Fabric 1.21.1, 1.21.11 и 26.x: Vladeezyy.

Порт распространяется под **LGPL-3.0-only** ([LICENSE](LICENSE), текст GPL в [licenses/COPYING](licenses/COPYING)),
Copyright (c) 2026 Vladeezyy. Форки приветствуются: они остаются открытыми, под той же лицензией и с указанием
авторов. Пожалуйста, скачивайте мод только с официальных страниц проекта.

</details>
