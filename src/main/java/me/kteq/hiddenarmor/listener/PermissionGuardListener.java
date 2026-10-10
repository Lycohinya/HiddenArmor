package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.util.EventUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 隱藏狀態會存進 enabled-players.yml,封包層不查權限;權限只有在 /togglearmor 時才檢查。
 * 所以特典被收回(例如 2026-10-10 從弦月席移到滿月席/霞染席以上)後,已經開著隱藏的玩家
 * 會永遠隱藏、而且沒權限切回來。登入時發現「開著但沒有切換權限」就自動恢復顯示。
 * 只改外觀狀態,不碰任何真實裝備。
 *
 * Folia: 只動這一位玩家,走他自己的 EntityScheduler。
 */
public class PermissionGuardListener implements Listener {
    private final HiddenArmor plugin;

    public PermissionGuardListener(HiddenArmor plugin) {
        EventUtil.register(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getPlayerManager().isEnabled(player)) return;
        player.getScheduler().runDelayed(plugin, task -> {
            if (!plugin.getPlayerManager().isEnabled(player)) return;
            boolean allowed = player.isOp()
                    || player.hasPermission("hiddenarmor.toggle")
                    || plugin.getConfig().getBoolean("default-permissions.toggle");
            if (!allowed) plugin.getPlayerManager().disablePlayer(player, false);
        }, null, 40L);
    }
}
