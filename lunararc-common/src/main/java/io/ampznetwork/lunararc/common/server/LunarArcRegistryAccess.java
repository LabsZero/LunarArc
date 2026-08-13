package io.ampznetwork.lunararc.common.server;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LunarArcRegistryAccess implements RegistryAccess {
    public static final RegistryAccess INSTANCE = new LunarArcRegistryAccess();
    private final Map<Class<?>, Registry<?>> registries = new ConcurrentHashMap<>();

    private LunarArcRegistryAccess() {}

    @Override
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull RegistryKey<T> key) {
        Class<T> type = resolveType(key);
        return type != null ? getRegistry(type) : LunarArcBukkitRegistry.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull Class<T> type) {
        Registry<?> existing = registries.get(type);
        if (existing != null) return (Registry<T>) existing;

        Registry<T> created = createRegistry(type);
        Registry<?> raced = registries.putIfAbsent(type, created);
        return raced == null ? created : (Registry<T>) raced;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> createRegistry(Class<T> type) {
        if ("org.bukkit.damage.DamageType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createDamageTypeRegistry((Class<? extends Keyed>) type);
        }
        if ("org.bukkit.potion.PotionEffectType".equals(type.getName())) {
            Registry<? extends Keyed> effects = createPotionEffectTypeRegistry(type);
            if (effects != null) return (Registry<T>) effects;
        }
        // Paper 1.21's Material#isItem()/isBlock() are registry-backed. Returning an
        // empty registry here makes perfectly normal materials such as WOODEN_AXE
        // report that they are not items, which then causes the Paper ItemStack
        // constructor (and WorldEdit //wand) to throw. Expose the live Minecraft
        // registries through lightweight Paper API registry-entry proxies.
        if ("org.bukkit.inventory.ItemType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createMinecraftTypeRegistry((Class<? extends Keyed>) type, true);
        }
        if ("org.bukkit.block.BlockType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createMinecraftTypeRegistry((Class<? extends Keyed>) type, false);
        }
        // MenuType constants bootstrap through Registry.MENU.getOrThrow(...) during
        // org.bukkit.inventory.MenuType.<clinit> (triggered by InventoryType.<clinit>
        // at player login). An enum-only registry leaves them empty, so the first
        // constant (generic_9x1) throws NoSuchElementException and permanently
        // poisons InventoryType. Populate from the live vanilla menu registry.
        if ("org.bukkit.inventory.MenuType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createMenuTypeRegistry((Class<? extends Keyed>) type);
        }
        // Every remaining Paper registry entry type whose <clinit> bootstraps
        // through Registry.*.getOrThrow. Enums are safe via forType(); these
        // interfaces/classes need populated registries or their static constants
        // throw NoSuchElementException (same failure mode as the MenuType crash).
        switch (type.getName()) {
            case "org.bukkit.enchantments.Enchantment":
                return (Registry<T>) LunarArcRegistryEntries.createEnchantmentRegistry();
            case "org.bukkit.GameEvent":
                return (Registry<T>) LunarArcRegistryEntries.createGameEventRegistry();
            case "org.bukkit.MusicInstrument":
                return (Registry<T>) LunarArcRegistryEntries.createMusicInstrumentRegistry();
            case "org.bukkit.generator.structure.Structure":
                return (Registry<T>) LunarArcRegistryEntries.createStructureRegistry();
            case "org.bukkit.generator.structure.StructureType":
                return (Registry<T>) LunarArcRegistryEntries.createStructureTypeRegistry();
            case "org.bukkit.block.banner.PatternType":
                return (Registry<T>) LunarArcRegistryEntries.createPatternTypeRegistry();
            case "org.bukkit.inventory.meta.trim.TrimMaterial":
                return (Registry<T>) LunarArcRegistryEntries.createTrimRegistry(
                        (Class<? extends Keyed>) type, LunarArcRegistryEntries.TRIM_MATERIAL_KEYS, "trim_material");
            case "org.bukkit.inventory.meta.trim.TrimPattern":
                return (Registry<T>) LunarArcRegistryEntries.createTrimRegistry(
                        (Class<? extends Keyed>) type, LunarArcRegistryEntries.TRIM_PATTERN_KEYS, "trim_pattern");
            case "org.bukkit.JukeboxSong":
                return (Registry<T>) LunarArcRegistryEntries.createJukeboxSongRegistry();
            case "org.bukkit.map.MapCursor$Type":
                return (Registry<T>) LunarArcRegistryEntries.createMapCursorTypeRegistry();
            case "org.bukkit.entity.Cat$Type":
                return (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                        (Class<? extends Keyed>) type, net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT);
            case "org.bukkit.entity.Frog$Variant":
                return (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                        (Class<? extends Keyed>) type, net.minecraft.core.registries.BuiltInRegistries.FROG_VARIANT);
            case "org.bukkit.entity.Villager$Profession":
                return (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                        (Class<? extends Keyed>) type, net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION);
            case "org.bukkit.entity.Villager$Type":
                return (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                        (Class<? extends Keyed>) type, net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE);
            case "org.bukkit.entity.Wolf$Variant":
                return (Registry<T>) LunarArcRegistryEntries.createKeyedHardcodedRegistry(
                        (Class<? extends Keyed>) type, LunarArcRegistryEntries.WOLF_VARIANT_KEYS);
            default:
                return LunarArcBukkitRegistry.forType(type);
        }
    }

    /**
     * Builds a Bukkit {@code Registry<MenuType>} from the live NMS menu registry.
     * Each entry becomes a {@code CraftMenuType} (a real {@code MenuType.Typed})
     * keyed by its {@code minecraft:<path>} id so the Paper MenuType constants and
     * {@code Registry.MENU} lookups resolve.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<? extends Keyed> createMenuTypeRegistry(Class<? extends Keyed> type) {
        java.util.List<Keyed> values = new java.util.ArrayList<>();
        net.minecraft.core.Registry<net.minecraft.world.inventory.MenuType<?>> nms =
                net.minecraft.core.registries.BuiltInRegistries.MENU;
        for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.inventory.MenuType<?>>, net.minecraft.world.inventory.MenuType<?>> entry : nms.entrySet()) {
            net.minecraft.resources.ResourceLocation location = entry.getKey().location();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(location.getNamespace(), location.getPath());
            values.add(new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftMenuType<>(key, entry.getValue(), viewClassFor(location.getPath())));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    private static Class<? extends org.bukkit.inventory.InventoryView> viewClassFor(String path) {
        return switch (path) {
            case "anvil" -> org.bukkit.inventory.view.AnvilView.class;
            case "beacon" -> org.bukkit.inventory.view.BeaconView.class;
            case "blast_furnace", "furnace", "smoker" -> org.bukkit.inventory.view.FurnaceView.class;
            case "brewing_stand" -> org.bukkit.inventory.view.BrewingStandView.class;
            case "crafter_3x3" -> org.bukkit.inventory.view.CrafterView.class;
            case "enchantment" -> org.bukkit.inventory.view.EnchantmentView.class;
            case "lectern" -> org.bukkit.inventory.view.LecternView.class;
            case "loom" -> org.bukkit.inventory.view.LoomView.class;
            case "merchant" -> org.bukkit.inventory.view.MerchantView.class;
            case "stonecutter" -> org.bukkit.inventory.view.StonecutterView.class;
            default -> org.bukkit.inventory.InventoryView.class;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<? extends Keyed> createMinecraftTypeRegistry(Class<? extends Keyed> type, boolean itemRegistry) {
        final java.util.Map<org.bukkit.NamespacedKey, Keyed> cache = new java.util.concurrent.ConcurrentHashMap<>();

        return new org.bukkit.Registry<Keyed>() {
            private boolean exists(org.bukkit.NamespacedKey key) {
                if (key == null) return false;
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(key.toString());
                if (id == null) return false;
                if (itemRegistry) {
                    return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id);
                }
                return net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id);
            }

            private Keyed create(org.bukkit.NamespacedKey key) {
                if (!exists(key)) return null;
                return cache.computeIfAbsent(key, ignored -> (Keyed) java.lang.reflect.Proxy.newProxyInstance(
                        type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
                            if (method.isDefault()) {
                                Object[] actualArgs = args == null ? new Object[0] : args;
                                return java.lang.reflect.InvocationHandler.invokeDefault(instance, method, actualArgs);
                            }
                            String name = method.getName();
                            switch (name) {
                                case "getKey": return key;
                                case "key": return net.kyori.adventure.key.Key.key(key.getNamespace(), key.getKey());
                                case "translationKey":
                                case "getTranslationKey":
                                    return (itemRegistry ? "item." : "block.") + key.getNamespace() + "." + key.getKey();
                                case "getMaxStackSize": {
                                    if (!itemRegistry) return 0;
                                    net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(key.toString());
                                    Object item = id == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                                    if (item != null) {
                                        try {
                                            java.lang.reflect.Method max = item.getClass().getMethod("getDefaultMaxStackSize");
                                            Object value = max.invoke(item);
                                            if (value instanceof Number number) return number.intValue();
                                        } catch (ReflectiveOperationException ignored2) {
                                        }
                                    }
                                    return 64;
                                }
                                case "isEnabledByFeature": return true;
                                case "toString": return type.getSimpleName() + "[" + key + "]";
                                case "hashCode": return key.hashCode();
                                case "equals": {
                                    Object other = args == null || args.length == 0 ? null : args[0];
                                    if (instance == other) return true;
                                    if (!(other instanceof org.bukkit.Keyed keyed)) return false;
                                    return key.equals(keyed.getKey());
                                }
                                default: {
                                    // Paper registry entry interfaces grow convenience methods over time.
                                    // Supply safe primitive/default values for methods not required to
                                    // identify the entry; default interface methods are handled by the JVM.
                                    Class<?> returnType = method.getReturnType();
                                    if (returnType == boolean.class) return false;
                                    if (returnType == byte.class) return (byte) 0;
                                    if (returnType == short.class) return (short) 0;
                                    if (returnType == int.class) return 0;
                                    if (returnType == long.class) return 0L;
                                    if (returnType == float.class) return 0.0F;
                                    if (returnType == double.class) return 0.0D;
                                    if (returnType == char.class) return (char) 0;
                                    return null;
                                }
                            }
                        }));
            }

            @Override public Keyed get(org.bukkit.NamespacedKey key) { return create(key); }
            @Override public Keyed getOrThrow(org.bukkit.NamespacedKey key) {
                Keyed value = create(key);
                if (value == null) throw new java.util.NoSuchElementException(key.toString());
                return value;
            }
            @Override public org.bukkit.NamespacedKey getKey(Keyed value) { return value == null ? null : value.getKey(); }
            @Override public java.util.Iterator<Keyed> iterator() {
                java.util.List<Keyed> values = new java.util.ArrayList<>();
                java.util.Set<net.minecraft.resources.ResourceLocation> keys = itemRegistry
                        ? net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()
                        : net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet();
                for (net.minecraft.resources.ResourceLocation id : keys) {
                    Keyed value = create(new org.bukkit.NamespacedKey(id.getNamespace(), id.getPath()));
                    if (value != null) values.add(value);
                }
                return values.iterator();
            }
            @Override public java.util.stream.Stream<Keyed> stream() {
                java.util.Spliterator<Keyed> spliterator = java.util.Spliterators.spliteratorUnknownSize(iterator(), 0);
                return java.util.stream.StreamSupport.stream(spliterator, false);
            }
        };
    }


    /**
     * Paper's PotionEffectType constants bootstrap through RegistryAccess. Unlike old
     * Bukkit they are not enum constants, so an enum-only registry leaves SPEED and
     * every other vanilla effect missing during PotionEffectType.<clinit>. Reuse the
     * exact CraftBukkit implementation inherited from the pinned Paper server and
     * construct it from NeoForge's live vanilla effect registry. Reflection keeps this
     * bridge tolerant of minor constructor-shape differences within 1.21.1.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<? extends Keyed> createPotionEffectTypeRegistry(Class<?> apiType) {
        try {
            ClassLoader loader = LunarArcRegistryAccess.class.getClassLoader();
            Class<?> craftType;
            try {
                craftType = Class.forName("org.bukkit.craftbukkit.potion.CraftPotionEffectType", false, loader);
            } catch (ClassNotFoundException first) {
                craftType = Class.forName("org.bukkit.craftbukkit."
                        + LunarArcVersionInfo.craftBukkitPackage()
                        + ".potion.CraftPotionEffectType", false, loader);
            }
            final Class<?> finalCraftType = craftType;
            final net.minecraft.core.Registry nmsRegistry = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT;
            final java.util.Map<org.bukkit.NamespacedKey, Keyed> cache = new java.util.concurrent.ConcurrentHashMap<>();

            return new org.bukkit.Registry<Keyed>() {
                private Keyed create(org.bukkit.NamespacedKey key) {
                    if (key == null) return null;
                    return cache.computeIfAbsent(key, ignored -> {
                        try {
                            net.minecraft.resources.ResourceLocation location = net.minecraft.resources.ResourceLocation.tryParse(key.toString());
                            if (location == null) return null;
                            Object effect = nmsRegistry.get(location);
                            if (effect == null) return null;
                            Object holder = nmsRegistry.wrapAsHolder(effect);
                            for (java.lang.reflect.Constructor<?> constructor : finalCraftType.getDeclaredConstructors()) {
                                constructor.setAccessible(true);
                                Class<?>[] params = constructor.getParameterTypes();
                                if (params.length == 1 && params[0].isInstance(holder)) {
                                    Object value = constructor.newInstance(holder);
                                    return value instanceof Keyed keyed ? keyed : null;
                                }
                            }
                        } catch (Throwable t) {
                            java.util.logging.Logger.getLogger("LunarArc").warning(
                                    "Could not create PotionEffectType " + key + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
                        }
                        return null;
                    });
                }

                @Override public Keyed get(org.bukkit.NamespacedKey key) { return create(key); }
                @Override public Keyed getOrThrow(org.bukkit.NamespacedKey key) {
                    Keyed value = create(key);
                    if (value == null) throw new java.util.NoSuchElementException(key.toString());
                    return value;
                }
                @Override public org.bukkit.NamespacedKey getKey(Keyed value) { return value == null ? null : value.getKey(); }
                @Override public java.util.Iterator<Keyed> iterator() {
                    java.util.List<Keyed> values = new java.util.ArrayList<>();
                    for (Object rawKey : nmsRegistry.keySet()) {
                        net.minecraft.resources.ResourceLocation location = (net.minecraft.resources.ResourceLocation) rawKey;
                        Keyed value = create(new org.bukkit.NamespacedKey(location.getNamespace(), location.getPath()));
                        if (value != null) values.add(value);
                    }
                    return values.iterator();
                }
                @Override public java.util.stream.Stream<Keyed> stream() {
                    java.util.Spliterator<Keyed> spliterator = java.util.Spliterators.spliteratorUnknownSize(iterator(), 0);
                    return java.util.stream.StreamSupport.stream(spliterator, false);
                }
            };
        } catch (Throwable error) {
            java.util.logging.Logger.getLogger("LunarArc").warning(
                    "Could not build PotionEffectType registry: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            return null;
        }
    }

    private static Registry<? extends Keyed> createDamageTypeRegistry(Class<? extends Keyed> type) {
        String[] vanilla = {
                "in_fire", "campfire", "lightning_bolt", "on_fire", "lava", "hot_floor",
                "in_wall", "cramming", "drown", "starve", "cactus", "fall", "fly_into_wall",
                "out_of_world", "generic", "magic", "wither", "dragon_breath", "dry_out",
                "sweet_berry_bush", "freeze", "stalagmite", "falling_block", "falling_anvil",
                "falling_stalactite", "sting", "mob_attack", "mob_attack_no_aggro",
                "player_attack", "arrow", "trident", "mob_projectile", "spit", "fireworks",
                "fireball", "unattributed_fireball", "wither_skull", "thrown", "indirect_magic",
                "thorns", "explosion", "player_explosion", "sonic_boom", "bad_respawn_point",
                "outside_border", "generic_kill", "wind_charge"
        };

        java.util.List<Keyed> values = new java.util.ArrayList<>(vanilla.length);
        for (String name : vanilla) {
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(name);
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[]{type},
                    (instance, method, args) -> {
                        return switch (method.getName()) {
                            case "getKey" -> key;
                            case "key" -> net.kyori.adventure.key.Key.key(key.getNamespace(), key.getKey());
                            case "getTranslationKey" -> "death.attack." + name;
                            case "getExhaustion" -> 0.0F;
                            case "toString" -> "DamageType[" + key + "]";
                            case "hashCode" -> key.hashCode();
                            case "equals" -> instance == (args == null ? null : args[0]);
                            default -> {
                                Class<?> returnType = method.getReturnType();
                                if (returnType.isEnum()) {
                                    Object[] constants = returnType.getEnumConstants();
                                    yield constants != null && constants.length > 0 ? constants[0] : null;
                                }
                                if (returnType == boolean.class) yield false;
                                if (returnType == int.class) yield 0;
                                if (returnType == long.class) yield 0L;
                                if (returnType == double.class) yield 0.0D;
                                if (returnType == float.class) yield 0.0F;
                                yield null;
                            }
                        };
                    });
            values.add((Keyed) proxy);
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Class<T> resolveType(RegistryKey<T> key) {
        for (Field field : RegistryKey.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !RegistryKey.class.isAssignableFrom(field.getType())) continue;
            try {
                Object candidate = field.get(null);
                if (!key.equals(candidate)) continue;
                java.lang.reflect.Type generic = field.getGenericType();
                if (generic instanceof java.lang.reflect.ParameterizedType parameterized) {
                    java.lang.reflect.Type valueType = parameterized.getActualTypeArguments()[0];
                    if (valueType instanceof Class<?> valueClass && Keyed.class.isAssignableFrom(valueClass)) {
                        return (Class<T>) valueClass;
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        Object identifier = readIdentifier(key);
        if (identifier == null) return null;

        for (Field field : RegistryKey.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !RegistryKey.class.isAssignableFrom(field.getType())) continue;
            try {
                Object candidate = field.get(null);
                if (!identifier.equals(readIdentifier(candidate))) continue;
                java.lang.reflect.Type generic = field.getGenericType();
                if (generic instanceof java.lang.reflect.ParameterizedType parameterized) {
                    java.lang.reflect.Type valueType = parameterized.getActualTypeArguments()[0];
                    if (valueType instanceof Class<?> valueClass && Keyed.class.isAssignableFrom(valueClass)) {
                        return (Class<T>) valueClass;
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object readIdentifier(Object value) {
        if (value == null) return null;
        for (String name : new String[]{"key", "registryKey", "getKey"}) {
            try {
                Method method = value.getClass().getMethod(name);
                if (method.getParameterCount() == 0) {
                    Object result = method.invoke(value);
                    if (result != null) return result;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
