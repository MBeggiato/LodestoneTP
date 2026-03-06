# LodestoneTP Agent Guidelines

This file contains build commands and code style guidelines for agentic coding assistants working on this repository.

## Build Commands

**Build the project:**
```bash
./gradlew build
```

The JAR is output to `build/libs/` and auto-deployed to `server/plugins/` if the directory exists.

**Clean build:**
```bash
./gradlew clean build
```

**Deploy to server:**
```bash
./gradlew deployToServer
```

**Note:** This project currently has no automated tests. Add tests in `src/test/java/io/github/marcel/loadstonetp/` following the same package structure as main code.

## Project Overview

- **Type:** Minecraft Paper 1.21.11+ plugin
- **Language:** Java 21
- **Build System:** Gradle (Kotlin DSL)
- **Database:** SQLite with automated migrations
- **API:** Paper API + Paper Dialog API + Adventure text components

## Code Style Guidelines

### Package Structure
All code follows this pattern: `io.github.marcel.loadstonetp.{category}`
- `commands/` - Brigadier command handlers
- `listeners/` - Bukkit event listeners
- `model/` - Data classes (use records for immutable DTOs)
- `db/` - Database operations and migrations
- `dialogs/` - Paper Dialog UI components

### Naming Conventions
- **Classes:** PascalCase (e.g., `CooldownManager`, `LodestoneInteractListener`)
- **Methods:** camelCase (e.g., `getRemainingCooldown`, `isOnCooldown`)
- **Variables:** camelCase (e.g., `playerUuid`, `teleporterId`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `CURRENT_VERSION`)
- **Migration methods:** `migrateToV1`, `migrateToV2`, etc.

### Imports
- Standard order: `java.*`, then external libraries, then internal packages
- No wildcard imports (e.g., avoid `import java.util.*`)
- Keep imports sorted and remove unused ones

### Classes & Records
- One public class/record per file
- Filename matches the public class name exactly
- Utility classes should have private constructors to prevent instantiation
- Use Java records for immutable data transfer objects (e.g., `Teleporter`)

### Error Handling
- Use try-with-resources for `PreparedStatement`, `Statement`, `ResultSet`, `Connection`
- Log database errors at `Level.WARNING`, critical failures at `Level.SEVERE`
- Return `null` for "not found" queries (e.g., `getTeleporterAt`)
- Return `boolean` for success/failure operations (e.g., `addTeleporter`, `removeTeleporter`)
- Gracefully handle null checks (e.g., `Bukkit.getWorld()` returns null for unloaded worlds)

### Logging
- Use `plugin.getLogger()` from the main plugin class
- Log at `Level.SEVERE` for failures that prevent plugin initialization
- Log at `Level.WARNING` for recoverable errors or missing data
- Log at `Level.INFO` for important operations (migrations, initialization)

### Database Operations
- **Always use PreparedStatement** to prevent SQL injection
- Parameterize all user input in queries
- Use text blocks (`"""..."""`) for multi-line SQL queries
- Close resources in try-with-resources blocks
- Update `CURRENT_VERSION` constant when adding migrations

### String Handling
- Use Java 21 text blocks for multi-line SQL queries
- Use Adventure `Component` for all player-facing messages with `NamedTextColor`
- Store UUIDs as strings in the database: `player.getUniqueId().toString()`

### Configuration
- Access config via `plugin.getConfig()`
- Always provide default values: `getConfig().getString("key", "default")`
- Use types explicitly: `getInt()`, `getBoolean()`, `getString()`

### Event Listeners
- Register via `getServer().getPluginManager().registerEvents()`
- Use `@EventHandler` annotation
- Validate early: check action, hand, clicked block type
- Use `event.setCancelled(true)` to prevent vanilla behavior
- Use `event.getHand() == EquipmentSlot.HAND` to avoid double-triggering

### Commands (Brigadier)
- Build command nodes using `Commands.literal()`
- Add permission checks with `.requires(source -> source.getSender().hasPermission("perm"))`
- Use `CommandSourceStack` and check sender type: `source.getSender() instanceof Player`
- Return `Command.SINGLE_SUCCESS` from command executes

### Code Patterns
- Early returns for preconditions and validation
- Avoid nested if-statements; return early instead
- Check nulls before dereferencing Bukkit APIs that may return null
- Use `player.hasPermission("lodestonetp.admin")` for admin checks
- Admins bypass cooldowns, costs, and access restrictions

### Dialog API
- Build dialogs using `Dialog` builder pattern from `io.papermc.paper.dialog`
- Show dialogs with `player.showDialog(dialog)`
- Separate dialog building into helper classes (see `AdminDialogs`, `TeleporterDialogs`)

## Development Notes

- The plugin auto-detects multiblock structure (Polished Blackstone Bricks above/below Lodestone)
- Teleporters are destroyed if their structure is broken
- Directional spawning: players face the teleporter, offset 1.5 blocks forward
- Async teleportation logic is safe and non-blocking
- Database migrations run automatically on plugin startup
- Cooldowns are in-memory only (reset on server restart)

### Permissions

**CRITICAL:** Always consider permissions when adding new features.

- Every action that restricts access needs a permission check
- Use `player.hasPermission("permission.name")` or add command/node permission requirements
- When checking a specific management permission (e.g., `manage_networks`), ALSO check `lodestonetp.admin`:
  ```java
  if (player.hasPermission("lodestonetp.manage_xxx") || player.hasPermission("lodestonetp.admin")) {
      // Allow action
  }
  ```
- Add new permissions to `permissions.yml` with:
  - `description`: clear, concise description of what the permission does
  - `default`: usually `op` for management features, `true` for basic usage
- Document permissions in README.md with a table format
- Follow existing permission pattern: `lodestonetp.{feature}.{action}`
- Admins (`lodestonetp.admin`) should bypass ALL restrictions by default
- Example permission hierarchy:
  ```
  lodestonetp.use           # Basic usage (default: true)
  lodestonetp.create        # Create teleporters (default: op)
  lodestonetp.manage_xxx    # Manage specific feature (default: op)
  lodestonetp.admin         # Full admin access (default: op)
  ```

### Configuration Management

**When adding new configuration options:**

1. **Add to `src/main/resources/config.yml`:**
   - Use the existing section structure with comments
   - Provide sensible defaults
   - Add clear comments explaining the setting

2. **Update `README.md`:**
   - Add the new config option to the configuration section
   - Keep the YAML example up-to-date
   - Include all new config options with their defaults

3. **Add to Admin Dialog Panel (`AdminDialogs.java`):**
   - Create a dialog method (e.g., `createYourFeatureDialog()`)
   - Add button in main admin panel to access it
   - Allow in-game modification without config editing
   - Save changes to `config.yml` via `plugin.getConfig().set()` and `plugin.saveConfig()`

**Example flow for adding a new config option:**
```
1. Add to config.yml
2. Update README.md config example
3. Add dialog in AdminDialogs
4. Wire dialog button in main admin panel
5. Feature now fully accessible in-game!
```

### Feature Completeness & User Touchpoints

**CRITICAL:** Prevent "backend-only" features that users can't actually experience.

**Root Cause - Networks Feature Failure:**
- Implemented database, management dialogs, and command
- **Forgot to update the main user-facing dialog** (teleporter selection)
- Result: Players could create networks but saw no benefit in gameplay

**Prevention Strategy:**

**1. Identify ALL User Touchpoints Before Coding**
Before implementing, map where users will interact with the feature:
- Management UI (settings, configuration)
- Main gameplay UI (teleporter selection, combat, inventory)
- Feedback/Messages (chat, notifications)
- External integrations (other plugins, events)

**Example for Networks:**
- ✅ Create/Rename/Delete networks (management)
- ✅ Assign teleporters to networks (management)
- ❌ **MISSING**: Browse teleporters grouped by network (main gameplay) ← THIS CAUSED THE BUG

**2. User Journey Testing (Before Implementation)**
Ask yourself these questions:
- What problem does this solve for the player?
- Where will players **notice** this feature in their daily gameplay?
- What's the before/after experience?
- Can a player use the feature without knowing it exists?

**3. Complete Touchpoint Checklist**
For each feature, ensure you've updated:
- [ ] Database schema (if needed)
- [ ] Backend/Model classes
- [ ] Management/configuration UI (admin panels, dialogs)
- [ ] **MAIN GAMEPLAY UI** - where players interact during normal play
- [ ] User feedback (messages, sounds, particles)
- [ ] Permissions (with admin bypass)
- [ ] README.md documentation
- [ ] AGENTS.md (if adding new patterns)

**4. Example: What Would Have Prevented the Networks Bug**

**❌ Wrong approach (what I did):**
1. Add database migration ✅
2. Create NetworkDialogs for management ✅
3. Add /lodestonetp networks command ✅
4. Test command works ✅
5. Done

**✅ Correct approach (what to do):**
1. **User journey**: "I create a network, assign teleporters, then... what?" → Need to browse by network
2. **Identify touchpoints**: Management UI + **teleporter selection dialog**
3. **Implementation plan**:
   - Database + NetworkDialogs + command
   - **ALSO**: Update TeleporterDialogs.createTeleportDialog() to group by network
4. **Test full flow**: Create network → assign teleporters → try to teleport → see groups
5. Done

**5. Real-World Scenarios**
Before committing code, verify with actual gameplay scenarios:
- "A player has 3 networks with 5 teleporters each. They right-click a lodestone. What do they see?"
- "A player favorites 3 teleporters. How do they access favorites quickly?"
- "An admin wants to set a per-teleporter cooldown. Where do they click?"

If you can't answer these scenarios, the feature isn't complete.

## Adding New Features

When adding features:
1. Check if it needs a config entry - add to `config.yml` with defaults
2. Update `README.md` configuration example if adding config options
3. Add admin dialog in `AdminDialogs.java` for config management
4. If it affects database, create a migration method and increment `CURRENT_VERSION`
5. Add permission to `permissions.yml` if it restricts actions
6. Document permissions in README.md with a table
7. Use Adventure `Component` with `NamedTextColor` for all user messages
8. Log important operations with appropriate level
9. Consider if admin permission should bypass the feature
