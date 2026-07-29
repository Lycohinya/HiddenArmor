package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdateHandler;
import me.kteq.hiddenarmor.manager.PlayerManager;
import me.kteq.hiddenarmor.util.EventUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class EntityToggleGlideListener implements Listener {
    HiddenArmor plugin;
    PlayerManager playerManager;
    ArmorUpdateHandler armorUpdater;

    public EntityToggleGlideListener(HiddenArmor plugin){
        EventUtil.register(this, plugin);

        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.armorUpdater = plugin.getArmorUpdater();
    }

    @EventHandler
    public void onPlayerToggleGlide(EntityToggleGlideEvent e){
        if(!(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        if(playerManager.isArmorVisible(player)) return;

        // Folia: this only touches this one player (their inventory + the packets sent about them),
        // so it belongs on that player's EntityScheduler, which follows them across regions.
        // retired == null: if they log out during the 1-tick delay there is nothing to update.
        player.getScheduler().runDelayed(plugin, task -> armorUpdater.updatePlayer(player), null, 1L);
    }
}
