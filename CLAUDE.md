# LunarArc

## Project Overview

LunarArc is a **hybrid Minecraft server** that runs Paper plugins alongside mod loaders — analogous to Arclight. It bridges the Bukkit/Paper API with NeoForge, Forge, Fabric, and Quilt.

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

## Project Structure

- `projects/base/` — Bukkit/Paper stub implementations (CraftPlayer, CraftServer, etc.)
- `projects/core/` — Core hybrid logic
- `projects/loaders/fabric/` — Fabric loader integration
- `projects/loaders/forge/` — Forge loader integration
- `projects/loaders/quilt/` — Quilt loader integration
- `build/libs/` — Collected output JARs

## Build

```
./gradlew setupPaperServer build collect --no-daemon --stacktrace
```

CI runs on every push via `.github/workflows/gradle.yml`.

## Working Branch

`claude/papermc-mod-loader-compat-edmRz`
