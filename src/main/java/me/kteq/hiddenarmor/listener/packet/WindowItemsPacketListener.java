package me.kteq.hiddenarmor.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorPlaceholderHandler;
import me.kteq.hiddenarmor.manager.PlayerManager;
import me.kteq.hiddenarmor.util.protocol.PacketFields;
import me.kteq.hiddenarmor.util.protocol.PacketIndexMapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WindowItemsPacketListener extends PacketAdapter {
    private final PlayerManager playerManager;
    private final ArmorPlaceholderHandler placeholderHandler;

    private final int WINDOW_ID_INDEX;
    private final int ITEM_LIST_INDEX;

    public WindowItemsPacketListener(HiddenArmor plugin, PacketIndexMapper indexMapper) {
        super(plugin, PacketType.Play.Server.WINDOW_ITEMS);
        this.playerManager = plugin.getPlayerManager();
        this.placeholderHandler = plugin.getArmorPlaceholderHandler();

        this.WINDOW_ID_INDEX = indexMapper.get(PacketFields.WINDOW_ITEMS_$WINDOW_ID);
        this.ITEM_LIST_INDEX = indexMapper.get(PacketFields.WINDOW_ITEMS_$ITEM_LIST);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        // Folia (DEC-64 / D-49 plan B): connection thread - use the cached UUID variant, never
        // live entity state.
        if (playerManager.isArmorVisible(player.getUniqueId())) return;

        PacketContainer packet = event.getPacket();
        if (!packet.getIntegers().read(WINDOW_ID_INDEX).equals(0)) return;

        // No write() back: the packet behind WINDOW_ITEMS is a record, whose final `items` field
        // ProtocolLib cannot set. It works anyway because the ItemStacks read out are CraftItemStack
        // mirrors of the packet's own NMS stacks, and buildItemPlaceholder rewrites them in place.
        List<ItemStack> items = packet.getItemListModifier().read(ITEM_LIST_INDEX);
        for (int i = 5; i < 9; i++) {
            ItemStack itemStack = items.get(i);
            if (itemStack != null) {
                ItemStack placeholder = placeholderHandler.buildItemPlaceholder(itemStack);
                items.set(i, placeholder);
            }
        }
    }
}
