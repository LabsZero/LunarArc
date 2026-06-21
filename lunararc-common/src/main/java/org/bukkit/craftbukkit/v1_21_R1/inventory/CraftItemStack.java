package org.bukkit.craftbukkit.v1_21_R1.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftItemStack extends ItemStack {
    private final net.minecraft.world.item.ItemStack handle;

    public CraftItemStack(net.minecraft.world.item.ItemStack handle) {
        this.handle = handle;
    }

    @Override
    public @NotNull Material getType() {
        if (handle == null || handle.isEmpty()) return Material.AIR;
        try {
            net.minecraft.resources.ResourceLocation key =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(handle.getItem());
            return Material.valueOf(key.getPath().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception ignored) {
            return Material.AIR;
        }
    }

    @Override
    public int getAmount() {
        return handle != null ? handle.getCount() : 0;
    }

    public static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return new CraftItemStack(stack.copy());
    }

    public static net.minecraft.world.item.ItemStack asNMSCopy(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.parse(stack.getType().getKey().toString())
        );
        
        if (item == null) return net.minecraft.world.item.ItemStack.EMPTY;
        
        return new net.minecraft.world.item.ItemStack(item, stack.getAmount());
    }

    public static CraftItemStack asCraftMirror(net.minecraft.world.item.ItemStack stack) {
        return new CraftItemStack(stack);
    }

    public static CraftItemStack asCraftCopy(ItemStack stack) {
        if (stack instanceof CraftItemStack) {
            return new CraftItemStack(((CraftItemStack) stack).handle.copy());
        }
        return new CraftItemStack(asNMSCopy(stack));
    }

    public net.minecraft.world.item.ItemStack getHandle() {
        return handle;
    }

    @Override
    public boolean hasItemMeta() {
        if (handle == null || handle.isEmpty()) return false;
        CraftItemMeta meta = new CraftItemMeta(handle);
        return meta.hasDisplayName() || meta.hasLore() || meta.hasEnchants()
            || meta.isUnbreakable() || meta.hasCustomModelData();
    }

    @Override
    public @Nullable ItemMeta getItemMeta() {
        if (handle == null || handle.isEmpty()) return new CraftItemMeta();
        return new CraftItemMeta(handle);
    }

    @Override
    public boolean setItemMeta(@Nullable ItemMeta meta) {
        if (handle == null || handle.isEmpty()) return false;
        if (meta instanceof CraftItemMeta craftMeta) {
            craftMeta.applyToNms(handle);
        } else if (meta != null) {
            // Convert generic ItemMeta to CraftItemMeta
            CraftItemMeta craft = new CraftItemMeta();
            if (meta.hasDisplayName()) craft.setDisplayName(meta.getDisplayName());
            if (meta.hasLore()) craft.setLore(meta.getLore());
            craft.setUnbreakable(meta.isUnbreakable());
            craft.applyToNms(handle);
        } else {
            // null meta clears all
            handle.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
            handle.remove(net.minecraft.core.component.DataComponents.LORE);
            handle.remove(net.minecraft.core.component.DataComponents.UNBREAKABLE);
            handle.remove(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        }
        return true;
    }

}
