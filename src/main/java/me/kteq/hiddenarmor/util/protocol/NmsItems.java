package me.kteq.hiddenarmor.util.protocol;

import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/// Packet items without ProtocolLib's item converter.
///
/// ProtocolLib 583353e reads packet items through CraftItemStack.asCraftMirror(nms ItemStack),
/// which 26.3 removed, so every getItemModifier()/getItemListModifier()/getSlotStackPairLists()
/// read throws. The packet listeners therefore read and write the raw NMS fields and convert with
/// the core's own asNMSCopy/asBukkitCopy, which exist on 26.2 (asBukkitCopy(ItemStack)) and 26.3
/// (asBukkitCopy(ItemInstance)). Conversions are copies on purpose: on 26.3 CraftItemStack#setType
/// replaces its handle, so mutating a mirror no longer reaches the packet.
public final class NmsItems {
    private static Class<?> itemStackClass;
    private static Class<?> equipmentSlotClass;
    private static MethodHandle asNmsCopy;
    private static MethodHandle asBukkitCopy;
    private static MethodHandle pairOf;
    private static MethodHandle pairFirst;
    private static MethodHandle pairSecond;

    private NmsItems() {}

    /// Resolves everything up front so a missing method disables the plugin at enable time
    /// instead of throwing on every packet.
    public static void init(Server server) throws ReflectiveOperationException {
        ClassLoader loader = server.getClass().getClassLoader();
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        Class<?> craftItemStack = Class.forName(
                server.getClass().getPackageName() + ".inventory.CraftItemStack", true, loader);
        Method nmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
        itemStackClass = nmsCopy.getReturnType();
        asNmsCopy = lookup.unreflect(nmsCopy);

        Method bukkitCopy = null;
        for (Method m : craftItemStack.getMethods()) {
            if (m.getName().equals("asBukkitCopy") && Modifier.isStatic(m.getModifiers())
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(itemStackClass)) {
                bukkitCopy = m;
                break;
            }
        }
        if (bukkitCopy == null)
            throw new NoSuchMethodException(craftItemStack.getName() + ".asBukkitCopy(" + itemStackClass.getName() + ")");
        asBukkitCopy = lookup.unreflect(bukkitCopy)
                .asType(MethodType.methodType(ItemStack.class, Object.class));

        equipmentSlotClass = Class.forName("net.minecraft.world.entity.EquipmentSlot", true, loader);
        Class<?> pair = Class.forName("com.mojang.datafixers.util.Pair", true, loader);
        pairOf = lookup.findStatic(pair, "of", MethodType.methodType(pair, Object.class, Object.class));
        pairFirst = lookup.findVirtual(pair, "getFirst", MethodType.methodType(Object.class));
        pairSecond = lookup.findVirtual(pair, "getSecond", MethodType.methodType(Object.class));
    }

    /// net.minecraft.world.item.ItemStack - the field type to pass to StructureModifier#withType.
    public static Class<?> itemStackClass() {
        return itemStackClass;
    }

    public static Object toNms(ItemStack item) {
        try {
            return asNmsCopy.invoke(item);
        } catch (Throwable t) {
            throw new IllegalStateException("CraftItemStack.asNMSCopy failed", t);
        }
    }

    public static ItemStack toBukkit(Object nmsItem) {
        try {
            return (ItemStack) asBukkitCopy.invoke(nmsItem);
        } catch (Throwable t) {
            throw new IllegalStateException("CraftItemStack.asBukkitCopy failed", t);
        }
    }

    /// @param slot a net.minecraft.world.entity.EquipmentSlot constant name (HEAD, CHEST, ...)
    public static Object equipmentSlot(String slot) {
        return enumConstant(equipmentSlotClass, slot);
    }

    public static Object pair(Object first, Object second) {
        try {
            return pairOf.invoke(first, second);
        } catch (Throwable t) {
            throw new IllegalStateException("Pair.of failed", t);
        }
    }

    public static Object pairFirst(Object pair) {
        try {
            return pairFirst.invoke(pair);
        } catch (Throwable t) {
            throw new IllegalStateException("Pair.getFirst failed", t);
        }
    }

    public static Object pairSecond(Object pair) {
        try {
            return pairSecond.invoke(pair);
        } catch (Throwable t) {
            throw new IllegalStateException("Pair.getSecond failed", t);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> enumClass, String name) {
        return Enum.valueOf((Class) enumClass, name);
    }
}
