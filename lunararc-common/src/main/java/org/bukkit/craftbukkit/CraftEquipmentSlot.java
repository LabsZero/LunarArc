package org.bukkit.craftbukkit;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;

public final class CraftEquipmentSlot {
    private static final java.util.Map<String, EquipmentSlotGroup> BUKKIT_GROUPS = loadBukkitGroups();

    private CraftEquipmentSlot() {}

    public static EquipmentSlot getHand(net.minecraft.world.InteractionHand hand) {
        if (hand == null) throw new IllegalArgumentException("hand cannot be null");
        return hand == net.minecraft.world.InteractionHand.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
    }

    public static net.minecraft.world.entity.EquipmentSlot getNMS(EquipmentSlot slot) {
        if (slot == null) throw new IllegalArgumentException("slot cannot be null");
        return switch (slot) {
            case HAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFF_HAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case BODY -> net.minecraft.world.entity.EquipmentSlot.BODY;
        };
    }

    public static EquipmentSlot getSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        if (slot == null) throw new IllegalArgumentException("slot cannot be null");
        return switch (slot) {
            case MAINHAND -> EquipmentSlot.HAND;
            case OFFHAND -> EquipmentSlot.OFF_HAND;
            case FEET -> EquipmentSlot.FEET;
            case LEGS -> EquipmentSlot.LEGS;
            case CHEST -> EquipmentSlot.CHEST;
            case HEAD -> EquipmentSlot.HEAD;
            case BODY -> EquipmentSlot.BODY;
        };
    }

    public static net.minecraft.world.entity.EquipmentSlotGroup getNMSGroup(EquipmentSlotGroup group) {
        if (group == null) throw new IllegalArgumentException("group cannot be null");
        String name = group.toString();
        for (net.minecraft.world.entity.EquipmentSlotGroup candidate : net.minecraft.world.entity.EquipmentSlotGroup.values()) {
            if (candidate.getSerializedName().equals(name)) return candidate;
        }
        throw new IllegalArgumentException("Unknown equipment slot group: " + group);
    }

    public static EquipmentSlotGroup getSlot(net.minecraft.world.entity.EquipmentSlotGroup group) {
        if (group == null) throw new IllegalArgumentException("group cannot be null");
        EquipmentSlotGroup result = BUKKIT_GROUPS.get(group.getSerializedName());
        if (result == null) throw new IllegalArgumentException("Unknown equipment slot group: " + group.getSerializedName());
        return result;
    }

    private static java.util.Map<String, EquipmentSlotGroup> loadBukkitGroups() {
        java.util.HashMap<String, EquipmentSlotGroup> groups = new java.util.HashMap<>();
        for (java.lang.reflect.Field field : EquipmentSlotGroup.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType() != EquipmentSlotGroup.class) continue;
            try {
                EquipmentSlotGroup group = (EquipmentSlotGroup) field.get(null);
                if (group != null) groups.put(group.toString(), group);
            } catch (IllegalAccessException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
        return java.util.Map.copyOf(groups);
    }
}
