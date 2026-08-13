package io.ampznetwork.lunararc.common.server;

import io.papermc.paper.enchantments.EnchantmentRarity;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementations of the Paper API registry entry types whose
 * {@code <clinit>} blocks bootstrap through {@code Registry.*.getOrThrow}.
 * Every such type is either a JDK-proxy for the plain {@code Keyed}
 * interfaces, or a thin subclass of the abstract classes, keyed by the
 * vanilla {@code minecraft:<path>} id.
 */
final class LunarArcRegistryEntries {

    private LunarArcRegistryEntries() {}

    // ------------------------------------------------------------------
    // Enchantment (abstract class, 24 abstract methods + Keyed + Translatable)
    // ------------------------------------------------------------------

    private static final String[] ENCHANTMENT_KEYS = {
            "aqua_affinity", "bane_of_arthropods", "binding_curse", "blast_protection",
            "breach", "channeling", "density", "depth_strider", "efficiency", "feather_falling",
            "fire_aspect", "fire_protection", "flame", "fortune", "frost_walker", "impaling",
            "infinity", "knockback", "looting", "loyalty", "luck_of_the_sea", "lure",
            "mending", "multishot", "piercing", "power", "projectile_protection", "protection",
            "punch", "quick_charge", "respiration", "riptide", "sharpness", "silk_touch",
            "smite", "soul_speed", "sweeping_edge", "swift_sneak", "thorns", "unbreaking",
            "vanishing_curse", "wind_burst"
    };

    static Registry<? extends Keyed> createEnchantmentRegistry() {
        List<Keyed> values = new ArrayList<>(ENCHANTMENT_KEYS.length);
        for (String path : ENCHANTMENT_KEYS) {
            values.add(new CraftEnchantment(NamespacedKey.minecraft(path)));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    private static final class CraftEnchantment extends Enchantment {
        private final NamespacedKey key;

        CraftEnchantment(NamespacedKey key) {
            this.key = key;
        }

        @Override public NamespacedKey getKey() { return key; }

        @Override public String getTranslationKey() { return translationKey(); }

        @Override public String translationKey() { return "enchantment." + key.getNamespace() + "." + key.getKey(); }

        @Override public String getName() { return key.getKey(); }

        @Override public int getMaxLevel() { return 5; }

        @Override public int getStartLevel() { return 1; }

        @Override public EnchantmentTarget getItemTarget() { return EnchantmentTarget.ALL; }

        @Override public boolean isTreasure() { return false; }

        @Override public boolean isCursed() { return key.getKey().contains("curse"); }

        @Override public boolean conflictsWith(Enchantment other) { return false; }

        @Override public boolean canEnchantItem(ItemStack item) { return true; }

        @Override public net.kyori.adventure.text.Component displayName(int level) {
            return net.kyori.adventure.text.Component.translatable(translationKey());
        }

        @Override public boolean isTradeable() { return true; }

        @Override public boolean isDiscoverable() { return true; }

        @Override public int getMinModifiedCost(int level) { return level; }

        @Override public int getMaxModifiedCost(int level) { return level; }

        @Override public int getAnvilCost() { return 1; }

        @Override public EnchantmentRarity getRarity() { return EnchantmentRarity.COMMON; }

        @Override public float getDamageIncrease(int level, EntityCategory category) { return 0.0F; }

        @Override public float getDamageIncrease(int level, EntityType type) { return 0.0F; }

        @Override public Set<EquipmentSlotGroup> getActiveSlotGroups() { return Set.of(); }

        @Override public net.kyori.adventure.text.Component description() {
            return net.kyori.adventure.text.Component.translatable(translationKey());
        }

        @Override public RegistryKeySet<ItemType> getSupportedItems() { return emptyKeySet(); }

        @Override public RegistryKeySet<ItemType> getPrimaryItems() { return emptyKeySet(); }

        @Override public int getWeight() { return 1; }

        @Override public RegistryKeySet<Enchantment> getExclusiveWith() { return emptyKeySet(); }

        @Override public String toString() { return "Enchantment[" + key + "]"; }

        @Override public int hashCode() { return key.hashCode(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Enchantment enchantment)) return false;
            return key.equals(enchantment.getKey());
        }
    }

    private static <E extends Keyed> RegistryKeySet<E> emptyKeySet() {
        return new RegistryKeySet<E>() {
            @Override public Collection<TypedKey<E>> values() { return List.of(); }
            @Override public Collection<E> resolve(Registry<E> registry) { return List.of(); }
            @Override public boolean contains(TypedKey<E> typedKey) { return false; }
            @Override public RegistryKey<E> registryKey() { return null; }
            @Override public int size() { return 0; }
        };
    }

    // ------------------------------------------------------------------
    // GameEvent (abstract class backed by the live NMS game event registry)
    // ------------------------------------------------------------------

    static Registry<? extends Keyed> createGameEventRegistry() {
        List<Keyed> values = new ArrayList<>();
        net.minecraft.core.DefaultedRegistry<net.minecraft.world.level.gameevent.GameEvent> nms =
                net.minecraft.core.registries.BuiltInRegistries.GAME_EVENT;
        for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.gameevent.GameEvent>,
                net.minecraft.world.level.gameevent.GameEvent> entry : nms.entrySet()) {
            net.minecraft.resources.ResourceLocation location = entry.getKey().location();
            values.add(new CraftGameEvent(
                    new NamespacedKey(location.getNamespace(), location.getPath()),
                    entry.getValue().notificationRadius()));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    private static final class CraftGameEvent extends org.bukkit.GameEvent {
        private final NamespacedKey key;
        private final int range;

        CraftGameEvent(NamespacedKey key, int range) {
            this.key = key;
            this.range = range;
        }

        @Override public NamespacedKey getKey() { return key; }

        @Override public int getRange() { return range; }

        @Override public int getVibrationLevel() { return range; }

        @Override public String toString() { return "GameEvent[" + key + "]"; }

        @Override public int hashCode() { return key.hashCode(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof org.bukkit.GameEvent event)) return false;
            return key.equals(event.getKey());
        }
    }

    // ------------------------------------------------------------------
    // MusicInstrument (abstract class, backed by the NMS instrument registry)
    // ------------------------------------------------------------------

    static Registry<? extends Keyed> createMusicInstrumentRegistry() {
        List<Keyed> values = new ArrayList<>();
        net.minecraft.core.Registry<net.minecraft.world.item.Instrument> nms =
                net.minecraft.core.registries.BuiltInRegistries.INSTRUMENT;
        for (net.minecraft.resources.ResourceLocation location : nms.keySet()) {
            values.add(new CraftMusicInstrument(new NamespacedKey(location.getNamespace(), location.getPath())));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    private static final class CraftMusicInstrument extends org.bukkit.MusicInstrument {
        private final NamespacedKey key;

        CraftMusicInstrument(NamespacedKey key) {
            this.key = key;
        }

        @Override public NamespacedKey getKey() { return key; }

        @Override public String toString() { return "MusicInstrument[" + key + "]"; }

        @Override public int hashCode() { return key.hashCode(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof org.bukkit.MusicInstrument instrument)) return false;
            return key.equals(instrument.getKey());
        }
    }

    // ------------------------------------------------------------------
    // StructureType (abstract class, backed by the NMS structure type registry)
    // ------------------------------------------------------------------

    static Registry<? extends Keyed> createStructureTypeRegistry() {
        List<Keyed> values = new ArrayList<>();
        net.minecraft.core.Registry<net.minecraft.world.level.levelgen.structure.StructureType<?>> nms =
                net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_TYPE;
        for (net.minecraft.resources.ResourceLocation location : nms.keySet()) {
            values.add(new CraftStructureType(new NamespacedKey(location.getNamespace(), location.getPath())));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    private static final class CraftStructureType extends StructureType {
        private final NamespacedKey key;

        CraftStructureType(NamespacedKey key) {
            this.key = key;
        }

        @Override public NamespacedKey getKey() { return key; }

        @Override public String toString() { return "StructureType[" + key + "]"; }

        @Override public int hashCode() { return key.hashCode(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof StructureType type)) return false;
            return key.equals(type.getKey());
        }
    }

    // ------------------------------------------------------------------
    // Structure (abstract class, hardcoded vanilla structure ids)
    // ------------------------------------------------------------------

    private static final String[] STRUCTURE_KEYS = {
            "ancient_city", "bastion_remnant", "buried_treasure", "desert_pyramid", "end_city",
            "fortress", "igloo", "jungle_pyramid", "mansion", "mineshaft", "mineshaft_mesa",
            "monument", "nether_fossil", "ocean_ruin_cold", "ocean_ruin_warm", "pillager_outpost",
            "ruined_portal", "ruined_portal_desert", "ruined_portal_jungle", "ruined_portal_mountain",
            "ruined_portal_nether", "ruined_portal_ocean", "ruined_portal_swamp", "shipwreck",
            "shipwreck_beached", "stronghold", "swamp_hut", "trail_ruins", "trial_chambers",
            "village_desert", "village_plains", "village_savanna", "village_snowy", "village_taiga"
    };

    private static final Map<String, String> STRUCTURE_TO_TYPE = Map.ofEntries(
            Map.entry("ancient_city", "jigsaw"),
            Map.entry("bastion_remnant", "jigsaw"),
            Map.entry("buried_treasure", "buried_treasure"),
            Map.entry("desert_pyramid", "desert_pyramid"),
            Map.entry("end_city", "end_city"),
            Map.entry("fortress", "fortress"),
            Map.entry("igloo", "igloo"),
            Map.entry("jungle_pyramid", "jungle_temple"),
            Map.entry("mansion", "woodland_mansion"),
            Map.entry("mineshaft", "mineshaft"),
            Map.entry("mineshaft_mesa", "mineshaft"),
            Map.entry("monument", "ocean_monument"),
            Map.entry("nether_fossil", "nether_fossil"),
            Map.entry("ocean_ruin_cold", "ocean_ruin"),
            Map.entry("ocean_ruin_warm", "ocean_ruin"),
            Map.entry("pillager_outpost", "jigsaw"),
            Map.entry("ruined_portal", "ruined_portal"),
            Map.entry("ruined_portal_desert", "ruined_portal"),
            Map.entry("ruined_portal_jungle", "ruined_portal"),
            Map.entry("ruined_portal_mountain", "ruined_portal"),
            Map.entry("ruined_portal_nether", "ruined_portal"),
            Map.entry("ruined_portal_ocean", "ruined_portal"),
            Map.entry("ruined_portal_swamp", "ruined_portal"),
            Map.entry("shipwreck", "shipwreck"),
            Map.entry("shipwreck_beached", "shipwreck"),
            Map.entry("stronghold", "stronghold"),
            Map.entry("swamp_hut", "swamp_hut"),
            Map.entry("trail_ruins", "jigsaw"),
            Map.entry("trial_chambers", "jigsaw"),
            Map.entry("village_desert", "jigsaw"),
            Map.entry("village_plains", "jigsaw"),
            Map.entry("village_savanna", "jigsaw"),
            Map.entry("village_snowy", "jigsaw"),
            Map.entry("village_taiga", "jigsaw"));

    private static final Map<String, StructureType> STRUCTURE_TYPE_CACHE = new ConcurrentHashMap<>();

    static Registry<? extends Keyed> createStructureRegistry() {
        List<Keyed> values = new ArrayList<>(STRUCTURE_KEYS.length);
        for (String path : STRUCTURE_KEYS) {
            values.add(new CraftStructure(NamespacedKey.minecraft(path)));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    private static final class CraftStructure extends org.bukkit.generator.structure.Structure {
        private final NamespacedKey key;

        CraftStructure(NamespacedKey key) {
            this.key = key;
        }

        @Override public NamespacedKey getKey() { return key; }

        @Override public StructureType getStructureType() {
            String typePath = STRUCTURE_TO_TYPE.getOrDefault(key.getKey(), "jigsaw");
            StructureType cached = STRUCTURE_TYPE_CACHE.get(typePath);
            if (cached != null) return cached;
            StructureType resolved = null;
            try {
                resolved = LunarArcRegistryAccess.INSTANCE.getRegistry(StructureType.class)
                        .get(NamespacedKey.minecraft(typePath));
            } catch (Throwable ignored) {
            }
            if (resolved == null) resolved = new CraftStructureType(NamespacedKey.minecraft(typePath));
            STRUCTURE_TYPE_CACHE.put(typePath, resolved);
            return resolved;
        }

        @Override public String toString() { return "Structure[" + key + "]"; }

        @Override public int hashCode() { return key.hashCode(); }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof org.bukkit.generator.structure.Structure structure)) return false;
            return key.equals(structure.getKey());
        }
    }

    // ------------------------------------------------------------------
    // Keyed-only interfaces: NMS-backed (Cat$Type, Frog$Variant,
    // Villager$Profession, Villager$Type) and hardcoded (Wolf$Variant)
    // ------------------------------------------------------------------

    static final String[] WOLF_VARIANT_KEYS = {
            "ashen", "black", "chestnut", "pale", "rusty", "snowy", "spotted", "striped", "woods"
    };

    static Registry<? extends Keyed> createKeyedNmsRegistry(Class<? extends Keyed> apiType,
                                                            net.minecraft.core.Registry<?> nms) {
        List<Keyed> values = new ArrayList<>();
        for (Object keyObject : nms.keySet()) {
            net.minecraft.resources.ResourceLocation location = (net.minecraft.resources.ResourceLocation) keyObject;
            String path = location.getPath();
            values.add(interfaceEntry(apiType, new NamespacedKey(location.getNamespace(), path), path, null));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    static Registry<? extends Keyed> createKeyedHardcodedRegistry(Class<? extends Keyed> apiType, String[] keys) {
        List<Keyed> values = new ArrayList<>(keys.length);
        for (String path : keys) {
            values.add(interfaceEntry(apiType, NamespacedKey.minecraft(path), path, null));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    // ------------------------------------------------------------------
    // PatternType (interface, getKey + getIdentifier)
    // ------------------------------------------------------------------

    private static final String[] PATTERN_KEYS = {
            "base", "border", "bricks", "circle", "creeper", "cross", "curly_border",
            "diagonal_left", "diagonal_right", "diagonal_up_left", "diagonal_up_right", "flow",
            "flower", "globe", "gradient", "gradient_up", "guster", "half_horizontal",
            "half_horizontal_bottom", "half_vertical", "half_vertical_right", "mojang", "piglin",
            "rhombus", "skull", "small_stripes", "square_bottom_left", "square_bottom_right",
            "square_top_left", "square_top_right", "straight_cross", "stripe_bottom", "stripe_center",
            "stripe_downleft", "stripe_downright", "stripe_left", "stripe_middle", "stripe_right",
            "stripe_top", "triangle_bottom", "triangle_top", "triangles_bottom", "triangles_top"
    };

    private static final Map<String, String> PATTERN_IDENTIFIERS = Map.ofEntries(
            Map.entry("base", "b"), Map.entry("border", "bo"), Map.entry("bricks", "bri"),
            Map.entry("circle", "c"), Map.entry("creeper", "cre"), Map.entry("cross", "cr"),
            Map.entry("curly_border", "cbo"), Map.entry("diagonal_left", "dl"),
            Map.entry("diagonal_right", "dr"), Map.entry("diagonal_up_left", "dul"),
            Map.entry("diagonal_up_right", "dud"), Map.entry("flow", "fl"), Map.entry("flower", "flo"),
            Map.entry("globe", "glo"), Map.entry("gradient", "gra"), Map.entry("gradient_up", "gru"),
            Map.entry("guster", "gus"), Map.entry("half_horizontal", "hh"),
            Map.entry("half_horizontal_bottom", "hhb"), Map.entry("half_vertical", "vh"),
            Map.entry("half_vertical_right", "vhr"), Map.entry("mojang", "moj"),
            Map.entry("piglin", "pig"), Map.entry("rhombus", "mr"), Map.entry("skull", "sku"),
            Map.entry("small_stripes", "ss"), Map.entry("square_bottom_left", "bl"),
            Map.entry("square_bottom_right", "br"), Map.entry("square_top_left", "tl"),
            Map.entry("square_top_right", "tr"), Map.entry("straight_cross", "sc"),
            Map.entry("stripe_bottom", "bs"), Map.entry("stripe_center", "cs"),
            Map.entry("stripe_downleft", "dls"), Map.entry("stripe_downright", "drs"),
            Map.entry("stripe_left", "ls"), Map.entry("stripe_middle", "ms"),
            Map.entry("stripe_right", "rs"), Map.entry("stripe_top", "ts"),
            Map.entry("triangle_bottom", "bt"), Map.entry("triangle_top", "tt"),
            Map.entry("triangles_bottom", "bts"), Map.entry("triangles_top", "tts"));

    static Registry<? extends Keyed> createPatternTypeRegistry() {
        List<Keyed> values = new ArrayList<>(PATTERN_KEYS.length);
        for (String path : PATTERN_KEYS) {
            String identifier = PATTERN_IDENTIFIERS.getOrDefault(path, path);
            values.add(interfaceEntry(
                    org.bukkit.block.banner.PatternType.class,
                    NamespacedKey.minecraft(path),
                    "minecraft." + path,
                    Map.of("getIdentifier", identifier)));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    // ------------------------------------------------------------------
    // TrimMaterial / TrimPattern (interface, getKey + getTranslationKey + description)
    // ------------------------------------------------------------------

    static final String[] TRIM_MATERIAL_KEYS = {
            "amethyst", "copper", "diamond", "emerald", "gold", "iron", "lapis", "netherite",
            "quartz", "redstone"
    };

    static final String[] TRIM_PATTERN_KEYS = {
            "bolt", "coast", "dune", "eye", "flow", "host", "raiser", "rib", "sentry", "shaper",
            "silence", "snout", "spire", "tide", "vex", "ward", "wayfinder", "wild"
    };

    static Registry<? extends Keyed> createTrimRegistry(Class<? extends Keyed> apiType, String[] keys, String prefix) {
        List<Keyed> values = new ArrayList<>(keys.length);
        for (String path : keys) {
            String translationKey = "minecraft." + prefix + "." + path;
            values.add(interfaceEntry(apiType, NamespacedKey.minecraft(path), translationKey, null));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    // ------------------------------------------------------------------
    // JukeboxSong (interface, getKey + getTranslationKey)
    // ------------------------------------------------------------------

    private static final String[] JUKEBOX_SONG_KEYS = {
            "11", "13", "5", "blocks", "cat", "chirp", "creator", "creator_music_box", "far",
            "mall", "mellohi", "otherside", "pigstep", "precipice", "relic", "stal", "strad",
            "wait", "ward"
    };

    static Registry<? extends Keyed> createJukeboxSongRegistry() {
        List<Keyed> values = new ArrayList<>(JUKEBOX_SONG_KEYS.length);
        for (String path : JUKEBOX_SONG_KEYS) {
            String translationKey = "jukebox_song." + path;
            values.add(interfaceEntry(
                    org.bukkit.JukeboxSong.class, NamespacedKey.minecraft(path), translationKey, null));
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    // ------------------------------------------------------------------
    // MapCursor$Type (interface, getKey + getValue), backed by NMS
    // map decoration types
    // ------------------------------------------------------------------

    static Registry<? extends Keyed> createMapCursorTypeRegistry() {
        List<Keyed> values = new ArrayList<>();
        net.minecraft.core.Registry<net.minecraft.world.level.saveddata.maps.MapDecorationType> nms =
                net.minecraft.core.registries.BuiltInRegistries.MAP_DECORATION_TYPE;
        byte index = 0;
        for (net.minecraft.resources.ResourceLocation location : nms.keySet()) {
            String path = location.getPath();
            values.add(interfaceEntry(
                    org.bukkit.map.MapCursor.Type.class,
                    new NamespacedKey(location.getNamespace(), path),
                    "minecraft." + path,
                    Map.of("getValue", index)));
            index++;
        }
        return LunarArcBukkitRegistry.fromValues(values);
    }

    // ------------------------------------------------------------------
    // Generic interface entry proxy
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> T interfaceEntry(Class<T> type, NamespacedKey key,
                                                      String translationKey, Map<String, Object> extras) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> {
                    if (method.isDefault()) {
                        Object[] actualArgs = args == null ? new Object[0] : args;
                        return java.lang.reflect.InvocationHandler.invokeDefault(instance, method, actualArgs);
                    }
                    if (extras != null && extras.containsKey(method.getName())) {
                        return extras.get(method.getName());
                    }
                    switch (method.getName()) {
                        case "getKey":
                            return key;
                        case "key":
                            return net.kyori.adventure.key.Key.key(key.getNamespace(), key.getKey());
                        case "getTranslationKey":
                        case "translationKey":
                            return translationKey;
                        case "description":
                            return net.kyori.adventure.text.Component.translatable(translationKey);
                        case "toString":
                            return type.getSimpleName() + "[" + key + "]";
                        case "hashCode":
                            return key.hashCode();
                        case "equals": {
                            Object other = args == null || args.length == 0 ? null : args[0];
                            if (instance == other) return true;
                            if (!(other instanceof Keyed keyed)) return false;
                            return key.equals(keyed.getKey());
                        }
                        default:
                            return defaultFor(method);
                    }
                });
    }

    private static Object defaultFor(java.lang.reflect.Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.isEnum()) {
            Object[] constants = returnType.getEnumConstants();
            if (constants != null && constants.length > 0) return constants[0];
            return null;
        }
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
