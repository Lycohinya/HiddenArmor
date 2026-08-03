package me.kteq.hiddenarmor.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.manager.PacketStateCache;
import me.kteq.hiddenarmor.util.protocol.PacketFields;
import me.kteq.hiddenarmor.util.protocol.PacketIndexMapper;

import java.util.List;
import java.util.UUID;

/// Folia (DEC-64 / D-49 plan B): keeps PacketStateCache's shared-flags byte (gliding / invisible)
/// in sync with what the clients are actually told, so the equipment path never has to read live
/// entity state. Read-only - this listener never modifies the packet.
///
/// Deliberately no isAnyoneHiding() early exit: skipping updates while nobody hides would leave
/// stale flags behind for the moment hiding is turned on. The per-packet cost without a match is
/// one int read plus one map lookup.
public class EntityMetadataPacketListener extends PacketAdapter {
    private final PacketStateCache stateCache;

    private final int ENTITY_ID_INDEX;
    private final int DATA_VALUE_LIST_INDEX;

    public EntityMetadataPacketListener(HiddenArmor plugin, PacketIndexMapper indexMapper) {
        super(plugin, PacketType.Play.Server.ENTITY_METADATA);
        this.stateCache = plugin.getPacketStateCache();

        this.ENTITY_ID_INDEX = indexMapper.get(PacketFields.ENTITY_METADATA_$ENTITY_ID);
        this.DATA_VALUE_LIST_INDEX = indexMapper.get(PacketFields.ENTITY_METADATA_$DATA_VALUE_LIST);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        UUID uuid = stateCache.trackedPlayer(packet.getIntegers().read(ENTITY_ID_INDEX));
        if (uuid == null) return;

        // Metadata packets carry only dirty entries; the shared-flags byte is index 0 and is
        // present only when it changed (or in the full metadata sent on spawn/tracking start).
        List<WrappedDataValue> values = packet.getDataValueCollectionModifier().read(DATA_VALUE_LIST_INDEX);
        if (values == null) return;
        for (WrappedDataValue value : values) {
            if (value.getIndex() != 0) continue;
            Object raw = value.getValue();
            if (raw instanceof Byte) {
                stateCache.updateFlags(uuid, (Byte) raw);
            }
            return;
        }
    }
}
