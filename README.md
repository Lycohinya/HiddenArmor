> **This is a fork.** The original Hidden Armor was written by **[Kteq](https://github.com/Kteq1)** and lives at
> **[Kteq1/HiddenArmor](https://github.com/Kteq1/HiddenArmor)**. All credit for the plugin goes to them; this fork only
> updates it for Paper 26.2 and trims some hot-path work. Licensed GPL-3.0, same as upstream.
>
> See [Changes in this fork](#changes-in-this-fork).

This plugin requires **[ProtocolLib](https://github.com/dmulloy2/ProtocolLib)** to work! (at least 5.3.0)

# Hidden Armor
### Requirements:
- Paper 26.2 (this fork uses `paper-api` and Paper-only APIs, so it will not run on Spigot)
- Java 25
- ProtocolLib 5.3.0 or newer

### Download:
Upstream builds: [Modrinth](https://modrinth.com/plugin/hidden-armor) or [Spigot](https://www.spigotmc.org/resources/hidden-armor.100374).
This fork is not published anywhere; build it from source with `mvn package`.

### Features:
- Toggle armor visibility by using the /togglearmor command
- Armor can always be invisible when drinking an invisibility potion (Disabled by default, enable it in the config.yml)
- Some armor pieces can be ignored and not hidden by the plugin (Only leather armor is ignored by default, configurable)

Armor is never hidden while a player is in creative mode. A creative client sends its inventory back to the
server, so a placeholder item sitting in an armor slot can overwrite the real armor.


### Commands and Permissions:
- /hiddenarmor <hide/show/toggle> - Changes the player's own armor visibility (hiddenarmor.toggle - Allowed to all players by default, you can change this in the config file)
- /hiddenarmor <hide/show/toggle> <player> - Changes other player's armor visibility (hiddenarmor.toggle.other)
- /hiddenarmor reload - Reloads the configuration (hiddenarmor.reload)
- /hiddenarmor help - Shows all available commands for the player
- /ha and /armor - Aliases for /hiddenarmor

### Metrics:
This plugin collects and sends anonymous data to bStats. This helps me to know how many servers and players are using it. You can disable this on the bStats config inside your plugins folder.

### Dependencies:
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib)

### Changes in this fork:
Fixes:
- Armor tiers with no placeholder mapping were left fully visible. Copper armor, added in the 26.x line, hit this
  and hid nothing at all. Unmapped tiers now fall back to a plain button instead of silently doing nothing.
- `/togglearmor` and `/hiddenarmor toggle` threw `ClassCastException` for any non-player sender that was not the
  console, so RCON and command blocks crashed the command.
- Toggling in creative mode reported "Hidden" and then did nothing. It now says why.
- Swapped field indices in the entity-equipment packet listener.

Performance:
- Equipment updates go to `Entity#getTrackedBy()`, the set the server already broadcasts entity updates to,
  instead of scanning every online player and measuring distance to each.
- The invisibility listener only reacts to the invisibility effect. It used to rebroadcast a player's equipment
  on every potion effect, including beacon reapplications.
- The packet listeners skip their work entirely when nobody on the server is hiding armor.
- The bundled fallback locale is parsed once rather than on every placeholder lookup.

Build: targets `paper-api` 26.2 and Java 25. `WordUtils` was dropped, since `paper-api` does not ship commons-lang2.

### Additional Info:
From the original author: *"This is my first finished plugin, I initially made it for my private server with
friends, and then decided to share it."* — [Kteq](https://github.com/Kteq1)
