package org.bukkit.craftbukkit.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftItemStack extends ItemStack {
    private static final java.lang.invoke.VarHandle API_ITEM_STACK_CRAFT_DELEGATE_FIELD;
    static {
        try {
            API_ITEM_STACK_CRAFT_DELEGATE_FIELD = java.lang.invoke.MethodHandles.privateLookupIn(
                    ItemStack.class, java.lang.invoke.MethodHandles.lookup())
                    .findVarHandle(ItemStack.class, "craftDelegate", ItemStack.class);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public net.minecraft.world.item.ItemStack handle;

    public static @Nullable CraftItemStack getCraftStack(@Nullable ItemStack stack) {
        if (stack == null) return null;
        if (stack instanceof CraftItemStack craft) return craft;
        Object delegate = API_ITEM_STACK_CRAFT_DELEGATE_FIELD.get(stack);
        return delegate instanceof CraftItemStack craft ? craft : null;
    }

    public CraftItemStack(net.minecraft.world.item.ItemStack handle) {
        this.handle = handle;
    }

    @Override
    public @NotNull Material getType() {
        if (handle == null || handle.isEmpty()) return Material.AIR;
        net.minecraft.resources.Identifier key =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(handle.getItem());
        if (key == null) return Material.AIR;

        Material material = io.lunararcdevs.lunararc.common.server.LunarArcDynamicBukkitEnums.material(key);
        if (material != null) return material;

        material = Material.matchMaterial(key.toString());
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
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.parse(type.getKey().toString()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            throw new IllegalArgumentException(type + " is not an item");
        }
        if (handle == null || handle.isEmpty()) {
            handle = new net.minecraft.world.item.ItemStack(item, 1);
        } else {


            net.minecraft.world.item.ItemStack replacement = new net.minecraft.world.item.ItemStack(item, handle.getCount());
            replacement.applyComponents(handle.getComponentsPatch());
            handle = replacement;
        }
    }

    @Override
    public @NotNull ItemStack withType(@NotNull Material type) {
        if (type == null) throw new IllegalArgumentException("Material cannot be null");
        if (type == Material.AIR) return new CraftItemStack(net.minecraft.world.item.ItemStack.EMPTY);
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.parse(type.getKey().toString()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException(type + " is not an item");
        net.minecraft.world.item.ItemStack replacement = new net.minecraft.world.item.ItemStack(item, Math.max(1, getAmount()));
        if (handle != null && !handle.isEmpty()) {


            replacement.applyComponents(handle.getComponentsPatch());
        }
        CraftItemStack result = new CraftItemStack(replacement);


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


        if (handle == null || handle.isEmpty()) {
            return;
        }
        if (amount <= 0) {
            handle.setCount(0);
            return;
        }
        handle.setCount(amount);
    }

    /** Paper's dialog codecs hand over either a stack or a stack template (both are ItemInstance in 26.x). */
    public static java.util.List<net.minecraft.world.item.ItemStack> asNMSCopy(java.util.List<? extends ItemStack> original) {
        java.util.List<net.minecraft.world.item.ItemStack> copies = new java.util.ArrayList<>(original.size());
        for (ItemStack stack : original) copies.add(asNMSCopy(stack));
        return copies;
    }

    public static ItemMeta getItemMeta(net.minecraft.world.item.ItemStack item) {
        return item == null || item.isEmpty() ? null : asCraftMirror(item).getItemMeta();
    }

    public static boolean setItemMeta(net.minecraft.world.item.ItemStack item, ItemMeta meta) {
        return item != null && !item.isEmpty() && asCraftMirror(item).setItemMeta(meta);
    }

    public static void applyMetaToItem(net.minecraft.world.item.ItemStack item, ItemMeta meta) {
        setItemMeta(item, meta);
    }

    public static net.minecraft.world.item.ItemStackTemplate asTemplate(ItemStack bukkit) {
        return net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(asNMSCopy(bukkit));
    }

    public static ItemStack asBukkitMirror(net.minecraft.world.item.ItemStack original) {
        return asCraftMirror(original);
    }

    public static net.minecraft.advancements.predicates.ItemPredicate asCriterionConditionItem(ItemStack key) {
        net.minecraft.world.item.ItemStack item = unwrap(key);
        return net.minecraft.advancements.predicates.ItemPredicate.Builder.item()
                .of(org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry(net.minecraft.core.registries.Registries.ITEM), item.getItem())
                .withComponents(net.minecraft.advancements.predicates.DataComponentMatchers.Builder.components()
                        .exact(net.minecraft.core.component.DataComponentExactPredicate.allOf(
                                net.minecraft.core.component.PatchedDataComponentMap.fromPatch(
                                        net.minecraft.core.component.DataComponentMap.EMPTY, item.getComponentsPatch())))
                        .build())
                .build();
    }

    public static ItemMeta getItemMeta(net.minecraft.world.item.ItemStack item, org.bukkit.inventory.ItemType metaForType) {
        return asCraftMirror(item).getItemMeta();
    }

    public static ItemStack asBukkitCopy(net.minecraft.world.item.ItemInstance instance) {
        if (instance instanceof net.minecraft.world.item.ItemStack stack) return asBukkitCopy(stack);
        if (instance instanceof net.minecraft.world.item.ItemStackTemplate template) return asBukkitCopy(template.create());
        throw new IllegalArgumentException("Unsupported item instance: " + instance);
    }

    public static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack stack) {


        if (stack == null || stack.isEmpty()) {
            return new CraftItemStack(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return new CraftItemStack(stack.copy());
    }

    public static net.minecraft.world.item.ItemStack asNMSCopy(@Nullable ItemStack stack) {
        if (stack == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        CraftItemStack craftStack = getCraftStack(stack);
        if (craftStack != null) {
            if (craftStack.handle == null || craftStack.handle.isEmpty()) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            return craftStack.handle.copy();
        }

        if (stack.getType() == Material.AIR) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.parse(stack.getType().getKey().toString())
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

    public @Nullable net.minecraft.resources.Identifier getItemKey() {
        if (handle == null || handle.isEmpty()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(handle.getItem());
    }

    public boolean isModdedItem() {
        net.minecraft.resources.Identifier key = getItemKey();
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
    public void removeEnchantments() {
        CraftItemMeta meta = (CraftItemMeta) getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            for (org.bukkit.enchantments.Enchantment ench : new java.util.ArrayList<>(meta.getEnchants().keySet())) meta.removeEnchant(ench);
            setItemMeta(meta);
        }
    }

    @Override
    public @NotNull java.util.Map<String, Object> serialize() {
        java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
        out.put("v", net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version());
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
            net.minecraft.world.entity.LivingEntity living = entity instanceof org.bukkit.craftbukkit.entity.CraftEntity craft
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
        org.bukkit.Material type = getType();
        if (type == org.bukkit.Material.POTION || type == org.bukkit.Material.SPLASH_POTION
                || type == org.bukkit.Material.LINGERING_POTION || type == org.bukkit.Material.TIPPED_ARROW) return new CraftMetaPotion(handle);
        if (type == org.bukkit.Material.WRITABLE_BOOK) return new CraftMetaBook(handle);
        if (type == org.bukkit.Material.WRITTEN_BOOK) return new CraftMetaBookSigned(handle);
        if (type == org.bukkit.Material.FIREWORK_ROCKET) return new CraftMetaFirework(handle);
        if (type == org.bukkit.Material.FIREWORK_STAR) return new CraftMetaCharge(handle);
        if (type == org.bukkit.Material.PLAYER_HEAD) return new CraftMetaSkull(handle);
        if (CraftItemFactory.isLeather(type)) return new CraftMetaColorableArmor(handle);
        if (type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE") || type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS")) return new CraftMetaArmor(handle);
        if (type.name().endsWith("_BANNER")) return new CraftMetaBanner(handle);
        return new CraftItemMeta(handle);
    }


    private static void resetComponentsToPrototype(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getComponentsPatch().isEmpty()) return;


        java.util.List<net.minecraft.core.component.DataComponentType<?>> changed = new java.util.ArrayList<>();
        for (var entry : ComponentPatches.entries(stack.getComponentsPatch()).entrySet()) {
            changed.add(entry.getKey());
        }
        for (net.minecraft.core.component.DataComponentType<?> type : changed) {
            restorePrototypeComponent(stack, type);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restorePrototypeComponent(net.minecraft.world.item.ItemStack stack,
            net.minecraft.core.component.DataComponentType<?> type) {
        net.minecraft.core.component.DataComponentType rawType = type;
        Object prototypeValue = stack.getPrototype().get(rawType);
        if (prototypeValue == null) {
            stack.remove(rawType);
        } else {
            stack.set(rawType, prototypeValue);
        }
    }

    @Override
    public boolean setItemMeta(@Nullable ItemMeta meta) {
        if (handle == null || handle.isEmpty()) return false;


        resetComponentsToPrototype(handle);
        if (meta == null) return true;

        CraftItemMeta craftMeta = meta instanceof CraftItemMeta c ? c.clone() : CraftItemMeta.copyOf(meta);
        craftMeta.applyToNms(handle);
        return true;
    }

    // CraftBukkit's NMS bridging statics. unwrap and getOrCloneOnMutation in particular are how
    // Paper's own code, and plugins copying it, get at the backing stack without a defensive copy.
    public static net.minecraft.world.item.ItemStack unwrap(ItemStack bukkit) {
        CraftItemStack craft = getCraftStack(bukkit);
        return craft == null || craft.handle == null ? net.minecraft.world.item.ItemStack.EMPTY : craft.handle;
    }

    public static net.minecraft.world.item.ItemStack getOrCloneOnMutation(ItemStack old, ItemStack newInstance) {
        return old == newInstance ? unwrap(old) : asNMSCopy(newInstance);
    }

    public static net.minecraft.world.item.ItemStack copyNMSStack(net.minecraft.world.item.ItemStack original, int amount) {
        net.minecraft.world.item.ItemStack stack = original.copy();
        stack.setCount(amount);
        return stack;
    }

    public static CraftItemStack asNewCraftStack(net.minecraft.world.item.Item item) {
        return asNewCraftStack(item, 1);
    }

    public static CraftItemStack asNewCraftStack(net.minecraft.world.item.Item item, int amount) {
        return new CraftItemStack(new net.minecraft.world.item.ItemStack(item, amount));
    }

    @Override
    public net.kyori.adventure.text.@NotNull Component effectiveName() {
        return handle == null || handle.isEmpty() ? net.kyori.adventure.text.Component.empty()
                : io.papermc.paper.adventure.PaperAdventure.asAdventure(handle.getStyledHoverName());
    }

    @Override
    public boolean editPersistentDataContainer(final java.util.function.Consumer<org.bukkit.persistence.PersistentDataContainer> consumer) {
        if (handle == null || handle.isEmpty()) return false;
        ItemMeta meta = getItemMeta();
        consumer.accept(meta.getPersistentDataContainer());
        setItemMeta(meta);
        return true;
    }

    // Paper data component API
    private boolean noData() {
        return handle == null || handle.isEmpty();
    }

    @Override
    public <T> T getData(final io.papermc.paper.datacomponent.DataComponentType.Valued<T> type) {
        if (noData()) return null;
        return io.papermc.paper.datacomponent.PaperDataComponentType.convertDataComponentValue(
                handle.getComponents(), (io.papermc.paper.datacomponent.PaperDataComponentType.ValuedImpl<T, ?>) type);
    }

    @Override
    public boolean hasData(final io.papermc.paper.datacomponent.DataComponentType type) {
        if (noData()) return false;
        return handle.has(io.papermc.paper.datacomponent.PaperDataComponentType.bukkitToMinecraft(type));
    }

    @Override
    public java.util.Set<io.papermc.paper.datacomponent.DataComponentType> getDataTypes() {
        if (noData()) return java.util.Collections.emptySet();
        return io.papermc.paper.datacomponent.PaperDataComponentType.minecraftToBukkit(handle.getComponents().keySet());
    }

    @Override
    public <T> void setData(final io.papermc.paper.datacomponent.DataComponentType.Valued<T> type, final T value) {
        com.google.common.base.Preconditions.checkArgument(value != null, "value cannot be null");
        if (noData()) return;
        setDataInternal((io.papermc.paper.datacomponent.PaperDataComponentType.ValuedImpl<T, ?>) type, value);
    }

    @Override
    public void setData(final io.papermc.paper.datacomponent.DataComponentType.NonValued type) {
        if (noData()) return;
        setDataInternal((io.papermc.paper.datacomponent.PaperDataComponentType.NonValuedImpl<?, ?>) type, null);
    }

    private <A, V> void setDataInternal(final io.papermc.paper.datacomponent.PaperDataComponentType<A, V> type, final A value) {
        handle.set(type.getHandle(), type.getAdapter().toVanilla(value, type.getHolder()));
    }

    @Override
    public void unsetData(final io.papermc.paper.datacomponent.DataComponentType type) {
        if (noData()) return;
        handle.remove(io.papermc.paper.datacomponent.PaperDataComponentType.bukkitToMinecraft(type));
    }

    @Override
    public void resetData(final io.papermc.paper.datacomponent.DataComponentType type) {
        if (noData()) return;
        resetDataInternal((io.papermc.paper.datacomponent.PaperDataComponentType<?, ?>) type);
    }

    private <M> void resetDataInternal(final io.papermc.paper.datacomponent.PaperDataComponentType<?, M> type) {
        final net.minecraft.core.component.DataComponentType<M> nms = io.papermc.paper.datacomponent.PaperDataComponentType.bukkitToMinecraft(type);
        final M nmsValue = handle.getItem().components().get(nms);
        handle.set(nms, nmsValue);
    }

    @Override
    public void copyDataFrom(final ItemStack source, final java.util.function.Predicate<io.papermc.paper.datacomponent.DataComponentType> filter) {
        com.google.common.base.Preconditions.checkArgument(source != null, "source cannot be null");
        com.google.common.base.Preconditions.checkArgument(filter != null, "filter cannot be null");
        CraftItemStack craftSource = getCraftStack(source);
        if (noData() || craftSource == null || craftSource.noData()) return;

        final java.util.function.Predicate<net.minecraft.core.component.DataComponentType<?>> nmsFilter =
                nms -> filter.test(io.papermc.paper.datacomponent.PaperDataComponentType.minecraftToBukkit(nms));
        net.minecraft.world.item.ItemStack sourceNms = craftSource.handle;
        handle.applyComponents(sourceNms.getPrototype().filter(nmsType -> !sourceNms.hasNonDefault(nmsType) && nmsFilter.test(nmsType)));

        final net.minecraft.core.component.DataComponentPatch.SplitResult split = sourceNms.getComponentsPatch().split();
        handle.applyComponents(split.added().filter(nmsFilter));
        split.removed().stream().filter(nmsFilter).forEach(handle::remove);
    }

    @Override
    public boolean isDataOverridden(final io.papermc.paper.datacomponent.DataComponentType type) {
        if (noData()) return false;
        return handle.hasNonDefault(io.papermc.paper.datacomponent.PaperDataComponentType.bukkitToMinecraft(type));
    }

    @Override
    public boolean matchesWithoutData(final ItemStack item, final java.util.Set<io.papermc.paper.datacomponent.DataComponentType> exclude, final boolean ignoreCount) {
        final CraftItemStack other = getCraftStack(item);
        if (other == null) return false;
        if (handle == other.handle) return true;
        if (handle == null || other.handle == null) return false;
        if (handle.isEmpty() && other.handle.isEmpty()) return true;

        net.minecraft.world.item.ItemStack left = handle;
        net.minecraft.world.item.ItemStack right = other.handle;
        if (!ignoreCount && left.getCount() != right.getCount()) return false;
        if (!left.is(right.getItem())) return false;
        if (exclude.isEmpty()) return left.getComponentsPatch().equals(right.getComponentsPatch());

        java.util.Set<net.minecraft.core.component.DataComponentType<?>> skipping = new java.util.HashSet<>(exclude.size());
        for (io.papermc.paper.datacomponent.DataComponentType api : exclude) {
            skipping.add(io.papermc.paper.datacomponent.PaperDataComponentType.bukkitToMinecraft(api));
        }
        return left.getComponentsPatch().forget(skipping::contains).equals(right.getComponentsPatch().forget(skipping::contains));
    }
}
