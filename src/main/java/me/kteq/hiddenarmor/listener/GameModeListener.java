package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.manager.PlayerManager;
import me.kteq.hiddenarmor.util.EventUtil;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class GameModeListener implements Listener {
    HiddenArmor plugin;
    PlayerManager hiddenArmorManager;

    public GameModeListener(HiddenArmor plugin){
        EventUtil.register(this, plugin);

        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getPlayerManager();
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event){
        if(!hiddenArmorManager.isEnabled(event.getPlayer())) return;
        if(event.getNewGameMode().equals(GameMode.CREATIVE)) {
            hiddenArmorManager.disablePlayer(event.getPlayer(), false);
            plugin.getArmorUpdater().updatePlayer(event.getPlayer());
        }

        // Folia: only touches this one player -> their own EntityScheduler.
        event.getPlayer().getScheduler().runDelayed(plugin, task -> {
            if (event.getNewGameMode().equals(GameMode.CREATIVE)) {
                hiddenArmorManager.enablePlayer(event.getPlayer(), false);
            } else {
                plugin.getArmorUpdater().updatePlayer(event.getPlayer());
            }
        }, null, 1L);
    }


}
