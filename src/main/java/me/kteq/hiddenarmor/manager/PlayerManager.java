package me.kteq.hiddenarmor.manager;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdateHandler;
import me.kteq.hiddenarmor.handler.MessageHandler;
import me.kteq.hiddenarmor.util.ConfigHolder;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class PlayerManager implements ConfigHolder {
    private final HiddenArmor plugin;
    private final ArmorUpdateHandler armorUpdater;
    private final MessageHandler messageHandler;


    private File enabledPlayersFile = null;
    private FileConfiguration enabledPlayersConfig;

    private boolean invisibleAlwaysHideGear;

    /// Folia: read on **every** equipment/inventory packet from many region threads in parallel
    /// (see the packet listeners), written from commands and gamemode changes. A plain HashSet
    /// resizing under a concurrent read can spin or return garbage, so this has to be concurrent.
    /// Identical behaviour single-threaded, so `main` (Paper) semantics are unchanged.
    private Set<UUID> enabledPlayersUUID = ConcurrentHashMap.newKeySet();


    public PlayerManager(HiddenArmor plugin) {
        this.plugin = plugin;
        plugin.addConfigHolder(this);
        this.armorUpdater = plugin.getArmorUpdater();
        this.messageHandler = plugin.getMessageHandler();
        loadEnabledPlayers();
    }

    public void togglePlayer(Player player, boolean inform) {
        if (isEnabled(player)) {
            disablePlayer(player, inform);
        } else {
            enablePlayer(player, inform);
        }
    }

    public void enablePlayer(Player player, boolean inform) {
        if (isEnabled(player)) return;
        if (inform) {
            Map<String, String> placeholderMap = new HashMap<>();
            placeholderMap.put("visibility", "%visibility-hidden%");
            messageHandler.message(ChatMessageType.ACTION_BAR, player, "%armor-visibility%", false, placeholderMap);

            // Creative players never get their armor hidden (see isArmorVisible), so reporting
            // "Hidden" and nothing else reads as the plugin being broken.
            if (player.getGameMode().equals(GameMode.CREATIVE))
                messageHandler.message(player, "%creative-mode-warning%");
        }

        this.enabledPlayersUUID.add(player.getUniqueId());
        armorUpdater.updatePlayer(player);
    }

    public void disablePlayer(Player player, boolean inform) {
        if (!isEnabled(player)) return;
        if (inform) {
            Map<String, String> placeholderMap = new HashMap<>();
            placeholderMap.put("visibility", "%visibility-shown%");
            messageHandler.message(ChatMessageType.ACTION_BAR, player, "%armor-visibility%", false, placeholderMap);
        }

        enabledPlayersUUID.remove(player.getUniqueId());
        armorUpdater.updatePlayer(player);
    }

    public boolean isEnabled(Player player) {
        return this.enabledPlayersUUID.contains(player.getUniqueId());
    }

    /// @return **false** when no player on the server can possibly be hiding armor.
    /// Lets the packet listeners skip their work entirely, which matters because
    /// they run on every equipment and inventory packet the server sends.
    public boolean isAnyoneHiding() {
        return !enabledPlayersUUID.isEmpty() || invisibleAlwaysHideGear;
    }

    /// Only safe on threads that own the player - reads live entity state. The packet listeners
    /// must use the UUID overload below instead.
    public boolean isArmorVisible(Player player) {
        boolean hidden = isEnabled(player);
        boolean invisible = player.isInvisible();

        if (player.getGameMode().equals(GameMode.CREATIVE) || (invisible && !invisibleAlwaysHideGear))
            hidden = false;
        if (invisible && invisibleAlwaysHideGear)
            hidden = true;

        return !hidden;
    }

    /// Folia (DEC-64 / D-49 plan B): packet-path variant. Same decision as above, but reads only
    /// PacketStateCache ("what the clients have been told"), never live entity state, so it is
    /// safe on any connection thread. Unknown state (never seeded) counts as visible, i.e. the
    /// packet is left alone - vanilla behaviour, never corrupt data.
    public boolean isArmorVisible(UUID uuid) {
        PacketStateCache cache = plugin.getPacketStateCache();
        GameMode gameMode = cache.gameMode(uuid);
        if (gameMode == null) return true;

        boolean hidden = enabledPlayersUUID.contains(uuid);
        boolean invisible = cache.isInvisible(uuid);

        if (gameMode.equals(GameMode.CREATIVE) || (invisible && !invisibleAlwaysHideGear))
            hidden = false;
        if (invisible && invisibleAlwaysHideGear)
            hidden = true;

        return !hidden;
    }

    public void saveCurrentEnabledPlayers() {
        List<String> enabledUUIDs = this.enabledPlayersUUID.stream().map(UUID::toString).collect(Collectors.toList());

        enabledPlayersConfig.set("enabled-players", enabledUUIDs);
        try {
            enabledPlayersConfig.save(enabledPlayersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save enabled players to " + enabledPlayersFile, e);
        }
    }

    private void loadEnabledPlayers() {
        loadEnabledPlayersConfig();
        Set<UUID> loaded = ConcurrentHashMap.newKeySet();
        enabledPlayersConfig.getStringList("enabled-players").stream().map(UUID::fromString).forEach(loaded::add);
        this.enabledPlayersUUID = loaded;
    }

    private void loadEnabledPlayersConfig() {
        enabledPlayersFile = new File(plugin.getDataFolder(), "enabled-players.yml");
        if (!enabledPlayersFile.exists()) {
            enabledPlayersFile.getParentFile().mkdirs();
            plugin.saveResource("enabled-players.yml", false);
        }

        enabledPlayersConfig = YamlConfiguration.loadConfiguration(enabledPlayersFile);
    }

    @Override
    public void loadConfig(FileConfiguration config) {
        this.invisibleAlwaysHideGear = config.getBoolean("invisibility-potion.always-hide-gear");
    }
}
