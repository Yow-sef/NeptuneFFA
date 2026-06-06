# NeptuneFFA 


A high-performance, fully featured Free-For-All (FFA) addon built for the **Neptune Core** tournament/practice framework. 

NeptuneFFA allows server administrators to define FFA arenas per kit, manage live sessions with automated resets, track session and lifetime stats, and display dynamic scoreboards with placeholders.

---

## 📋 Table of Contents
1. [Prerequisites](#-prerequisites)
2. [Installation](#-installation)
3. [Configuration](#-configuration)
   - [config.yml](#configyml)
   - [kits.yml](#kitsyml)
   - [messages.yml](#messagesyml)
4. [Setting Up an FFA Arena (Step-by-Step)](#-setting-up-an-ffa-arena-step-by-step)
5. [In-Game Admin GUI](#-in-game-admin-gui)
6. [Commands & Permissions](#-commands--permissions)
7. [Scoreboard & Placeholders](#-scoreboard--placeholders)
8. [Developer Info & Build Guide](#-developer-info--build-guide)

---

## 🛠️ Prerequisites
Before installing NeptuneFFA, make sure your server meets the following requirements:
* **Minecraft Version**: Spigot or Paper 1.21+
* **Java Version**: Java 21 or higher
* **Required Dependency**: [Neptune Core] (must be installed and enabled on the server)

---

## 🚀 Installation

### 1. Direct Installation
1. Compile the project (see [Build Guide](#-developer-info--build-guide)) or download the compiled `NeptuneFFA-1.0.0.jar`.
2. Drop `NeptuneFFA-1.0.0.jar` into your Minecraft server's `plugins/` directory.
3. Restart or start the server. This will automatically generate the default configuration files.

---

## ⚙️ Configuration

NeptuneFFA generates three primary configuration files under `plugins/NeptuneFFA/`.

### `config.yml`
Controls global features like the Lobby Join Item, global respawn delays, arena reset warnings, and the scoreboard layout.

```yaml
ffa:
  # Item given to players in the lobby (State: IN_LOBBY) to open the FFA Selector
  lobby-item:
    enabled: true
    slot: 8
    material: DIAMOND_SWORD
    name: "&c&lFree For All &7(Right Click)"
    lore:
      - "&7Jump into an FFA arena"

  # General settings
  respawn-delay-seconds: 5
  reset-warning-seconds: [60, 30, 10, 5]
  track-neptune-kit-stats: true

  # Scoreboard settings for players in the "IN_FFA" state
  scoreboard:
    update-interval-ticks: 20
    title: "&c&lFFA"
    lines:
      - "&7&m--------------------"
      - "&fKit: &c<ffa_kit>"
      - "&fArena: &e<ffa_arena>"
      - "&fPlayers: &a<ffa_players>"
      - " "
      - "&6Reset in: &e<ffa_reset_timer>"
      - " "
      - "&7— Session —"
      - "&fKills: &a<ffa_session_kills>"
      - "&fDeaths: &c<ffa_session_deaths>"
      - "&fStreak: &e<ffa_session_streak>"
      - "&fBest Streak: &6<ffa_session_best_streak>"
      - "&fKDR: &b<ffa_session_kdr>"
      - " "
      - "&7— Lifetime —"
      - "&fKills: &a<ffa_lifetime_kills>"
      - "&fDeaths: &c<ffa_lifetime_deaths>"
      - "&fBest Streak: &6<ffa_lifetime_best_streak>"
      - "&fKDR: &b<ffa_kdr>"
      - " "
      - "&7— Ranking —"
      - "&fGlobal Rank: &d#<ffa_rank>"
      - "&fTop Killer: &e<ffa_top_killer> &7(<ffa_top_killer_kills>)"
      - "&fKills to Pass: &c<ffa_kills_to_next_rank>"
      - " "
      - "&bserver.net"
      - "&7&m--------------------"
```

### `kits.yml`
Defines kit-specific FFA arenas and settings.

```yaml
kits:
  # Example Configuration:
  NoDebuff:
    enabled: true                    # Enable/disable FFA for this kit
    arena: CastleTop                 # Name of the arena
    worldgen: false                  # Whether to auto-regenerate/reset blocks in the world
    reset-interval-minutes: 10       # Reset task timer (recreates session, resets blocks)
    respawn-delay-override: -1       # Overrides global respawn delay (-1 to use global config)
    spawn-points:                    # List of serialized locations players spawn at
      - "world,0.5,64.0,0.5,90.0,0.0"
```

### `messages.yml`
Allows localization and customization of all messages.

```yaml
ffa-join:        "&a{player} &7joined FFA &e({kit})&7."
ffa-leave:       "&c{player} &7left FFA &e({kit})&7."
ffa-kill:        "&c{killer} &7killed &c{victim} &8[&e{victim_session_kills} kills&8]"
ffa-reset-warn:  "&6[FFA] &eArena resets in &c{seconds}s&e!"
ffa-reset-kick:  "&6[FFA] &eArena is resetting. Returning you to lobby."
ffa-reset-open:  "&6[FFA] &eArena &a{arena} &ehas reopened!"
ffa-respawn:     "&7Respawning in &e{seconds}&7..."
ffa-no-session:  "&cNo FFA session is open for that kit."
ffa-not-in-ffa:  "&cYou are not in an FFA session."
ffa-already-in:  "&cYou are already in an FFA session."
```

---

## 🗺️ Setting Up an FFA Arena (Step-by-Step)

To set up a kit for Free-For-All:

1. **Create the Kit in Neptune**:
   Make sure the kit exists in your Neptune Core database/configs.
2. **Register it in `kits.yml`**:
   Add the kit name as a key in `kits.yml` (case-sensitive matching Neptune's kit name). For example, `NoDebuff`.
3. **Enable the Kit**:
   Set `enabled: true` and name the arena under `arena: <ArenaName>`.
4. **Set Spawn Points**:
   You can add spawn points in two ways:
   * **In-Game Command**: Stand at the desired spawn point in the arena and run:
     `/ffaadmin addspawn <kit>`
   * **In-Game GUI**: Open `/ffaadmin`, click **Kit Settings**, select your kit, click **Spawn Points**, and click **Add Current Location**.
5. **Reload the Plugin**:
   Run `/ffaadmin reload` to reload configs and automatically build and start the FFA sessions for enabled kits.

---

## 🖥️ In-Game Admin GUI

Administrators with the `neptuneffa.admin` permission can use the built-in GUI to manage FFA sessions and kits dynamically:
* Open the menu by running `/ffaadmin` (or `/ffaadmin menu` / `/ffaadmin gui`).

### GUI Features:
1. **Kit Settings**:
   * View all registered Neptune kits.
   * Enable/Disable FFA per kit.
   * Change worldgen reset settings.
   * Configure the reset intervals.
   * Manage spawn points (add, view, teleport to, or clear existing ones).
2. **Live Sessions**:
   * Monitor all active FFA arenas.
   * View current player counts per session.
   * Force manual resets or shut down specific sessions.
3. **Reload Config**:
   * Instantly reloads both `config.yml` and `kits.yml` and updates all active session rules without a server restart.

---

## 💬 Commands & Permissions

### Player Commands
* `/ffa` - Opens the interactive **FFA Kit Selector Menu** (allows joining any active arena).
* `/ffa join <kit>` - Directly join the FFA session for a specific kit.
* `/ffa leave` - Leave the current FFA arena and safely return to the lobby state.
* `/ffa list` - Display a list of all active sessions and their player counts.
* `/ffa stats [player]` - Display your detailed FFA statistics or another player's stats (requires permission for others).

### Admin Commands
* `/ffaadmin` - Opens the **FFA Admin GUI**.
* `/ffaadmin reload` - Reload configurations and reinitialize active sessions.
* `/ffaadmin reset <kit>` - Force an immediate arena reset and lobby kick for a specific kit's session.
* `/ffaadmin addspawn <kit>` - Add your current location to the spawn points list for a specific kit.

### Permissions
* `neptuneffa.admin` - Required to run `/ffaadmin` and access all admin functions.
* `neptuneffa.stats.others` - Required to view other players' stats via `/ffa stats <player>`.

---

## 📊 Scoreboard & Placeholders

NeptuneFFA registers custom Placeholders automatically with Neptune's scoreboard engine for players in the `"IN_FFA"` state. You can customize these lines in `config.yml`.

### Session Stats
* `<ffa_kit>` - Active kit name.
* `<ffa_arena>` - Active arena name.
* `<ffa_players>` - Active player count in the session.
* `<ffa_reset_timer>` - Formatted time remaining before the arena resets (e.g. `04:52`).
* `<ffa_session_kills>` - Kills in the current session.
* `<ffa_session_deaths>` - Deaths in the current session.
* `<ffa_session_streak>` - Current active killstreak.
* `<ffa_session_best_streak>` - Highest killstreak in the current session.
* `<ffa_session_kdr>` - Kill/Death ratio for the current session.
* `<ffa_time_in_session>` - Formatted duration spent in the current session.

### Lifetime Stats
* `<ffa_lifetime_kills>` - Total cumulative FFA kills.
* `<ffa_lifetime_deaths>` - Total cumulative FFA deaths.
* `<ffa_lifetime_best_streak>` - Best killstreak achieved across all sessions.
* `<ffa_lifetime_sessions>` - Total number of FFA sessions joined.
* `<ffa_kdr>` - Cumulative lifetime Kill/Death ratio.
* `<ffa_ping>` - Player's current connection latency.

### Leaderboard & Ranking Stats
* `<ffa_rank>` - The player's overall rank position based on lifetime kills.
* `<ffa_top_killer>` - Name of the player with the most lifetime kills.
* `<ffa_top_killer_kills>` - Number of kills the top killer has.
* `<ffa_kills_to_next_rank>` - Number of kills needed to pass the player ranked immediately ahead.

---

## 💻 Developer Info & Build Guide

NeptuneFFA depends on the local Neptune API. To build it from source:

### 1. Install Neptune API
If you have the local Neptune codebase, you must first compile and install it to your local Maven repository (`.m2` cache) so NeptuneFFA can resolve it:
```bash
# Navigate to the Neptune core/API directory
cd Neptune/API

# Compile and install the API
mvn clean install
```

### 2. Build NeptuneFFA
Once Neptune API is installed in your `.m2` repository, navigate to the NeptuneFFA folder and run:
```bash
# Navigate to NeptuneFFA
cd NeptuneFFA

# Package into a shading JAR
mvn clean package
```
This will compile the addon and output a shadowed jar under `target/NeptuneFFA-1.0.0.jar`.

# [Support Server](https://discord.gg/QFpe4j8bNq)
