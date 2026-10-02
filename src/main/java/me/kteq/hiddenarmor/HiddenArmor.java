package me.kteq.hiddenarmor;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.kteq.hiddenarmor.command.HiddenArmorTabCompleter;
import me.kteq.hiddenarmor.command.HiddenArmorCommand;
import me.kteq.hiddenarmor.command.ToggleArmorCommand;
import me.kteq.hiddenarmor.handler.ArmorPlaceholderHandler;
import me.kteq.hiddenarmor.handler.ArmorUpdateHandler;
import me.kteq.hiddenarmor.handler.MessageHandler;
import me.kteq.hiddenarmor.listener.packet.WindowItemsPacketListener;
import me.kteq.hiddenarmor.util.ConfigHolder;
import me.kteq.hiddenarmor.util.protocol.NmsItems;
import me.kteq.hiddenarmor.util.protocol.PacketIndexMapper;
import me.kteq.hiddenarmor.util.Metrics;
import me.kteq.hiddenarmor.listener.EntityToggleGlideListener;
import me.kteq.hiddenarmor.listener.GameModeListener;
import me.kteq.hiddenarmor.listener.PotionEffectListener;
import me.kteq.hiddenarmor.listener.InventoryShiftClickListener;
import me.kteq.hiddenarmor.listener.packet.EntityEquipmentPacketListener;
import me.kteq.hiddenarmor.listener.packet.EntityMetadataPacketListener;
import me.kteq.hiddenarmor.listener.packet.SetSlotPacketListener;
import me.kteq.hiddenarmor.manager.PacketStateCache;
import me.kteq.hiddenarmor.manager.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class HiddenArmor extends JavaPlugin {
    private PlayerManager playerManager;
    private PacketStateCache packetStateCache;
    private ArmorUpdateHandler armorUpdater;
    private ArmorPlaceholderHandler armorPlaceholderHandler;
    private MessageHandler messageHandler;

    private List<ConfigHolder> configHolders;

    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        // Default config file
        this.saveDefaultConfig();
        checkConfig();

        try {
            NmsItems.init(getServer());
        } catch (ReflectiveOperationException e) {
            getLogger().log(Level.SEVERE, "Cannot resolve the core's packet item conversion; disabling", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PacketIndexMapper packetIndexMapper = new PacketIndexMapper(this);

        // Instantiate members
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.messageHandler = new MessageHandler(this, "&c[&fHiddenArmor&c] &f");
        this.armorUpdater = new ArmorUpdateHandler(this, packetIndexMapper);
        this.packetStateCache = new PacketStateCache(this);
        this.playerManager = new PlayerManager(this);
        this.armorPlaceholderHandler = new ArmorPlaceholderHandler(this);

        // Enable commands
        new ToggleArmorCommand(this, "togglearmor")
                .setPermission("hiddenarmor")
                .setPermissionRequired(false);
        new HiddenArmorCommand(this, "hiddenarmor")
                .setPermission("hiddenarmor")
                .setPermissionRequired(false)
                .setTabCompleter(new HiddenArmorTabCompleter(this));

        // Register ProtocolLib packet listeners
        protocolManager.addPacketListener(new SetSlotPacketListener(this, packetIndexMapper));
        protocolManager.addPacketListener(new WindowItemsPacketListener(this, packetIndexMapper));
        protocolManager.addPacketListener(new EntityEquipmentPacketListener(this, packetIndexMapper));
        protocolManager.addPacketListener(new EntityMetadataPacketListener(this, packetIndexMapper));

        // Register event listeners
        new InventoryShiftClickListener(this);
        new GameModeListener(this);
        new PotionEffectListener(this);
        new EntityToggleGlideListener(this);

        //getCommand("hiddenarmor").setTabCompleter(new HiddenArmorTabCompleter(this));
        reloadConfig();

        // Metrics
        new Metrics(this, 14419);
    }

    @Override
    public void onDisable() {
        if (playerManager != null)
            playerManager.saveCurrentEnabledPlayers();
    }

    private void checkConfig() {
        reloadConfig();
        if(getConfig().getInt("config-version") >= getConfig().getDefaults().getInt("config-version"))
            return;
        getLogger().log(Level.WARNING, "Your HiddenArmor configuration file is outdated!");
        getLogger().log(Level.WARNING, "Please regenerate the 'config.yml' file when possible.");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (configHolders == null)
            configHolders = new ArrayList<>();
        configHolders.forEach(c -> c.loadConfig(getConfig()));
    }

    public void addConfigHolder(ConfigHolder configHolder) {
        configHolders.add(configHolder);
    }


    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public PacketStateCache getPacketStateCache() {
        return packetStateCache;
    }

    public ArmorUpdateHandler getArmorUpdater() {
        return armorUpdater;
    }

    public ArmorPlaceholderHandler getArmorPlaceholderHandler() {
        return armorPlaceholderHandler;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}
