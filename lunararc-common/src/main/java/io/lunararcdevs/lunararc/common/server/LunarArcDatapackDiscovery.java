package io.lunararcdevs.lunararc.common.server;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackRegistrar;
import io.papermc.paper.datapack.DatapackSource;
import io.papermc.paper.datapack.DiscoveredDatapack;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.bukkit.FeatureFlag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Paper's {@code LifecycleEvents.DATAPACK_DISCOVERY}: plugin bootstrappers hand datapacks to a {@link DatapackRegistrar}.
 * Paper reads them before the world loads; LunarArc's resources are already loaded by then, so the discovered packs are
 * added to the pack repository and one resource reload applies them (recipes, tags, loot, functions, advancements).
 */
public final class LunarArcDatapackDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    public static final PackSource PLUGIN_SOURCE = PackSource.create(
            name -> net.minecraft.network.chat.Component.translatable("pack.nameAndSource", name,
                    net.minecraft.network.chat.Component.literal("plugin")), true);

    private static final Map<String, Pack> PACKS = new LinkedHashMap<>();
    private static final List<String> AUTO_ENABLE = new ArrayList<>();

    private LunarArcDatapackDiscovery() {
    }

    public static Collection<Pack> packs() {
        synchronized (PACKS) {
            return List.copyOf(PACKS.values());
        }
    }

    /** Runs the plugins' discovery handlers; call after the bootstrappers ran. The packs are applied by {@link #apply} once worlds exist. */
    public static void discover() {
        try {
            LunarArcLifecycleEventRunner.fire(LifecycleEvents.DATAPACK_DISCOVERY, new LunarArcRegistrarEvent<DatapackRegistrar>(new Registrar()));
        } catch (Throwable failure) {
            LOGGER.error("Plugin datapack discovery failed", failure);
        }
    }

    /** Selects the discovered packs and reloads resources; needs a loaded overworld, so run it after the worlds loaded. */
    public static void apply(MinecraftServer server) {
        List<String> toEnable;
        synchronized (PACKS) {
            // tag lifecycle handlers also need one reload: the first tag load happened before any plugin could register them
            if (PACKS.isEmpty() && !LunarArcTagEvents.anyHandlers()) {
                LunarArcTagEvents.startupComplete();
                return;
            }
            toEnable = List.copyOf(AUTO_ENABLE);
        }
        if (!io.lunararcdevs.lunararc.common.config.LunarArcConfig.isPluginDataReloadOnStartup()) {
            LunarArcTagEvents.startupComplete();
            LOGGER.info("Plugin datapacks / tag handlers found; they take effect on the next /reload. Set plugin-data.reload-on-startup=true in lunararc.conf to apply them at startup (some mods lose data they only load once at boot).");
            return;
        }
        try {
            var repository = server.getPackRepository();
            repository.reload();
            List<String> selected = new ArrayList<>(repository.getSelectedIds());
            for (String id : toEnable) if (!selected.contains(id) && repository.isAvailable(id)) selected.add(id);
            var reload = server.reloadResources(selected);
            server.managedBlock(reload::isDone);
            reload.join();
            LunarArcTagEvents.startupComplete();
            if (!PACKS.isEmpty()) LOGGER.info("Loaded {} plugin-provided datapack(s): {}", PACKS.size(), String.join(", ", PACKS.keySet()));
        } catch (Throwable failure) {
            LOGGER.error("Could not apply plugin-provided datapacks", failure);
        }
    }

    private static final class Registrar implements DatapackRegistrar {
        private final Map<String, DiscoveredDatapack> discovered = new LinkedHashMap<>();

        @Override
        public boolean hasPackDiscovered(@NotNull String name) {
            return discovered.containsKey(name);
        }

        @Override
        public DiscoveredDatapack getDiscoveredPack(@NotNull String name) {
            return discovered.get(name);
        }

        @Override
        public boolean removeDiscoveredPack(@NotNull String name) {
            DiscoveredDatapack removed = discovered.remove(name);
            if (removed == null) return false;
            synchronized (PACKS) {
                PACKS.remove(removed.getName());
                AUTO_ENABLE.remove(removed.getName());
            }
            return true;
        }

        @Override
        public @NotNull Map<String, DiscoveredDatapack> getDiscoveredPacks() {
            return Collections.unmodifiableMap(discovered);
        }

        @Override
        public @NotNull DiscoveredDatapack discoverPack(@NotNull URI uri, @NotNull String id,
                @NotNull Consumer<Configurer> configurer) throws IOException {
            return discoverPack(ownerMeta(), uri, id, configurer);
        }

        @Override
        public @NotNull DiscoveredDatapack discoverPack(@NotNull Path path, @NotNull String id,
                @NotNull Consumer<Configurer> configurer) throws IOException {
            return discoverPack(ownerMeta(), path, id, configurer);
        }

        @Override
        public @NotNull DiscoveredDatapack discoverPack(@NotNull PluginMeta meta, @NotNull URI uri, @NotNull String id,
                @NotNull Consumer<Configurer> configurer) throws IOException {
            return discoverPack(meta, toPath(uri), id, configurer);
        }

        @Override
        public @NotNull DiscoveredDatapack discoverPack(@NotNull PluginMeta meta, @NotNull Path path, @NotNull String id,
                @NotNull Consumer<Configurer> configurer) throws IOException {
            if (discovered.containsKey(id)) throw new IllegalStateException("A datapack with id '" + id + "' was already discovered");
            Cfg config = new Cfg(meta.getName() + "/" + id);
            configurer.accept(config);

            Pack.ResourcesSupplier supplier = Files.isDirectory(path)
                    ? new PathPackResources.PathResourcesSupplier(path)
                    : path.getFileSystem() == FileSystems.getDefault()
                            ? new FilePackResources.FileResourcesSupplier(path)
                            : new PathPackResources.PathResourcesSupplier(path);
            String packId = meta.getName() + "/" + id;
            PackLocationInfo info = new PackLocationInfo(packId, config.title, PLUGIN_SOURCE, Optional.empty());
            Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.SERVER_DATA,
                    new PackSelectionConfig(false, config.top ? Pack.Position.TOP : Pack.Position.BOTTOM, config.fixed));
            if (pack == null) throw new IOException("'" + path + "' is not a valid datapack (missing or invalid pack.mcmeta)");

            DiscoveredDatapack result = new Discovered(pack);
            discovered.put(id, result);
            synchronized (PACKS) {
                PACKS.put(packId, pack);
                if (config.autoEnable) AUTO_ENABLE.add(packId);
            }
            return result;
        }

        private static PluginMeta ownerMeta() {
            LifecycleEventOwner owner = LunarArcLifecycleEventRunner.currentOwner();
            if (owner == null) throw new IllegalStateException("A datapack can only be discovered from a plugin lifecycle handler");
            return owner.getPluginMeta();
        }

        private static Path toPath(URI uri) throws IOException {
            if ("jar".equals(uri.getScheme())) {
                try {
                    return Path.of(uri);
                } catch (FileSystemNotFoundException notOpen) {
                    // keep the plugin jar's file system open: the pack reads from it for the rest of the run
                    FileSystems.newFileSystem(uri, Map.of());
                    return Path.of(uri);
                }
            }
            return Path.of(uri);
        }
    }

    private static final class Cfg implements DatapackRegistrar.Configurer {
        net.minecraft.network.chat.Component title;
        boolean autoEnable = true;
        boolean top;
        boolean fixed;

        Cfg(String defaultTitle) {
            this.title = net.minecraft.network.chat.Component.literal(defaultTitle);
        }

        @Override
        public DatapackRegistrar.@NotNull Configurer title(@NotNull Component title) {
            this.title = io.papermc.paper.adventure.PaperAdventure.asVanilla(title);
            return this;
        }

        @Override
        public DatapackRegistrar.@NotNull Configurer autoEnableOnServerStart(boolean autoEnable) {
            this.autoEnable = autoEnable;
            return this;
        }

        @Override
        public DatapackRegistrar.@NotNull Configurer position(boolean fixed, Datapack.@NotNull Position position) {
            this.fixed = fixed;
            this.top = position == Datapack.Position.TOP;
            return this;
        }
    }

    private record Discovered(Pack pack) implements DiscoveredDatapack {
        @Override public @NotNull String getName() { return pack.getId(); }
        @Override public @NotNull Component getTitle() { return io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.toAdventure(pack.getTitle()); }
        @Override public @NotNull Component getDescription() { return io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.toAdventure(pack.getDescription()); }
        @Override public boolean isRequired() { return pack.isRequired(); }
        @Override public Datapack.@NotNull Compatibility getCompatibility() { return Datapack.Compatibility.valueOf(pack.getCompatibility().name()); }
        @Override public @NotNull Set<FeatureFlag> getRequiredFeatures() { return io.papermc.paper.datapack.PaperDatapack.toBukkitFeatures(pack.getRequestedFeatures()); }
        @Override public @NotNull DatapackSource getSource() { return DatapackSource.PLUGIN; }
    }
}
