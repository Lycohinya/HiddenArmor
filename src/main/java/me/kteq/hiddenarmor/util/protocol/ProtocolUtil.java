package me.kteq.hiddenarmor.util.protocol;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ProtocolUtil {

    /// Sends the packet to exactly the players the server is tracking this player for.
    /// This is the same set the server itself broadcasts entity updates to, so it respects
    /// per-player view distance instead of guessing with a world-wide distance scan.
    public static void broadcastPlayerPacket(ProtocolManager manager, PacketContainer packet, Player player) {
        for(Player viewer : player.getTrackedBy()){
            manager.sendServerPacket(viewer, packet);
        }
    }

    public enum ArmorType {
        HELMET(5), CHEST(6), LEGGS(7), BOOTS(8);

        private final int value;

        public static ArmorType getType(int value){
            switch (value) {
                case 5: return HELMET;
                case 6: return CHEST;
                case 7: return LEGGS;
                case 8: return BOOTS;
                default: return null;
            }
        }

        public int getValue(){
            return value;
        }

        ArmorType(int i){
            this.value = i;
        }
    }

    /// @return A **copy** of the armor piece in that slot, or AIR. The copy matters: the packet
    /// listeners rewrite the ItemStack they are handed, and a Bukkit ItemStack read straight out
    /// of a live inventory mirrors the server's real item.
    public static ItemStack getArmor(ArmorType type, PlayerInventory inv) {
        switch (type) {
            case HELMET: if(inv.getHelmet()!=null) return inv.getHelmet().clone();
                break;
            case CHEST: if(inv.getChestplate()!=null) return inv.getChestplate().clone();
                break;
            case LEGGS: if(inv.getLeggings()!=null) return inv.getLeggings().clone();
                break;
            case BOOTS: if(inv.getBoots()!=null) return inv.getBoots().clone();
                break;
        }
        return new ItemStack(Material.AIR);
    }
}
