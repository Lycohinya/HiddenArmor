package me.kteq.hiddenarmor.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorPlaceholderHandler;
import me.kteq.hiddenarmor.manager.PlayerManager;
import me.kteq.hiddenarmor.util.protocol.NmsItems;
import me.kteq.hiddenarmor.util.protocol.PacketFields;
import me.kteq.hiddenarmor.util.protocol.PacketIndexMapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class WindowItemsPacketListener extends PacketAdapter {
    private final PlayerManager playerManager;
    private final ArmorPlaceholderHandler placeholderHandler;

    private final int WINDOW_ID_INDEX;
    private final int STATE_ID_INDEX = 1;
    private final int ITEM_LIST_INDEX;
    private final int CARRIED_ITEM_INDEX = 0;

    private volatile Constructor<?> packetConstructor;

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

        // The packet behind WINDOW_ITEMS is a record, whose final fields ProtocolLib cannot set,
        // and on 26.3 rewriting a mirror no longer reaches the NMS stack (see NmsItems). So build
        // a new packet with armor slots 5-8 replaced and send that one instead.
        List<?> items = (List<?>) packet.getModifier().withType(List.class).read(ITEM_LIST_INDEX);
        if (items == null || items.size() < 9) return;

        List<Object> rewritten = new ArrayList<>(items);
        for (int i = 5; i < 9; i++) {
            Object nmsItem = rewritten.get(i);
            if (nmsItem != null) {
                ItemStack placeholder = placeholderHandler.buildItemPlaceholder(NmsItems.toBukkit(nmsItem));
                rewritten.set(i, NmsItems.toNms(placeholder));
            }
        }

        Object carried = packet.getModifier().withType(NmsItems.itemStackClass()).read(CARRIED_ITEM_INDEX);
        Object handle = packet.getHandle();
        try {
            Object rebuilt = constructor(handle.getClass()).newInstance(
                    packet.getIntegers().read(WINDOW_ID_INDEX),
                    packet.getIntegers().read(STATE_ID_INDEX),
                    rewritten,
                    carried);
            event.setPacket(new PacketContainer(event.getPacketType(), rebuilt));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot rebuild " + handle.getClass().getName(), e);
        }
    }

    /// The record's canonical constructor (int containerId, int stateId, List items, ItemStack carried).
    private Constructor<?> constructor(Class<?> packetClass) throws NoSuchMethodException {
        Constructor<?> c = packetConstructor;
        if (c == null) {
            c = packetClass.getConstructor(int.class, int.class, List.class, NmsItems.itemStackClass());
            packetConstructor = c;
        }
        return c;
    }
}
