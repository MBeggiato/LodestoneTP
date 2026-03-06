# LodestoneTP — Roadmap

## ⭐ Tier 1 — High Impact

### Teleporter Networks / Groups

Organize teleporters into named networks (e.g. "Nether Highway", "Trading Posts"). The GUI shows grouped teleporters instead of one flat list. Reduces clutter for servers with many teleporters.

- [x] Network CRUD (create, rename, delete)
- [x] Assign teleporters to networks via GUI
- [x] Grouped view in teleporter list
- [x] Permissions per network

### Favorites & Sorting

Players can star/favorite teleporters for quick access. A "Favorites" tab at the top of the teleporter list GUI.

- [x] Favorite toggle in teleporter GUI
- [x] Persistent per-player favorites
- [x] Favorites section at top of list
- [x] Sort options (alphabetical, distance, most used)

### Teleporter Linking (A↔B Pairs)

Two lodestones can be directly linked — step on one, get teleported to the other. Think Nether portal pairs but with lodestones. Coexists with the current GUI-based system as an optional mode.

- [x] Link command / GUI option to pair two teleporters
- [x] Auto-teleport on interaction with linked teleporter
- [x] Visual indicator for linked pairs
- [x] Unlink option

### Per-Teleporter Cooldowns

Admins can set different cooldowns per teleporter. A hub teleporter could have 0 cooldown while remote ones have 30s. Configurable via the admin panel.

- [x] Cooldown override field in teleporter settings
- [x] Admin panel UI for cooldown management
- [x] Fallback to global cooldown when unset

---

## 🔥 Tier 2 — Cool & Unique

### Teleport Warmup / Channeling

Players must stand still for X seconds before teleporting (configurable). Movement cancels the teleport. Shows a boss bar or action bar countdown. Prevents combat-logging via teleport.

- [x] Configurable warmup duration
- [x] Movement detection to cancel
- [x] Boss bar / action bar countdown display
- [x] Bypass permission for warmup
- [x] Cancel message + sound

### Fuel / Charges System

Teleporters consume fuel (configurable item — ender pearls, lodestone compass, etc.). Players deposit fuel to keep a teleporter active. Survival-friendly resource sink beyond economy costs.

- [ ] Configurable fuel item type
- [ ] Fuel deposit GUI
- [ ] Charge counter per teleporter
- [ ] Low fuel warnings
- [ ] Auto-disable when empty

### Teleporter Upgrades

Tiers for teleporters (Basic → Enhanced → Legendary) that unlock benefits: faster warmup, lower cost, larger range, better particles. Players upgrade through a GUI or by placing items.

- [ ] Tier system (3+ levels)
- [ ] Per-tier config for warmup, cost, range, effects
- [ ] Upgrade GUI with item requirements
- [ ] Visual tier indicator on teleporter

### Cross-World / Cross-Dimension Teleporting

Allow teleporters to connect across worlds (Overworld ↔ Nether ↔ End). Configurable cost multiplier for cross-dimension travel.

- [ ] Cross-world teleporter visibility in GUI
- [ ] Dimension cost multiplier config
- [ ] Permission node for cross-world teleporting
- [ ] World filter in teleporter list

---

## 💡 Tier 3 — Quality of Life

### Teleporter Compass

A lodestone compass that points to the player's most-used or last-used teleporter. Right-click to open the teleporter GUI from anywhere (with range limit or cooldown).

- [ ] Custom compass item with NBT/PDC data
- [ ] Right-click opens teleporter GUI
- [ ] Optional range limit
- [ ] Crafting recipe or admin command to give

### Usage Statistics

Track how many times each teleporter is used, by whom, and when. Show stats in the admin panel.

- [ ] Per-teleporter use counter
- [ ] Per-player use tracking
- [ ] Stats page in admin panel
- [ ] Top teleporters leaderboard

### Teleporter Search / Filter

For servers with many teleporters, add a search bar and filters (by owner, distance, world, network) in the GUI.

- [ ] Anvil-input search in chest GUI
- [ ] Filter by owner, world, network
- [ ] Sort by distance from player
- [ ] Pagination for large lists

### Redstone Integration

Redstone signals can enable/disable teleporters. Pressure plate on a lodestone could trigger the teleport GUI or auto-teleport to a linked destination. Opens up adventure map possibilities.

- [ ] Redstone-powered enable/disable
- [ ] Pressure plate trigger mode
- [ ] Configurable per-teleporter
- [ ] Redstone output on teleport event

---

## 🎨 Tier 4 — Polish & Flair

### Custom Teleport Animations

Multiple animation presets (spiral particles, lightning strike, portal swirl, ender-style) configurable per-teleporter or globally.

- [ ] Animation preset system
- [ ] Per-teleporter animation selection
- [ ] Config for global default animation
- [ ] Smooth multi-tick animation sequences

### Hologram Labels (Display Entities)

Floating text above teleporters showing the name using Paper's Display Entity API. Text only — no resource packs needed.

- [ ] Text Display entity spawning above lodestone
- [ ] Auto-update on rename
- [ ] Configurable text color and style
- [ ] Toggle visibility per-player or globally

### Sounds & Music

Configurable ambient sounds near teleporters (subtle hum, mystical chime). Distinct arrival/departure sounds.

- [ ] Ambient sound loop near teleporters
- [ ] Configurable arrival/departure sounds
- [ ] Per-teleporter sound override
- [ ] Volume and range settings
