package me.kteq.hiddenarmor.manager;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.util.EventUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/// Folia (DEC-64 / D-49 plan B): outgoing packets about player X are written on the *receiver's*
/// connection thread, which does not necessarily own X, so the packet listeners must not read live
/// entity state. Everything they need is already present in the packet flow (the shared-flags byte
/// of ENTITY_METADATA) and in events that run on X's own region thread (join/respawn/world-change/
/// gamemode). This cache records "what the clients have been told" and the packet path reads only
/// from here - which is also the semantically correct reference frame for a display-only plugin.
///
/// All maps are keyed by online players only and cleaned on quit, so their size is bounded by the
/// online player count. ENTITY_DESTROY is deliberately NOT a removal trigger: it is per-viewer
/// (one viewer unloading X must not drop the shared entry while X is still online).
public class PacketStateCache implements Listener {

    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_GLIDING = (byte) 0x80;

    private final Map<Integer, UUID> entityIdToPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, GameMode> gameModes = new ConcurrentHashMap<>();
    /// Last shared-flags byte (metadata index 0) told to clients about this player.
    private final Map<UUID, Byte> sharedFlags = new ConcurrentHashMap<>();

    public PacketStateCache(HiddenArmor plugin) {
        EventUtil.register(this, plugin);
        // Players already online when the plugin enables (late enable, reload) never fire
        // PlayerJoinEvent. The live-state seed reads must happen on each player's own region
        // thread, hence per-player EntityScheduler tasks. retired == null: a player gone before
        // the task runs simply stays unseeded, same as never having joined.
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.getScheduler().run(plugin, task -> seed(player), null);
        }
    }

    /// Only called from contexts that own the player (their own events / EntityScheduler).
    private void seed(Player player) {
        entityIdToPlayer.put(player.getEntityId(), player.getUniqueId());
        gameModes.put(player.getUniqueId(), player.getGameMode());
        byte flags = 0;
        if (player.isInvisible()) flags |= FLAG_INVISIBLE;
        if (player.isGliding()) flags |= FLAG_GLIDING;
        sharedFlags.put(player.getUniqueId(), flags);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        seed(event.getPlayer());
    }

    // Player entity ids survive respawn/world change on Paper, but that is an implementation
    // detail - re-seeding on both is cheap and keeps the mapping correct if it ever changes.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        seed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        seed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        gameModes.put(event.getPlayer().getUniqueId(), event.getNewGameMode());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        entityIdToPlayer.remove(event.getPlayer().getEntityId());
        gameModes.remove(event.getPlayer().getUniqueId());
        sharedFlags.remove(event.getPlayer().getUniqueId());
    }

    /// @return the online player this entity id belongs to, or null (mobs, NPCs, unknown).
    public UUID trackedPlayer(int entityId) {
        return entityIdToPlayer.get(entityId);
    }

    /// Called by the ENTITY_METADATA listener from any connection thread.
    public void updateFlags(UUID uuid, byte flags) {
        sharedFlags.put(uuid, flags);
    }

    public boolean isGliding(UUID uuid) {
        Byte flags = sharedFlags.get(uuid);
        return flags != null && (flags & FLAG_GLIDING) != 0;
    }

    public boolean isInvisible(UUID uuid) {
        Byte flags = sharedFlags.get(uuid);
        return flags != null && (flags & FLAG_INVISIBLE) != 0;
    }

    /// @return null when unknown (never seeded); callers must then leave the packet alone.
    public GameMode gameMode(UUID uuid) {
        return gameModes.get(uuid);
    }
}
