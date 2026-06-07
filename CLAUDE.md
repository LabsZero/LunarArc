# LunarArc

## Project Overview

LunarArc is a **hybrid Minecraft server** that runs Paper plugins alongside mod loaders — analogous to Arclight. It bridges the Bukkit/Paper API with NeoForge, Forge, Fabric, and Quilt. Uses **PaperMC** (not Spigot) as the Bukkit implementation base.

## Key Facts

- **Minecraft version**: 1.21.1
- **Paper API**: `1.21.1-R0.1-SNAPSHOT` build 133 — this is NOT the latest Paper API
- **Build tool**: Architectury Loom 1.14.473, Gradle 9.5.0
- **Mod loaders**: NeoForge, Forge, Fabric, Quilt (multi-platform via Architectury)

## Critical: Paper API Version

The project compiles against **Paper 1.21.1-R0.1-SNAPSHOT build 133**, which is older than the current Paper `main` branch. Many methods in modern Paper do NOT exist here. Do not add `@Override` stubs based on the current Paper GitHub `main` branch — only implement methods that actually exist in build 133.

Known methods that do NOT exist in build 133 (do not `@Override` these):
- `getPlayerListOrder()` / `setPlayerListOrder(int)`
- `givenEffect(PotionEffect, Entity)`
- `getEnderPearls()`
- `removeResourcePacks(boolean, UUID, UUID...)`
- `getResourcePackIds()`
- `getResourcePackInfo(UUID)`
- `getResourcePacksHash()`
- `getCurrentInput()` (uses `org.bukkit.Input` which doesn't exist in build 133)

## Project Structure (Arclight-style)

Mirrors Arclight's folder layout with `ampznetwork`/`LunarArc` branding and PaperMC instead of Spigot.

| Module | Purpose |
|--------|---------|
| `lunararc-api/` | Non-Minecraft utilities & shared API (analogous to `arclight-api`) |
| `lunararc-common/` | Core hybrid code: mixins in `io/ampznetwork/lunararc/common/mixin/`, bridges in `io/ampznetwork/lunararc/common/bridge/`, remapper + mod utilities in `io/ampznetwork/lunararc/common/mod/` |
| `lunararc-neoforge/` | NeoForge-specific integration (analogous to `arclight-neoforge`) |
| `lunararc-forge/` | Forge-specific integration (analogous to `arclight-forge`) |
| `lunararc-fabric/` | Fabric-specific integration (analogous to `arclight-fabric`) |
| `lunararc-quilt/` | Quilt-specific integration |
| `bootstrap/` | Assembles one per-platform fat-JAR server artifact |
| `installer/` | Downloads & installs mod loaders automatically at runtime |
| `i18n-config/` | Multi-language translation strings |
| `buildSrc/` | Custom Gradle build logic (Paper JAR remapping utilities) |
| `gradle/` | `libs.versions.toml` — central dependency version catalog |
| `build/libs/` | Collected output JARs (neoforge, forge, fabric, quilt) |

## Build

```
./gradlew setupPaperServer build collect --no-daemon --stacktrace
```

CI runs on every push via `.github/workflows/gradle.yml`.

## Working Branch

`claude/papermc-mod-loader-compat-edmRz`
