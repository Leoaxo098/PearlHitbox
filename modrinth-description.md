# PearlHitbox

Ever tried to snipe your own ender pearl out of the air with a wind charge? In vanilla it's rough. The pearl's actual hitbox is about a quarter of a block wide and it's flying at speed, so you end up whiffing charges all day. This plugin gives flying pearls a bigger "virtual" hitbox so the wind charge pearl trick actually works without frame-perfect aim.

## What it does

Every tick the plugin looks for wind charges near in-flight ender pearls. If one gets inside the configured radius, it counts as a hit. There are two ways to handle that hit:

**Teleport mode** (default): you warp straight to the pearl the moment the charge connects, same as a normal pearl landing. Handy for cutting travel time short.

**Boost mode**: nobody teleports. The charge knocks the pearl in the direction it was flying and the pearl keeps going, so you can launch throws over walls or just make pearls fly further.

One thing vanilla makes really annoying: when you hit your own pearl, the same wind charge usually smacks *you* right as you teleport onto it. The plugin deals with that. By default the charge gets deflected past you, and the pearl keeps its momentum so mid-air warps don't feel janky.

## Setup

Drop the jar in `plugins/` and restart the server. That's the whole install.

This is a Paper plugin. It needs Paper 1.21 or newer (Purpur works too). It will not load on Spigot, and I'm not planning to make it do so.

After poking at the config, use `/pearlhitbox reload` and `/pearlhitbox info` to see what's active.

## Config

The generated `config.yml` is heavily commented, but the short version:

- `radius` — how big the virtual hitbox is. The default 0.6 is pretty forgiving. Vanilla is about 0.125. Crank it up to 1.2 if you want the trick to feel casual.
- `mode` — TELEPORT or BOOST, as above.
- `consume-projectile` / `redirect-trigger-to-look` — what happens to the wind charge after it hits.
- `teleport.*` — damage on landing, vertical offset, whether the pearl gets consumed, etc.
- `allowed-triggers.*` — only wind charges work by default, but if you set `only-windcharge: false` you can list other projectile types (arrows, tridents, snowballs, whatever) as triggers.

Particles are off by default, sound is on.

## Commands and permissions

`/pearlhitbox <reload|info>` (aliases `/ph`, `/pearlhb`) — permission `pearlhitbox.admin`, op by default.

## For plugin devs

There's a `PearlHitboxHitEvent` that fires every time a trigger registers a hit on a pearl. It's cancellable and exposes the pearl and the trigger projectile, so you can hook in scoring, cooldowns, custom effects, whatever you need.

## Notes

- Server-side only. Players don't need anything installed.
- If hits aren't registering for some reason, set `debug: true` in the config, reload, throw a pearl and hit it, then check the console.
- Found a bug or something behaves weird? Open an issue on GitHub.

License is MIT, do what you want with it.
