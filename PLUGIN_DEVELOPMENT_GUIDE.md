# Paper Plugin Development Guide

Ein umfassender Guide für die Entwicklung von qualitativ hochwertigen Minecraft Paper Plugins basierend auf Erfahrungen aus dem LodestoneTP-Projekt.

---

## 🏗️ Projekt-Setup

### Gradle mit Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.jar {
    manifest.attributes["Main-Class"] = "your.main.MainClass"
}
```

**Warum Gradle?**

- Bessere Dependency-Management als Maven
- Kotlin DSL ist leichter lesbar als XML
- Plugin-Kompilierung & Packaging automatisiert

---

## 📦 Paket-Struktur

```
io.github.username.pluginname/
├── PluginName.java                 # Main Plugin Class
├── commands/                        # Brigadier/CommandMap commands
├── listeners/                       # Bukkit Event Listeners
├── model/                          # Data Classes (use Records)
├── db/                             # Database & Schema
├── dialogs/                        # Paper Dialog API UI
├── managers/                       # Business Logic Manager Classes
└── utils/                          # Utility Classes
```

**Wichtig:**

- **Ein public class pro Datei** — keine Datei=Klasse-Verletzungen
- **Records für DTOs** — immutable und thread-safe
- **Manager-Pattern** — zentrale Verwaltung von Systemen

---

## 🎨 Code-Style Conventions

### Naming

```java
// ✅ RICHTIG
public class TeleporterManager { }
public record Teleporter(int id, String name) { }
public void handlePlayerInteract(Player player) { }
private static final int COOLDOWN_SECONDS = 30;
private static final String CONFIG_KEY = "cooldown.seconds";

// ❌ FALSCH
public class teleporter_manager { }
public class TeleporterManagerImpl { }
public void handle(Player p) { }
private int cooldownSeconds; // Sollte constant sein
```

### Imports

```java
// ✅ RICHTIG - Sortiert und spezifisch
import java.io.File;
import java.sql.*;
import java.util.*;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import io.github.username.plugin.model.Teleporter;

// ❌ FALSCH - Wildcard imports
import java.util.*;
import org.bukkit.*;
```

### Javadoc & Comments

```java
/**
 * Teleports a player to a destination, handling cooldown and effects.
 * @param player The player to teleport
 * @param destination The target location
 * @return true if teleport was successful, false if on cooldown
 */
public boolean teleportPlayer(Player player, Location destination) {
    // Early return for preconditions
    if (player == null || destination == null) return false;

    // Clear business logic in blocks
    String playerUuid = player.getUniqueId().toString();
    if (cooldownManager.isOnCooldown(playerUuid)) {
        return false;
    }

    // Perform action
    player.teleport(destination);
    return true;
}
```

---

## 🚨 Häufige Fehler & Lösungen

### 1. **Event Listeners mit ignoreCancelled = false**

```java
// ❌ FALSCH - Ignoriert andere Plugins' Cancelations
@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    // Wird trotzdem aufgerufen, auch wenn ein region plugin es canceled
}

// ✅ RICHTIG - Respektiert andere Plugins
@EventHandler(ignoreCancelled = true)
public void onPlayerInteract(PlayerInteractEvent event) {
    // Wird nur aufgerufen, wenn nicht canceled
}
```

### 2. **Locale-unsichere Stringoperationen**

```java
// ❌ FALSCH - Türkisches Locale: "I" → "ı" statt "i"
Comparator.comparing(tp -> tp.name().toLowerCase())

// ✅ RICHTIG - Deterministische Sortierung weltweit
Comparator.comparing(tp -> tp.name().toLowerCase(java.util.Locale.ROOT))
```

### 3. **N+1 Query Problem**

```java
// ❌ FALSCH - Pro Teleporter eine DB-Abfrage
for (Teleporter tp : teleporters) {
    boolean isFavorite = db.isFavorite(playerUuid, tp.id()); // 50 Queries!
}

// ✅ RICHTIG - Alle auf einmal laden
Set<Integer> favoriteIds = db.getFavoriteIds(playerUuid); // 1 Query!
for (Teleporter tp : teleporters) {
    boolean isFavorite = favoriteIds.contains(tp.id()); // O(1) Memory Lookup
}
```

### 4. **Fehlende Transaction Safety**

```java
// ❌ FALSCH - Kann orphaned Foreign Keys hinterlassen
public boolean deleteNetwork(int networkId) {
    return database.execute("DELETE FROM networks WHERE id = ?", networkId);
    // Aber die zugehörigen teleporters.network_id Felder sind jetzt null!
}

// ✅ RICHTIG - Transaktionale Integrität
public boolean deleteNetwork(int networkId) {
    boolean autoCommit = connection.getAutoCommit();
    try {
        connection.setAutoCommit(false);
        database.execute("UPDATE teleporters SET network_id = NULL WHERE network_id = ?", networkId);
        database.execute("DELETE FROM networks WHERE id = ?", networkId);
        connection.commit();
        return true;
    } catch (SQLException e) {
        connection.rollback();
        return false;
    } finally {
        connection.setAutoCommit(autoCommit);
    }
}
```

### 5. **Fehlende Empty-State Handler**

```java
// ❌ FALSCH - Crasht wenn Liste leer ist
List<ActionButton> buttons = new ArrayList<>();
for (Item item : items) {
    buttons.add(createButton(item));
}
dialog.showMultiAction(buttons); // IllegalArgumentException: actions cannot be empty!

// ✅ RICHTIG - Empty-State Dialog
if (items.isEmpty()) {
    return createEmptyStateDialog();
}
List<ActionButton> buttons = /* ... */;
return dialog.showMultiAction(buttons);
```

### 6. **Config-Typen nicht beachtet**

```java
// ❌ FALSCH - Crash wenn Config int statt String hat
String value = plugin.getConfig().getString("key");

// ✅ RICHTIG - Type-safe mit Defaults
String value = plugin.getConfig().getString("key", "default");
int cooldown = plugin.getConfig().getInt("cooldown.seconds", 30);
boolean enabled = plugin.getConfig().getBoolean("feature.enabled", true);
```

### 7. **Fehlende Null-Checks für Bukkit APIs**

```java
// ❌ FALSCH - NPE wenn Welt nicht geladen ist
Location loc = new Location(Bukkit.getWorld("world"), 0, 0, 0);
player.teleport(loc); // Crash wenn "world" nicht existiert

// ✅ RICHTIG - Null-Check
World world = Bukkit.getWorld("world");
if (world == null) {
    player.sendMessage("World not found!");
    return false;
}
player.teleport(new Location(world, 0, 0, 0));
```

### 8. **Event Handler mit Permission Check ohne OP-Bypass**

```java
// ❌ FALSCH - OPs sind manchmal nicht admin, je nach Permission Plugin
if (player.hasPermission("perm.admin")) {
    // OPs könnten hier ausgeschlossen sein
}

// ✅ RICHTIG - Immer OP-Status prüfen
if (player.isOp() || player.hasPermission("perm.admin")) {
    // Funktioniert zuverlässig
}
```

---

## 🎯 Best Practices

### Manager-Pattern für Business Logic

```java
public class CooldownManager {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean isOnCooldown(String playerUuid, int seconds) {
        Long lastUsed = cooldowns.get(playerUuid);
        if (lastUsed == null) return false;
        return System.currentTimeMillis() - lastUsed < seconds * 1000L;
    }

    public void setCooldown(String playerUuid) {
        cooldowns.put(playerUuid, System.currentTimeMillis());
    }
}
```

**Vorteile:**

- Business Logic getrennt von Event Listeners
- Leicht zu testen
- Wiederverwendbar

### Early Returns statt Nested If-Statements

```java
// ❌ FALSCH - Pyramide of Doom
public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        if (event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.LODESTONE) {
                if (/* permission check */) {
                    // Aktion
                }
            }
        }
    }
}

// ✅ RICHTIG - Early Returns
public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getClickedBlock() == null) return;
    if (event.getClickedBlock().getType() != Material.LODESTONE) return;
    if (!player.hasPermission("perm")) return;

    // Aktion - clear & readable
}
```

### Try-with-Resources für Ressourcen

```java
// ✅ RICHTIG - Auto-Cleanup
try (PreparedStatement stmt = connection.prepareStatement(sql)) {
    stmt.setString(1, playerUuid);
    try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
            // Verarbeite Zeilen
        }
    }
} catch (SQLException e) {
    logger.log(Level.WARNING, "Failed to query database", e);
}
// Statement & ResultSet werden automatisch geschlossen
```

### Logging mit korrektem Level

```java
// ✅ RICHTIG
logger.log(Level.SEVERE, "Plugin initialization failed, disabling...", e);
logger.log(Level.WARNING, "Database connection lost, retrying...", e);
logger.log(Level.INFO, "LodestoneTP v1.0 loaded successfully");
// Level.FINE ist zu unbedeutend für Plugin-Logs
```

### Database Migrations mit Versionierung

```java
private static final int CURRENT_VERSION = 5;

private void runMigrations() throws SQLException {
    int currentVersion = getDatabaseVersion();

    if (currentVersion < 1) migrateToV1();
    if (currentVersion < 2) migrateToV2();
    if (currentVersion < 3) migrateToV3();
    // ... etc

    setDatabaseVersion(CURRENT_VERSION);
}

private void migrateToV3() {
    connection.createStatement().execute("""
        ALTER TABLE teleporters ADD COLUMN cooldown_override INTEGER
        """);
}
```

---

## 🧪 Testing & Quality Assurance

### Code Review Checklist

- [ ] Alle Imports sortiert und spezifisch
- [ ] Keine wildcard imports
- [ ] Kein unused Code
- [ ] Locale.ROOT für String-Operationen
- [ ] Try-with-resources für DB-Operationen
- [ ] Null-Checks für Bukkit APIs
- [ ] OP-Bypass bei Permissions
- [ ] ignoreCancelled = true bei kritischen Events
- [ ] Empty-State Handler für Listen
- [ ] Keine nested If-Statements (Early Returns)
- [ ] Aussagekräftige Variable Namen
- [ ] Javadoc für public Methoden

### Manual Testing auf dem Server

```bash
# Test-Szenarien vor Release:
1. Plugin Load/Reload - Keine Fehler?
2. Erste Installation - Configs generiert?
3. Feature A wenn leer - Crasht oder Empty-State?
4. Feature B mit Daten - Funktioniert correct?
5. Permission Checks - OPs haben Zugriff?
6. Event Listeners - Respektiert andere Plugins?
7. Database Migration - Alt → Neu Daten intact?
```

---

## 📚 Dokumentation & Release

### README.md Must-Haves

```markdown
# Plugin Name

Kurzbeschreibung (1-2 Sätze)

## Installation

Schritt-für-Schritt

## Verwendung

Wie Spieler es nutzen

## Konfiguration

Config-Erklärung mit Beispielen

## Befehle

Alle Commands mit Permissions

## Berechtigungen

Tabelle mit Permissions

## Troubleshooting

Häufige Probleme und Lösungen
```

### Version Management

```gradle
// build.gradle.kts
version = "1.0"  // nach Release: 1.0, dann 1.1, 2.0, etc.

// paper-plugin.yml
version: 1.0
name: PluginName
description: "Was es macht"
authors: ["Dein Name"]
```

### Release Checklist

- [ ] Version in build.gradle bumped
- [ ] Full test suite passed (manual oder automated)
- [ ] README aktualisiert
- [ ] CHANGELOG erstellt
- [ ] Gradle build erfolgreich
- [ ] JAR nicht korrupt (kann entpackt werden)
- [ ] Auf Server deployed und getestet
- [ ] Git commit & tag erstellt

---

## 🔥 Lessons Learned aus LodestoneTP

### Was gut gelaufen ist

1. **Manager-Pattern** — Zentrale Verwaltung von Systemen hat sich bewährt
2. **Records für DTOs** — Clean und Thread-safe
3. **Dialog API statt Chest GUIs** — Native Unterstützung besser
4. **SQLite für Simple Games** — Kein Overhead, Daten persistent
5. **Zero-Config Philosophy** — Plugin funktioniert out-of-the-box
6. **Early Returns** — Code ist lesbarer und wartbarer
7. **API Abstraction** — DatabaseManager als zentrale Schnittstelle

### Was wir verbessert haben

1. **Event Handler Flags** → ignoreCancelled = true hinzugefügt
2. **Locale Safety** → toLowerCase(Locale.ROOT) überall
3. **Transaction Safety** → deleteNetwork jetzt mit Rollback
4. **Empty-State Handler** → Dialog crasht nicht mehr
5. **OP-Bypass** → Player.isOp() || hasPermission() Muster

### Was wir nicht machen würden

1. ❌ Feature implementieren ohne UI-Integration (Networks-Bug)
2. ❌ N+1 Queries (isFavorite loop) — Immer batch-loading
3. ❌ Nested If-Statements statt Early Returns
4. ❌ Locale-unsichere Stringoperationen
5. ❌ Fehlende Empty-State Handler für Listen

---

## 🚀 Tipps für zukünftige Projekte

### Vor dem Coding

- [ ] **Feature Map** — Wo interagieren Spieler damit?
- [ ] **Database Schema** — Alle Relationen durchdenken
- [ ] **Permission Tree** — Admin-Bypass-Strategie definieren
- [ ] **Config Structure** — Sensible Defaults setzen

### Während dem Coding

- [ ] **Teste lokal** — Server nach jedem Feature-Set
- [ ] **Code Reviews** — Gegen diese Checkliste prüfen
- [ ] **Logs sind dein Freund** — Level.INFO für wichtiges
- [ ] **Early Returns** — Lesbarkeit über Schachtelung

### Nach dem Release

- [ ] **Fehler sammeln** — In issues.md oder TODO.md
- [ ] **Code Quality** — Regelmäßig refactorn
- [ ] **Performance Monitor** — Ist das Plugin zu langsam?
- [ ] **User Feedback** — Community-Requests ernst nehmen

---

## 📖 Strukturiertes Projekt-Setup (Quick-Copy)

```bash
# Neue Plugin-Struktur
src/main/java/io/github/username/pluginname/
├── PluginName.java
├── commands/
│   └── AdminCommand.java
├── listeners/
│   └── PlayerInteractListener.java
├── model/
│   └── DataModel.java (record)
├── db/
│   ├── DatabaseManager.java
│   └── migrations.sql
├── dialogs/
│   └── DialogFactory.java
└── managers/
    ├── CooldownManager.java
    └── ConfigManager.java

src/main/resources/
├── paper-plugin.yml
├── config.yml
└── permissions.yml
```

---

## ✅ Final Checklist für jeden Release

```
Code Quality:
☐ Keine Compiler Warnings (außer deprecated APIs)
☐ JAR erfolgreich gepackt
☐ Alle Dependencies korrekt included
☐ Code nach Style-Guide formatiert

Funktionalität:
☐ Alle Features getestet
☐ Empty-States funktionieren
☐ Permissions correct
☐ Datenbank-Migration funktioniert

Dokumentation:
☐ README aktualisiert
☐ Version in Dateien bumped
☐ CHANGELOG geschrieben
☐ Javadoc für public APIs

Deployment:
☐ JAR auf Server getestet
☐ Keine Errors im Log
☐ Git commit + tag
☐ Release Notes geschrieben
```

---

**Happy coding! 🚀**

Diese Principles haben sich bei LodestoneTP bewährt und sollten auch für zukünftige Plugins gelten.
