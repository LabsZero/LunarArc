package io.lunararcdevs.lunararc.common.server;

import com.google.common.collect.ImmutableMap;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.PrioritizableLifecycleEventType;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import io.papermc.paper.tag.PreFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagLoader;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;

/**
 * Paper's tag lifecycle events. {@code LifecycleEvents.TAGS.preFlatten(key)} runs before a registry's tags are resolved, so plugins can
 * edit the raw entries; {@code postFlatten(key)} runs on the resolved tags. Both run on every tag (re)load.
 */
public final class LunarArcTagEvents {
    private static final Map<RegistryKey<?>, PrioritizableLifecycleEventType.Simple<BootstrapContext, ReloadableRegistrarEvent<PreFlattenTagRegistrar<?>>>> PRE_TYPES = new ConcurrentHashMap<>();
    private static final Map<RegistryKey<?>, PrioritizableLifecycleEventType.Simple<BootstrapContext, ReloadableRegistrarEvent<PostFlattenTagRegistrar<?>>>> POST_TYPES = new ConcurrentHashMap<>();
    private static volatile Map<String, RegistryKey<?>> byDirectory;
    private static volatile ReloadableRegistrarEvent.Cause cause = ReloadableRegistrarEvent.Cause.INITIAL;

    private LunarArcTagEvents() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> PrioritizableLifecycleEventType.Simple<BootstrapContext, ReloadableRegistrarEvent<PreFlattenTagRegistrar<T>>> preFlattenType(RegistryKey<T> key) {
        return (PrioritizableLifecycleEventType.Simple) PRE_TYPES.computeIfAbsent(key,
                k -> new PrioritizableLifecycleEventType.Simple<>("pre-flatten:" + k, BootstrapContext.class));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> PrioritizableLifecycleEventType.Simple<BootstrapContext, ReloadableRegistrarEvent<PostFlattenTagRegistrar<T>>> postFlattenType(RegistryKey<T> key) {
        return (PrioritizableLifecycleEventType.Simple) POST_TYPES.computeIfAbsent(key,
                k -> new PrioritizableLifecycleEventType.Simple<>("post-flatten:" + k, BootstrapContext.class));
    }

    /** True when any plugin registered a tag lifecycle handler, which makes the startup reload worth doing. */
    public static boolean anyHandlers() {
        for (var type : PRE_TYPES.values()) if (type.hasHandlers()) return true;
        for (var type : POST_TYPES.values()) if (type.hasHandlers()) return true;
        return false;
    }

    /** Tag loads after the startup reload report RELOAD instead of INITIAL. */
    public static void startupComplete() {
        cause = ReloadableRegistrarEvent.Cause.RELOAD;
    }

    private static Map<String, RegistryKey<?>> directories() {
        Map<String, RegistryKey<?>> map = byDirectory;
        if (map == null) {
            Map<String, RegistryKey<?>> built = new HashMap<>();
            for (java.lang.reflect.Field field : RegistryKey.class.getFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) || !RegistryKey.class.isAssignableFrom(field.getType())) continue;
                try {
                    RegistryKey<?> constant = (RegistryKey<?>) field.get(null);
                    ResourceKey<? extends net.minecraft.core.Registry<?>> nms = ResourceKey.createRegistryKey(Identifier.parse(constant.key().asString()));
                    built.put(Registries.tagsDirPath(nms), constant);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            byDirectory = map = built;
        }
        return map;
    }

    /** @return the API registry key whose tags live in {@code directory}, only when plugins listen to it */
    public static RegistryKey<?> listenedKey(String directory) {
        RegistryKey<?> key = directories().get(directory);
        if (key == null) return null;
        var pre = PRE_TYPES.get(key);
        var post = POST_TYPES.get(key);
        return (pre != null && pre.hasHandlers()) || (post != null && post.hasHandlers()) ? key : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<Identifier, List<TagLoader.EntryWithSource>> pre(RegistryKey<?> key, Map<Identifier, List<TagLoader.EntryWithSource>> initial) {
        var type = PRE_TYPES.get(key);
        if (type == null || !type.hasHandlers()) return initial;
        PreRegistrar registrar = new PreRegistrar(initial, (RegistryKey) key);
        LunarArcLifecycleEventRunner.fire((PrioritizableLifecycleEventType.Simple) type, new LunarArcReloadableRegistrarEvent<>(registrar, cause));
        return Map.copyOf(registrar.tags);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <M> Map<Identifier, List<M>> post(RegistryKey<?> key, Map<Identifier, List<M>> initial, TagLoader.ElementLookup<M> lookup) {
        var type = POST_TYPES.get(key);
        if (type == null || !type.hasHandlers()) return initial;
        PostRegistrar registrar = new PostRegistrar(initial, (RegistryKey) key, lookup);
        LunarArcLifecycleEventRunner.fire((PrioritizableLifecycleEventType.Simple) type, new LunarArcReloadableRegistrarEvent<>(registrar, cause));
        return Map.copyOf(registrar.tags);
    }

    private static String ownerName() {
        LifecycleEventOwner owner = LunarArcLifecycleEventRunner.currentOwner();
        if (owner == null) return "plugin";
        try {
            return owner.getPluginMeta().getDisplayName();
        } catch (Throwable unavailable) {
            return String.valueOf(owner);
        }
    }

    private static final class PreRegistrar<T> implements PreFlattenTagRegistrar<T> {
        final Map<Identifier, List<TagLoader.EntryWithSource>> tags;
        private final RegistryKey<T> registryKey;

        PreRegistrar(Map<Identifier, List<TagLoader.EntryWithSource>> tags, RegistryKey<T> registryKey) {
            this.tags = new HashMap<>(tags);
            this.registryKey = registryKey;
        }

        @Override
        public RegistryKey<T> registryKey() {
            return registryKey;
        }

        @Override
        public Map<TagKey<T>, Collection<TagEntry<T>>> getAllTags() {
            ImmutableMap.Builder<TagKey<T>, Collection<TagEntry<T>>> builder = ImmutableMap.builderWithExpectedSize(tags.size());
            for (Map.Entry<Identifier, List<TagLoader.EntryWithSource>> entry : tags.entrySet()) {
                builder.put(TagKey.create(registryKey, CraftNamespacedKey.fromMinecraft(entry.getKey())), convert(entry.getValue()));
            }
            return builder.build();
        }

        private List<TagEntry<T>> convert(List<TagLoader.EntryWithSource> nms) {
            List<TagEntry<T>> result = new ArrayList<>(nms.size());
            for (TagLoader.EntryWithSource source : nms) {
                net.minecraft.tags.TagEntry entry = source.entry();
                net.kyori.adventure.key.Key key = CraftNamespacedKey.fromMinecraft(entry.id);
                result.add(entry.tag ? TagEntry.tagEntry(TagKey.create(registryKey, key), entry.required)
                        : TagEntry.valueEntry(TypedKey.create(registryKey, key), entry.required));
            }
            return Collections.unmodifiableList(result);
        }

        private TagLoader.EntryWithSource convert(TagEntry<T> entry) {
            Identifier id = PaperAdventure.asVanilla(entry.key());
            net.minecraft.tags.TagEntry nms;
            if (entry.isTag()) nms = entry.isRequired() ? net.minecraft.tags.TagEntry.tag(id) : net.minecraft.tags.TagEntry.optionalTag(id);
            else nms = entry.isRequired() ? net.minecraft.tags.TagEntry.element(id) : net.minecraft.tags.TagEntry.optionalElement(id);
            return new TagLoader.EntryWithSource(nms, ownerName());
        }

        private List<TagLoader.EntryWithSource> nmsTag(TagKey<T> key, boolean create) {
            Identifier id = PaperAdventure.asVanilla(key.key());
            List<TagLoader.EntryWithSource> tag = tags.get(id);
            if (tag == null) {
                if (!create) throw new NoSuchElementException("Tag " + key + " is not present");
                tag = tags.computeIfAbsent(id, k -> new ArrayList<>());
            }
            return tag;
        }

        @Override
        public boolean hasTag(TagKey<T> tagKey) {
            return tags.containsKey(PaperAdventure.asVanilla(tagKey.key()));
        }

        @Override
        public List<TagEntry<T>> getTag(TagKey<T> tagKey) {
            return convert(nmsTag(tagKey, false));
        }

        @Override
        public void addToTag(TagKey<T> tagKey, Collection<TagEntry<T>> entries) {
            List<TagLoader.EntryWithSource> merged = new ArrayList<>(nmsTag(tagKey, true));
            for (TagEntry<T> entry : entries) merged.add(convert(entry));
            tags.put(PaperAdventure.asVanilla(tagKey.key()), merged);
        }

        @Override
        public void setTag(TagKey<T> tagKey, Collection<TagEntry<T>> entries) {
            List<TagLoader.EntryWithSource> replaced = new ArrayList<>(entries.size());
            for (TagEntry<T> entry : entries) replaced.add(convert(entry));
            tags.put(PaperAdventure.asVanilla(tagKey.key()), List.copyOf(replaced));
        }
    }

    private static final class PostRegistrar<M, T> implements PostFlattenTagRegistrar<T> {
        final Map<Identifier, List<M>> tags;
        private final RegistryKey<T> registryKey;
        private final TagLoader.ElementLookup<M> lookup;

        PostRegistrar(Map<Identifier, List<M>> tags, RegistryKey<T> registryKey, TagLoader.ElementLookup<M> lookup) {
            this.tags = new HashMap<>(tags);
            this.registryKey = registryKey;
            this.lookup = lookup;
        }

        @Override
        public RegistryKey<T> registryKey() {
            return registryKey;
        }

        private Identifier idOf(M element) {
            return ((Holder.Reference<?>) element).key().identifier();
        }

        private TypedKey<T> typed(M element) {
            return TypedKey.create(registryKey, CraftNamespacedKey.fromMinecraft(idOf(element)));
        }

        private M element(TypedKey<T> key) {
            Optional<? extends M> found = lookup.get(PaperAdventure.asVanilla(key.key()), true);
            if (found.isEmpty()) throw new IllegalArgumentException(key + " doesn't exist");
            return found.get();
        }

        private List<TypedKey<T>> convert(List<M> nms) {
            List<TypedKey<T>> result = new ArrayList<>(nms.size());
            for (M element : nms) result.add(typed(element));
            return Collections.unmodifiableList(result);
        }

        @Override
        public Map<TagKey<T>, Collection<TypedKey<T>>> getAllTags() {
            ImmutableMap.Builder<TagKey<T>, Collection<TypedKey<T>>> builder = ImmutableMap.builderWithExpectedSize(tags.size());
            for (Map.Entry<Identifier, List<M>> entry : tags.entrySet()) {
                builder.put(TagKey.create(registryKey, CraftNamespacedKey.fromMinecraft(entry.getKey())), convert(entry.getValue()));
            }
            return builder.build();
        }

        private List<M> nmsTag(TagKey<T> key, boolean create) {
            Identifier id = PaperAdventure.asVanilla(key.key());
            List<M> tag = tags.get(id);
            if (tag == null) {
                if (!create) throw new NoSuchElementException("Tag " + key + " is not present");
                tag = tags.computeIfAbsent(id, k -> new ArrayList<>());
            }
            return tag;
        }

        @Override
        public boolean hasTag(TagKey<T> tagKey) {
            return tags.containsKey(PaperAdventure.asVanilla(tagKey.key()));
        }

        @Override
        public Collection<TypedKey<T>> getTag(TagKey<T> tagKey) {
            return convert(nmsTag(tagKey, false));
        }

        @Override
        public void addToTag(TagKey<T> tagKey, Collection<TypedKey<T>> values) {
            List<M> merged = new ArrayList<>(nmsTag(tagKey, true));
            for (TypedKey<T> key : values) merged.add(element(key));
            tags.put(PaperAdventure.asVanilla(tagKey.key()), merged);
        }

        @Override
        public void setTag(TagKey<T> tagKey, Collection<TypedKey<T>> values) {
            List<M> replaced = new ArrayList<>(values.size());
            for (TypedKey<T> key : values) replaced.add(element(key));
            tags.put(PaperAdventure.asVanilla(tagKey.key()), List.copyOf(replaced));
        }
    }
}
