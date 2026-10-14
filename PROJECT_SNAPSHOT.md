# Project snapshot / 项目快照

Cigarettes: Embers / 香烟：余烬 0.1.2 — ShenXiangiioo.
Minecraft 1.20.1 · Forge 47.4.10 · Java 17 · network protocol 12.
Public source: https://github.com/ShenXiangiioo/Cigarettes-Embers-Mod

## Current release

The optional Commerce qualification dispenser now has a Cigarettes: Embers page in New certificate. Checking the biome waiver binds the privilege to the issued certificate ID. CultivationPermits stores only those IDs in overworld SavedData; actual holder UUID, expiry and revocation are checked against Commerce for every server-side planting action. No certificate in inventory is required once it is bound; possessing somebody else's certificate grants nothing. Unassigned certificates use Commerce's claim flow. Soil rules remain, and expiry does not alter planted crops.

Metadata author is ShenXiangiioo; homepage is the author's CurseForge profile. The mod includes an original logo. In-game text has 274 matching Chinese and English entries, matching placeholders and 48 language-specific pack textures.

## Verified

77 Forge GameTests and 5 JUnit tests passed. The formal release JAR started on a dedicated server without Commerce. Commerce 1.6.29 was then tested with two independent clients: a bound holder planted purple tobacco in plains; another player was denied; revocation through the actual UI immediately removed access without removing crops; an ordinary unchecked certificate gave no waiver. Singleplayer checks covered real issuance, expiration, all four tobacco colors, unassigned claim, stale requests, tablet authority and SavedData roundtrip. Chinese and English forms were inspected in game.

Optional Commerce API source reference: 1.6.30 project snapshot, checked 2026-10-14. Runtime verification is 1.6.29; no Commerce repository changes are required. Unknown future Commerce APIs are not promised compatible.

Previous 0.1.1 verification remains relevant for unchanged mechanics: all nine effect-bearing variants, 17 shared-input processing recipes, four tobacco regions, drying progress, JEI, 458 loaded foods, 14 multiplayer interaction/production/NPC scenarios. These were not represented as newly repeated visual checks in 0.1.2.

## Build and boundaries

`gradlew.bat build` builds the release; `runGameTestServer` runs Forge tests and `test` runs JUnit. `qaJar` is for isolated testing only and must not be installed for players. Normal JARs exclude QA classes. Mixin refmap entries are maintained explicitly; test actual reobfuscated JARs after changing mixins.

Optional adapters are in compat and mixin. The server owns certificate permissions, stress, items, currency and benefits. Original internal namespace embers and item IDs are preserved for existing worlds. All peers need the same version. Back up worlds before upgrading or downgrading.

## 本版要点

资格证发放器 → 新增资格证 → 香烟:余烬，勾选后出现“已取消不同花色烟草种植群系限制”。仅绑定玩家在证件有效期内可跨群系种植。服务器逐次检查真实资格证记录；过期、吊销立即失效；作物和土壤规则不被修改。商业模组不安装时仍可正常游玩，JEI 亦为可选。

本版已完成中英界面实机检查、77项游戏测试、5项单元测试及4个双客户端资格证场景。公开资料包含英文在前的双语玩法说明、独立标志、实际游戏截图、源码及问题反馈入口。CurseForge 上架资料已准备；平台审核状态不由本地构建决定。
