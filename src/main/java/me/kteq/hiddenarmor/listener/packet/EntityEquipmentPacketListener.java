package me.kteq.hiddenarmor.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.manager.PacketStateCache;
import me.kteq.hiddenarmor.util.ConfigHolder;
import me.kteq.hiddenarmor.util.protocol.NmsItems;
import me.kteq.hiddenarmor.util.protocol.PacketFields;
import me.kteq.hiddenarmor.util.protocol.PacketIndexMapper;
import me.kteq.hiddenarmor.manager.PlayerManager;
import me.kteq.hiddenarmor.util.ItemUtil;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntityEquipmentPacketListener extends PacketAdapter implements ConfigHolder {
    private final PlayerManager hiddenArmorManager;
    private final PacketStateCache stateCache;

    private boolean ignoreLeatherArmor;
    private boolean ignoreTurtleHelmet;
    private boolean ignoreElytra;

    private final int ENTITY_ID_INDEX;
    private final int SLOT_ITEM_PAIR_LIST_INDEX;

    public EntityEquipmentPacketListener(HiddenArmor plugin, PacketIndexMapper indexMapper) {
        super(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT);
        plugin.addConfigHolder(this);

        this.hiddenArmorManager = plugin.getPlayerManager();
        this.stateCache = plugin.getPacketStateCache();

        this.ENTITY_ID_INDEX = indexMapper.get(PacketFields.ENTITY_EQUIPMENT_$ENTITY_ID);
        this.SLOT_ITEM_PAIR_LIST_INDEX = indexMapper.get(PacketFields.ENTITY_EQUIPMENT_$SLOT_ITEM_PAIR_LIST);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        // Every equipment packet of every mob passes through here. Bail before the lookup when
        // nobody on the server is hiding anything.
        if(!hiddenArmorManager.isAnyoneHiding()) return;

        PacketContainer packet = event.getPacket();

        // Folia (DEC-64 / D-49 plan B): this thread is the *receiver's* connection thread, which
        // does not necessarily own the player the packet describes - so everything below reads
        // only PacketStateCache (what the clients have been told), never live entity state, and
        // no region-ownership guard is needed.
        UUID packetPlayer = stateCache.trackedPlayer(packet.getIntegers().read(ENTITY_ID_INDEX));
        if (packetPlayer == null) return; // mobs, NPCs, unknown ids -> vanilla

        if (hiddenArmorManager.isArmorVisible(packetPlayer)) return;

        // Raw NMS List<Pair<EquipmentSlot, ItemStack>>, not getSlotStackPairLists(): see NmsItems.
        StructureModifier<Object> lists = packet.getModifier().withType(List.class);
        List<?> pairList = (List<?>) lists.read(SLOT_ITEM_PAIR_LIST_INDEX);
        if (pairList == null) return;

        List<Object> rewritten = new ArrayList<>(pairList.size());
        for (Object pair : pairList) {
            ItemStack item = NmsItems.toBukkit(NmsItems.pairSecond(pair));
            ItemStack replacement = null;
            if (item.getType().equals(Material.ELYTRA)
                    && ((stateCache.isGliding(packetPlayer) || ignoreElytra)
                    && !stateCache.isInvisible(packetPlayer)))
            {
                replacement = new ItemStack(Material.ELYTRA);
            }
            else if (!shouldIgnore(item))
                replacement = new ItemStack(Material.AIR);

            rewritten.add(replacement == null
                    ? pair
                    : NmsItems.pair(NmsItems.pairFirst(pair), NmsItems.toNms(replacement)));
        }
        lists.write(SLOT_ITEM_PAIR_LIST_INDEX, rewritten);
    }

    private boolean shouldIgnore(ItemStack itemStack) {
        Material material = itemStack.getType();

        return (ignoreLeatherArmor && material.toString().startsWith("LEATHER")) ||
                (ignoreTurtleHelmet && material.equals(Material.TURTLE_HELMET)) ||
                (!ItemUtil.isArmor(itemStack) && !itemStack.getType().equals(Material.ELYTRA)) ||
                (ignoreElytra && itemStack.getType().equals(Material.ELYTRA));
    }

    @Override
    public void loadConfig(FileConfiguration config) {
        this.ignoreLeatherArmor = config.getBoolean("ignore.leather-armor");
        this.ignoreTurtleHelmet = config.getBoolean("ignore.turtle-helmet");
        this.ignoreElytra = config.getBoolean("ignore.elytra");
    }
}
