# PearlHitbox

**PearlHitbox** is a Paper plugin for Minecraft **1.21.x** that makes the *wind charge + ender pearl* launch trick dramatically easier to land.

In vanilla Minecraft, an in-flight ender pearl has a tiny collision box (~0.25 blocks wide) and moves fast — sniping your own pearl mid-air with a thrown wind charge is nearly pixel-perfect. PearlHitbox adds a configurable **virtual detection radius** around every flying pearl: when an allowed trigger entity (by default, a wind charge) gets close enough, it registers as a direct hit.

## Features

- **Virtual hitbox** for ender pearls — expand (easier) or shrink (harder/vanilla-like) the effective hit radius
- **Two modes:**
  - `TELEPORT` — hit your pearl mid-air and warp to it instantly (classic early-warp behavior)
  - `BOOST` — kick the pearl in the wind charge's direction so it keeps flying, then vanilla teleports you when it lands
- **Deflect behavior** — the triggering wind charge can "burst" against the pearl or be deflected past you so it never damages the player performing the trick
- **Fully configurable** — radius, cooldown, damage, velocity preservation, particles, sounds, and which entity types count as triggers
- **Developer API** — a cancellable `PearlHitboxHitEvent` fires on every registered hit, so other plugins can add scoring, cooldowns, custom effects, etc.
- **Lightweight** — a single polling task, no NMS, no packet magic

## Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/pearlhitbox reload` | `/ph`, `/pearlhb` | `pearlhitbox.admin` | Reload the config |
| `/pearlhitbox info` | | `pearlhitbox.admin` | Show the current active settings |

`pearlhitbox.admin` defaults to server operators.

## Installation

1. Download the latest jar from the [Releases](https://github.com/Leoaxo098/PearlHitbox/releases) page or Modrinth.
2. Drop it into your server's `plugins/` folder.
3. Restart the server (Paper 1.21.x).
4. Edit `plugins/PearlHitbox/config.yml` to taste and run `/pearlhitbox reload`.

Requires **Java 21** (bundled with Paper 1.21+).

## Configuration

The full, commented default config lives at [`src/main/resources/config.yml`](src/main/resources/config.yml). Highlights:

| Option | Default | Description |
|---|---|---|
| `hitbox.enabled` | `true` | Master toggle |
| `hitbox.radius` | `0.6` | Virtual hit radius in blocks (vanilla is ~0.125) |
| `hitbox.check-interval-ticks` | `1` | How often pearls are scanned |
| `hitbox.hit-cooldown-ticks` | `5` | Ticks before the same pearl can be hit again |
| `hitbox.mode` | `TELEPORT` | `TELEPORT` or `BOOST` |
| `hitbox.consume-projectile` | `false` | Whether the trigger projectile is removed on impact |
| `hitbox.redirect-trigger-to-look` | `true` | Deflect a surviving wind charge along your look direction |
| `hitbox.effects.*` | particles `false`, sound `true` | Impact particles and sound |
| `hitbox.teleport.*` | — | Damage, vertical offset, look direction, pearl removal |
| `hitbox.boost.*` | — | Push strength and vertical boost for BOOST mode |
| `allowed-triggers.*` | wind charges only | Which entity types may trigger a hit |
| `allowed-triggers.extra-entity-types` | `[]` | Extra EntityType names allowed when `only-windcharge` is false |
| `debug` | `false` | Verbose logging of every detected hit |

## Building from source

```bash
mvn package
```

The jar is output to `target/PearlHitbox-<version>.jar`.

## License

This project is licensed under the [MIT License](LICENSE).
