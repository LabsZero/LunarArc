package org.bukkit.craftbukkit.v1_21_R1.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftItemStack extends ItemStack {
    public net.minecraft.world.item.ItemStack handle;

    public CraftItemStack(net.minecraft.world.item.ItemStack handle) {
        this.handle = handle;
    }

    @Override
    public @NotNull Material getType() {
        if (handle == null || handle.isEmpty()) return Material.AIR;
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(handle.getItem());
        if (key == null) return Material.AIR;

        Material material = Material.matchMaterial(key.toString());
        if (material != null) return material;

        material = Material.matchMaterial(key.getPath());
        return material != null ? material : Material.AIR;
    }

    @Override
    @Deprecated
    public void setType(@NotNull Material type) {
        if (type == null) throw new IllegalArgumentException("Material cannot be null");
        if (getType() == type) return;
        if (type == Material.AIR) {
            handle = null;
            return;
        }
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(type.getKey().toString()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            throw new IllegalArgumentException(type + " is not an item");
        }
        if (handle == null || handle.isEmpty()) {
            handle = new net.minecraft.world.item.ItemStack(item, 1);
        } else {
            // 1.21.1 exposes ItemStack#setItem specifically for CraftBukkit's
            // legacy mutable type API. Existing components are retained and
            // then normal ItemMeta validation can reconcile type-specific data.
            handle.setItem(item);
        }
    }

    @Override
    public @NotNull ItemStack withType(@NotNull Material type) {
        if (type == null) throw new IllegalArgumentException("Material cannot be null");
        if (type == Material.AIR) return new CraftItemStack(net.minecraft.world.item.ItemStack.EMPTY);
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(type.getKey().toString()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException(type + " is not an item");
        net.minecraft.world.item.ItemStack replacement = new net.minecraft.world.item.ItemStack(item, Math.max(1, getAmount()));
        if (handle != null && !handle.isEmpty()) {
            // Paper 1.21.1 preserves the exact component patch here. This also
            // keeps specialist/modded components that ItemMeta does not yet know.
            replacement.applyComponents(handle.getComponentsPatch());
        }
        CraftItemStack result = new CraftItemStack(replacement);
        // Re-apply through ItemMeta once to let type-specific validation normalize
        // components in the same place as setItemMeta.
        ItemMeta meta = result.getItemMeta();
        if (meta != null) result.setItemMeta(meta);
        return result;
    }

    @Override
    public int getAmount() {
        return handle != null ? handle.getCount() : 0;
    }

    @Override
    public void setAmount(int amount) {
        // LunarArc CraftItemStack is backed directly by an NMS stack rather than
        // Paper's ItemStack craftDelegate. Do not call the API superclass here:
        // its delegate is intentionally absent in this bridge and would throw.
        if (handle == null || handle.isEmpty()) {
            return;
        }
        if (amount <= 0) {
            handle.setCount(0);
            return;
        }
        handle.setCount(amount);
    }

    public static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack stack) {
        // Bukkit/CraftBukkit represents an empty slot as an AIR ItemStack at API
        // boundaries such as Player#getItemInHand(). Returning null breaks plugins
        // (notably WorldEdit's BukkitAdapter) that immediately adapt the type.
        if (stack == null || stack.isEmpty()) {
            return new CraftItemStack(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return new CraftItemStack(stack.copy());
    }

    public static net.minecraft.world.item.ItemStack asNMSCopy(@Nullable ItemStack stack) {
        if (stack == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        if (stack instanceof CraftItemStack craftStack) {
            if (craftStack.handle == null || craftStack.handle.isEmpty()) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            net.minecraft.world.item.ItemStack copy = craftStack.handle.copy();
            copy.setCount(stack.getAmount());
            return copy;
        }

        if (stack.getType() == Material.AIR) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(stack.getType().getKey().toString())
        );
        if (item == null) return net.minecraft.world.item.ItemStack.EMPTY;

        net.minecraft.world.item.ItemStack converted = new net.minecraft.world.item.ItemStack(item, stack.getAmount());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            CraftItemMeta.copyOf(meta).applyToNms(converted);
        }
        return converted;
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

    public @Nullable net.minecraft.resources.ResourceLocation getItemKey() {
        if (handle == null || handle.isEmpty()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(handle.getItem());
    }

    public boolean isModdedItem() {
        net.minecraft.resources.ResourceLocation key = getItemKey();
        return key != null && !"minecraft".equals(key.getNamespace());
    }


    @Override
    public int getMaxStackSize() {
        if (handle == null || handle.isEmpty()) return 64;
        try { return handle.getMaxStackSize(); } catch (Throwable ignored) { return getType().getMaxStackSize(); }
    }

    @Override
    @Deprecated
    public short getDurability() {
        if (handle == null || handle.isEmpty()) return 0;
        try { return (short) handle.getDamageValue(); } catch (Throwable ignored) { return 0; }
    }

    @Override
    @Deprecated
    public void setDurability(short durability) {
        if (handle == null || handle.isEmpty()) return;
        try { handle.setDamageValue(Math.max(0, durability)); } catch (Throwable ignored) {}
    }

    @Override
    public boolean isSimilar(@Nullable ItemStack stack) {
        if (stack == null) return false;
        net.minecraft.world.item.ItemStack other = asNMSCopy(stack);
        if (handle == null || handle.isEmpty()) return other.isEmpty();
        if (other.isEmpty()) return false;
        try { return net.minecraft.world.item.ItemStack.isSameItemSameComponents(handle, other); }
        catch (Throwable ignored) { return getType() == stack.getType() && java.util.Objects.equals(getItemMeta(), stack.getItemMeta()); }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemStack other)) return false;
        return getAmount() == other.getAmount() && isSimilar(other);
    }

    @Override
    public int hashCode() { return java.util.Objects.hash(getType(), getAmount(), getItemMeta()); }

    @Override
    public String toString() { return "ItemStack{" + getType() + " x " + getAmount() + (hasItemMeta() ? ", " + getItemMeta() : "") + "}"; }

    @Override
    public boolean containsEnchantment(@NotNull org.bukkit.enchantments.Enchantment ench) {
        ItemMeta meta = getItemMeta(); return meta != null && meta.hasEnchant(ench);
    }

    @Override
    public int getEnchantmentLevel(@NotNull org.bukkit.enchantments.Enchantment ench) {
        ItemMeta meta = getItemMeta(); return meta == null ? 0 : meta.getEnchantLevel(ench);
    }

    @Override
    public @NotNull java.util.Map<org.bukkit.enchantments.Enchantment, Integer> getEnchantments() {
        ItemMeta meta = getItemMeta(); return meta == null ? java.util.Collections.emptyMap() : meta.getEnchants();
    }

    @Override
    public void addEnchantment(@NotNull org.bukkit.enchantments.Enchantment ench, int level) {
        if (ench == null) throw new IllegalArgumentException("Enchantment cannot be null");
        if (!ench.canEnchantItem(this)) throw new IllegalArgumentException("Enchantment cannot be applied to " + getType());
        if (level < ench.getStartLevel() || level > ench.getMaxLevel()) throw new IllegalArgumentException("Enchantment level is outside allowed range");
        addUnsafeEnchantment(ench, level);
    }

    @Override
    public void addUnsafeEnchantment(@NotNull org.bukkit.enchantments.Enchantment ench, int level) {
        CraftItemMeta meta = (CraftItemMeta) getItemMeta();
        meta.addEnchant(ench, level, true);
        setItemMeta(meta);
    }

    @Override
    public int removeEnchantment(@NotNull org.bukkit.enchantments.Enchantment ench) {
        CraftItemMeta meta = (CraftItemMeta) getItemMeta();
        int old = meta.getEnchantLevel(ench);
        if (old != 0) { meta.removeEnchant(ench); setItemMeta(meta); }
        return old;
    }

    @Override
    public @NotNull java.util.Map<String, Object> serialize() {
        java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
        out.put("v", net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        out.put("type", getType().name());
        if (getAmount() != 1) out.put("amount", getAmount());
        if (hasItemMeta()) out.put("meta", getItemMeta());
        return out;
    }

    @Override
    @Deprecated(forRemoval = true)
    public String getTranslationKey() {
        return handle == null || handle.isEmpty() ? "block.minecraft.air" : handle.getItem().getDescriptionId();
    }

    @Override
    public io.papermc.paper.persistence.@NotNull PersistentDataContainerView getPersistentDataContainer() {
        ItemMeta meta = getItemMeta();
        return (io.papermc.paper.persistence.PersistentDataContainerView) meta.getPersistentDataContainer();
    }

    @Override
    public boolean isEmpty() {
        return handle == null || handle.isEmpty() || getAmount() <= 0 || getType() == Material.AIR;
    }

    @Override
    public @NotNull ItemStack asOne() { return asQuantity(1); }

    @Override
    public @NotNull ItemStack asQuantity(int qty) {
        CraftItemStack copy = clone();
        copy.setAmount(qty);
        return copy;
    }

    @Override
    public @NotNull ItemStack add(int qty) {
        setAmount(Math.min(getMaxStackSize(), getAmount() + qty));
        return this;
    }

    @Override
    public @NotNull ItemStack subtract(int qty) {
        setAmount(Math.max(0, getAmount() - qty));
        return this;
    }

    @Override
    public int getMaxItemUseDuration(@NotNull org.bukkit.entity.LivingEntity entity) {
        if (handle == null || handle.isEmpty()) return 0;
        try {
            net.minecraft.world.entity.LivingEntity living = entity instanceof org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity craft
                    && craft.getHandle() instanceof net.minecraft.world.entity.LivingEntity nmsLiving ? nmsLiving : null;
            return handle.getUseDuration(living);
        } catch (Throwable ignored) {
            try { return handle.getUseDuration(null); } catch (Throwable ignoredAgain) { return 0; }
        }
    }

    @Override
    public CraftItemStack clone() {
        return new CraftItemStack(handle == null ? net.minecraft.world.item.ItemStack.EMPTY : handle.copy());
    }

    @Override
    public boolean hasItemMeta() {
        return handle != null && !handle.isEmpty() && !handle.getComponentsPatch().isEmpty();
    }

    @Override
    public @Nullable ItemMeta getItemMeta() {
        if (handle == null || handle.isEmpty()) return new CraftItemMeta();
        return new CraftItemMeta(handle);
    }

    @Override
    public boolean setItemMeta(@Nullable ItemMeta meta) {
        if (handle == null || handle.isEmpty()) return false;
        if (meta == null) {
            // Paper 1.21.1 clears the complete patch, not a hand-picked subset.
            // This prevents stale lore/components/PDC/modded data surviving
            // ItemStack#setItemMeta(null).
            handle.restorePatch(net.minecraft.core.component.DataComponentPatch.EMPTY);
            return true;
        }
        CraftItemMeta craftMeta = meta instanceof CraftItemMeta c ? c.clone() : CraftItemMeta.copyOf(meta);
        handle.restorePatch(net.minecraft.core.component.DataComponentPatch.EMPTY);
        craftMeta.applyToNms(handle);
        return true;
    }


}
