package io.papermc.paper.registry;

import io.lunararcdevs.lunararc.common.server.LunarArcBukkitRegistry;
import io.lunararcdevs.lunararc.common.server.LunarArcMissingAdapterException;
import io.lunararcdevs.lunararc.common.server.LunarArcRegistryEntries;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PaperRegistryAccess implements RegistryAccess {
    public static final RegistryAccess INSTANCE = new PaperRegistryAccess();
    private final Map<Class<?>, Registry<?>> registries = new ConcurrentHashMap<>();

    public PaperRegistryAccess() {}

    public static RegistryAccess provider() {
        return INSTANCE;
    }

    @Override
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull RegistryKey<T> key) {
        Class<T> type = resolveType(key);
        if (type == null) {
            throw LunarArcMissingAdapterException.forSurface("registry " + key);
        }
        return getRegistry(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull Class<T> type) {
        Registry<?> existing = registries.get(type);
        if (existing != null) return (Registry<T>) existing;

        Registry<T> created;
        try {
            created = createRegistry(type);
        } catch (LunarArcMissingAdapterException missing) {
            throw missing;
        } catch (Throwable throwable) {
            throw new IllegalStateException("Unable to create concrete Bukkit registry adapter for " + type.getName(), throwable);
        }
        if (created == null) {
            throw LunarArcMissingAdapterException.forSurface("registry " + type.getName());
        }
        RegistryKey<T> registryKey = registryKeyFor(type);
        if (registryKey != null) created = new LunarArcBukkitRegistry.Tagged<>(created, registryKey);
        Registry<?> raced = registries.putIfAbsent(type, created);
        return raced == null ? created : (Registry<T>) raced;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> createRegistry(Class<T> type) {
        if ("org.bukkit.damage.DamageType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) LunarArcRegistryEntries.createDamageTypeRegistry();
        }
        if ("org.bukkit.potion.PotionEffectType".equals(type.getName())) {
            Registry<? extends Keyed> effects = createPotionEffectTypeRegistry(type);
            if (effects != null) return (Registry<T>) effects;
        }


        if ("org.bukkit.inventory.ItemType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createMinecraftTypeRegistry((Class<? extends Keyed>) type, true);
        }
        if ("org.bukkit.block.BlockType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createMinecraftTypeRegistry((Class<? extends Keyed>) type, false);
        }


        if ("org.bukkit.inventory.MenuType".equals(type.getName()) && type.isInterface()) {
            return (Registry<T>) createMenuTypeRegistry((Class<? extends Keyed>) type);
        }


        switch (type.getName()) {
            case "org.bukkit.enchantments.Enchantment":
                return (Registry<T>) LunarArcRegistryEntries.createEnchantmentRegistry();
            case "org.bukkit.attribute.Attribute":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.ATTRIBUTE, org.bukkit.craftbukkit.attribute.CraftAttribute::new);
            case "org.bukkit.Fluid":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.FLUID, org.bukkit.craftbukkit.CraftFluid::new);
            case "org.bukkit.Sound":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.SOUND_EVENT, org.bukkit.craftbukkit.CraftSound::new);
            case "org.bukkit.block.Biome":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.BIOME, org.bukkit.craftbukkit.block.CraftBiome::new);
            case "org.bukkit.Art":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.PAINTING_VARIANT, org.bukkit.craftbukkit.CraftArt::new);
            case "org.bukkit.entity.memory.MemoryKey":
                return (Registry<T>) LunarArcRegistryEntries.createMemoryKeyRegistry();
            case "org.bukkit.GameEvent":
                return (Registry<T>) LunarArcRegistryEntries.createGameEventRegistry();
            case "org.bukkit.MusicInstrument":
                return (Registry<T>) LunarArcRegistryEntries.createMusicInstrumentRegistry();
            case "org.bukkit.generator.structure.Structure":
                return (Registry<T>) LunarArcRegistryEntries.createStructureRegistry();
            case "org.bukkit.generator.structure.StructureType":
                return (Registry<T>) LunarArcRegistryEntries.createStructureTypeRegistry();
            case "org.bukkit.block.banner.PatternType":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createPatternTypeRegistry()
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.inventory.meta.trim.TrimMaterial":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createTrimRegistry((Class<? extends Keyed>) type)
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.inventory.meta.trim.TrimPattern":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createTrimRegistry((Class<? extends Keyed>) type)
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.JukeboxSong":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createJukeboxSongRegistry()
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.map.MapCursor$Type":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createMapCursorTypeRegistry()
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.entity.Cat$Type":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                                (Class<? extends Keyed>) type, net.minecraft.core.registries.Registries.CAT_VARIANT)
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.entity.Frog$Variant":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                                (Class<? extends Keyed>) type, net.minecraft.core.registries.Registries.FROG_VARIANT)
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.entity.Villager$Profession":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                                (Class<? extends Keyed>) type, net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION)
                        : LunarArcBukkitRegistry.forType(type);
            case "org.bukkit.entity.Villager$Type":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createKeyedNmsRegistry(
                                (Class<? extends Keyed>) type, net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE)
                        : LunarArcBukkitRegistry.forType(type);
            case "io.papermc.paper.datacomponent.DataComponentType":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
                        holder -> io.papermc.paper.datacomponent.PaperDataComponentType.of(holder));
            case "org.bukkit.GameRule":
                return (Registry<T>) LunarArcBukkitRegistry.lazy(() -> {
                    java.util.List<Keyed> values = new java.util.ArrayList<>();
                    net.minecraft.core.registries.BuiltInRegistries.GAME_RULE.listElements()
                            .forEach(holder -> values.add(new org.bukkit.craftbukkit.CraftGameRule<>(holder)));
                    return values;
                });
            case "org.bukkit.entity.Pig$Variant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.PIG_VARIANT, org.bukkit.craftbukkit.entity.CraftPig.CraftVariant::new);
            case "org.bukkit.entity.Cow$Variant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.COW_VARIANT, org.bukkit.craftbukkit.entity.CraftCow.CraftVariant::new);
            case "org.bukkit.entity.Chicken$Variant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.CHICKEN_VARIANT, org.bukkit.craftbukkit.entity.CraftChicken.CraftVariant::new);
            case "org.bukkit.entity.Cow$SoundVariant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.COW_SOUND_VARIANT, org.bukkit.craftbukkit.entity.CraftCow.CraftSoundVariant::new);
            case "org.bukkit.entity.Pig$SoundVariant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.PIG_SOUND_VARIANT, org.bukkit.craftbukkit.entity.CraftPig.CraftSoundVariant::new);
            case "org.bukkit.entity.Chicken$SoundVariant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.CHICKEN_SOUND_VARIANT, org.bukkit.craftbukkit.entity.CraftChicken.CraftSoundVariant::new);
            case "org.bukkit.entity.Cat$SoundVariant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.CAT_SOUND_VARIANT, org.bukkit.craftbukkit.entity.CraftCat.CraftSoundVariant::new);
            case "io.papermc.paper.entity.poi.PoiType":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.POINT_OF_INTEREST_TYPE, io.papermc.paper.entity.poi.PaperPoiType::new);
            case "org.bukkit.entity.ZombieNautilus$Variant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.ZOMBIE_NAUTILUS_VARIANT, org.bukkit.craftbukkit.entity.CraftZombieNautilus.CraftVariant::new);
            case "org.bukkit.entity.SulfurCube$Archetype":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.SULFUR_CUBE_ARCHETYPE, org.bukkit.craftbukkit.entity.CraftSulfurCube.CraftArchetype::new);
            case "io.papermc.paper.block.pot.PotPatternType":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.DECORATED_POT_PATTERN, io.papermc.paper.block.pot.PaperPotPatternType::new);
            case "io.papermc.paper.dialog.Dialog":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.DIALOG, io.papermc.paper.dialog.PaperDialog::new);
            case "org.bukkit.entity.Wolf$SoundVariant":
                return (Registry<T>) holderRegistry(net.minecraft.core.registries.Registries.WOLF_SOUND_VARIANT, org.bukkit.craftbukkit.entity.CraftWolf.CraftSoundVariant::new);
            case "org.bukkit.entity.Wolf$Variant":
                return type.isInterface()
                        ? (Registry<T>) LunarArcRegistryEntries.createWolfVariantRegistry()
                        : LunarArcBukkitRegistry.forType(type);
            default:
                return LunarArcBukkitRegistry.forType(type);
        }
    }


    /** Lazy Bukkit registry over a data-driven vanilla registry, one Holderable wrapper per registry entry. */
    private static <M> Registry<? extends Keyed> holderRegistry(
            net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<M>> key,
            java.util.function.Function<net.minecraft.core.Holder<M>, ? extends Keyed> wrapper) {
        return LunarArcBukkitRegistry.lazy(() -> {
            java.util.List<Keyed> values = new java.util.ArrayList<>();
            org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry().lookupOrThrow(key).listElements()
                    .forEach(holder -> values.add(wrapper.apply(holder)));
            return values;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<? extends Keyed> createMenuTypeRegistry(Class<? extends Keyed> type) {
        net.minecraft.core.Registry<net.minecraft.world.inventory.MenuType<?>> nms =
                net.minecraft.core.registries.BuiltInRegistries.MENU;
        return LunarArcBukkitRegistry.lazy(() -> {
            java.util.List<Keyed> values = new java.util.ArrayList<>();
            for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.inventory.MenuType<?>>, net.minecraft.world.inventory.MenuType<?>> entry : nms.entrySet()) {
                net.minecraft.resources.Identifier location = entry.getKey().identifier();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(location.getNamespace(), location.getPath());
                values.add(new org.bukkit.craftbukkit.inventory.CraftMenuType<>(key, entry.getValue(), viewClassFor(location.getPath())));
            }
            return values;
        });
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
        return LunarArcBukkitRegistry.lazy(() -> {
            java.util.List<Keyed> values = new java.util.ArrayList<>();
            if (itemRegistry) {
                for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item>, net.minecraft.world.item.Item> entry
                        : net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet()) {
                    net.minecraft.resources.Identifier id = entry.getKey().identifier();
                    values.add(new org.bukkit.craftbukkit.inventory.CraftItemType<>(
                            new org.bukkit.NamespacedKey(id.getNamespace(), id.getPath()), entry.getValue()));
                }
            } else {
                for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.Block>, net.minecraft.world.level.block.Block> entry
                        : net.minecraft.core.registries.BuiltInRegistries.BLOCK.entrySet()) {
                    net.minecraft.resources.Identifier id = entry.getKey().identifier();
                    values.add(new org.bukkit.craftbukkit.block.CraftBlockType<>(
                            new org.bukkit.NamespacedKey(id.getNamespace(), id.getPath()), entry.getValue()));
                }
            }
            return values;
        });
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<? extends Keyed> createPotionEffectTypeRegistry(Class<?> apiType) {
        return LunarArcBukkitRegistry.lazy(() -> {
            java.util.List<Keyed> values = new java.util.ArrayList<>();
            var nmsRegistry = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT;
            for (var location : nmsRegistry.keySet()) {
                net.minecraft.world.effect.MobEffect effect = nmsRegistry.getValue(location);
                if (effect == null) continue;
                values.add(new org.bukkit.craftbukkit.potion.CraftPotionEffectType(
                        new org.bukkit.NamespacedKey(location.getNamespace(), location.getPath()), effect));
            }
            return values;
        });
    }

    private static final java.util.List<RegistryKey<?>> TAGGABLE_KEYS = java.util.List.of(
            RegistryKey.GAME_EVENT,
            RegistryKey.STRUCTURE_TYPE,
            RegistryKey.MOB_EFFECT,
            RegistryKey.BLOCK,
            RegistryKey.ITEM,
            RegistryKey.CAT_VARIANT,
            RegistryKey.FROG_VARIANT,
            RegistryKey.VILLAGER_PROFESSION,
            RegistryKey.VILLAGER_TYPE,
            RegistryKey.MAP_DECORATION_TYPE,
            RegistryKey.MENU,
            RegistryKey.ATTRIBUTE,
            RegistryKey.FLUID,
            RegistryKey.SOUND_EVENT,
            RegistryKey.BIOME,
            RegistryKey.STRUCTURE,
            RegistryKey.TRIM_MATERIAL,
            RegistryKey.TRIM_PATTERN,
            RegistryKey.DAMAGE_TYPE,
            RegistryKey.WOLF_VARIANT,
            RegistryKey.ENCHANTMENT,
            RegistryKey.JUKEBOX_SONG,
            RegistryKey.BANNER_PATTERN,
            RegistryKey.PAINTING_VARIANT,
            RegistryKey.INSTRUMENT,
            RegistryKey.ENTITY_TYPE,
            RegistryKey.PARTICLE_TYPE,
            RegistryKey.POTION,
            RegistryKey.MEMORY_MODULE_TYPE,
            RegistryKey.PIG_VARIANT,
            RegistryKey.COW_VARIANT,
            RegistryKey.CHICKEN_VARIANT,
            RegistryKey.COW_SOUND_VARIANT,
            RegistryKey.PIG_SOUND_VARIANT,
            RegistryKey.CHICKEN_SOUND_VARIANT,
            RegistryKey.CAT_SOUND_VARIANT,
            RegistryKey.POINT_OF_INTEREST_TYPE,
            RegistryKey.WOLF_SOUND_VARIANT);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Keyed> RegistryKey<T> registryKeyFor(Class<T> type) {
        for (RegistryKey<?> key : TAGGABLE_KEYS) {
            if (resolveType((RegistryKey) key) == type) return (RegistryKey<T>) key;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Class<T> resolveType(RegistryKey<T> key) {
        if (key == RegistryKey.GAME_EVENT) return (Class<T>) org.bukkit.GameEvent.class;
        if (key == RegistryKey.STRUCTURE_TYPE) return (Class<T>) org.bukkit.generator.structure.StructureType.class;
        if (key == RegistryKey.MOB_EFFECT) return (Class<T>) org.bukkit.potion.PotionEffectType.class;
        if (key == RegistryKey.BLOCK) return (Class<T>) org.bukkit.block.BlockType.class;
        if (key == RegistryKey.ITEM) return (Class<T>) org.bukkit.inventory.ItemType.class;
        if (key == RegistryKey.CAT_VARIANT) return (Class<T>) org.bukkit.entity.Cat.Type.class;
        if (key == RegistryKey.FROG_VARIANT) return (Class<T>) org.bukkit.entity.Frog.Variant.class;
        if (key == RegistryKey.VILLAGER_PROFESSION) return (Class<T>) org.bukkit.entity.Villager.Profession.class;
        if (key == RegistryKey.VILLAGER_TYPE) return (Class<T>) org.bukkit.entity.Villager.Type.class;
        if (key == RegistryKey.MAP_DECORATION_TYPE) return (Class<T>) org.bukkit.map.MapCursor.Type.class;
        if (key == RegistryKey.MENU) return (Class<T>) org.bukkit.inventory.MenuType.class;
        if (key == RegistryKey.ATTRIBUTE) return (Class<T>) org.bukkit.attribute.Attribute.class;
        if (key == RegistryKey.FLUID) return (Class<T>) org.bukkit.Fluid.class;
        if (key == RegistryKey.SOUND_EVENT) return (Class<T>) org.bukkit.Sound.class;
        if (key == RegistryKey.BIOME) return (Class<T>) org.bukkit.block.Biome.class;
        if (key == RegistryKey.STRUCTURE) return (Class<T>) org.bukkit.generator.structure.Structure.class;
        if (key == RegistryKey.TRIM_MATERIAL) return (Class<T>) org.bukkit.inventory.meta.trim.TrimMaterial.class;
        if (key == RegistryKey.TRIM_PATTERN) return (Class<T>) org.bukkit.inventory.meta.trim.TrimPattern.class;
        if (key == RegistryKey.DAMAGE_TYPE) return (Class<T>) org.bukkit.damage.DamageType.class;
        if (key == RegistryKey.WOLF_VARIANT) return (Class<T>) org.bukkit.entity.Wolf.Variant.class;
        if (key == RegistryKey.ENCHANTMENT) return (Class<T>) org.bukkit.enchantments.Enchantment.class;
        if (key == RegistryKey.JUKEBOX_SONG) return (Class<T>) org.bukkit.JukeboxSong.class;
        if (key == RegistryKey.BANNER_PATTERN) return (Class<T>) org.bukkit.block.banner.PatternType.class;
        if (key == RegistryKey.PAINTING_VARIANT) return (Class<T>) org.bukkit.Art.class;
        if (key == RegistryKey.INSTRUMENT) return (Class<T>) org.bukkit.MusicInstrument.class;
        if (key == RegistryKey.ENTITY_TYPE) return (Class<T>) org.bukkit.entity.EntityType.class;
        if (key == RegistryKey.PARTICLE_TYPE) return (Class<T>) org.bukkit.Particle.class;
        if (key == RegistryKey.POTION) return (Class<T>) org.bukkit.potion.PotionType.class;
        if (key == RegistryKey.MEMORY_MODULE_TYPE) return (Class<T>) org.bukkit.entity.memory.MemoryKey.class;
        if (key == RegistryKey.DATA_COMPONENT_TYPE) return (Class<T>) io.papermc.paper.datacomponent.DataComponentType.class;
        if (key == RegistryKey.GAME_RULE) return (Class<T>) org.bukkit.GameRule.class;
        if (key == RegistryKey.PIG_VARIANT) return (Class<T>) org.bukkit.entity.Pig.Variant.class;
        if (key == RegistryKey.COW_VARIANT) return (Class<T>) org.bukkit.entity.Cow.Variant.class;
        if (key == RegistryKey.CHICKEN_VARIANT) return (Class<T>) org.bukkit.entity.Chicken.Variant.class;
        if (key == RegistryKey.COW_SOUND_VARIANT) return (Class<T>) org.bukkit.entity.Cow.SoundVariant.class;
        if (key == RegistryKey.PIG_SOUND_VARIANT) return (Class<T>) org.bukkit.entity.Pig.SoundVariant.class;
        if (key == RegistryKey.CHICKEN_SOUND_VARIANT) return (Class<T>) org.bukkit.entity.Chicken.SoundVariant.class;
        if (key == RegistryKey.CAT_SOUND_VARIANT) return (Class<T>) org.bukkit.entity.Cat.SoundVariant.class;
        if (key == RegistryKey.POINT_OF_INTEREST_TYPE) return (Class<T>) io.papermc.paper.entity.poi.PoiType.class;
        if (key == RegistryKey.WOLF_SOUND_VARIANT) return (Class<T>) org.bukkit.entity.Wolf.SoundVariant.class;
        if (key == RegistryKey.ZOMBIE_NAUTILUS_VARIANT) return (Class<T>) org.bukkit.entity.ZombieNautilus.Variant.class;
        if (key == RegistryKey.SULFUR_CUBE_ARCHETYPE) return (Class<T>) org.bukkit.entity.SulfurCube.Archetype.class;
        if (key == RegistryKey.DECORATED_POT_PATTERN) return (Class<T>) io.papermc.paper.block.pot.PotPatternType.class;
        if (key == RegistryKey.DIALOG) return (Class<T>) io.papermc.paper.dialog.Dialog.class;
        return null;
    }

}
