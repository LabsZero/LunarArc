package org.bukkit.craftbukkit.v1_21_R1;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.*;
import org.bukkit.loot.LootTable;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import io.ampznetwork.lunararc.common.server.LunarArcLogger;
import io.papermc.paper.configuration.FeatureFlagConfig;

public class CraftServer implements Server {
    private final MinecraftServer console;
    private final PlayerList playerList;
    private final Logger logger = LunarArcLogger.getLogger("Minecraft");
    private final SimpleCommandMap commandMap = new io.ampznetwork.lunararc.common.server.LunarArcCommandMap(this);
    private final PluginManager pluginManager;
    private final SimplePluginManager simplePluginManager;
    private final ServicesManager servicesManager = new SimpleServicesManager();
    private final UnsafeValues unsafeValues;
    private final Map<UUID, Player> playerCache = new HashMap<>();
    private final Map<NamespacedKey, KeyedBossBar> bossBars = new LinkedHashMap<>();
    /** Bukkit-side views for server data that is ultimately backed by the live Minecraft managers. */
    private final Map<Integer, MapView> mapViews = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger nextMapId = new java.util.concurrent.atomic.AtomicInteger();
    private final Map<NamespacedKey, Recipe> runtimeRecipes = new java.util.concurrent.ConcurrentHashMap<>();
    private final org.bukkit.metadata.MetadataStore<org.bukkit.entity.Entity> entityMetadata = (org.bukkit.metadata.MetadataStore<org.bukkit.entity.Entity>) java.lang.reflect.Proxy.newProxyInstance(
        org.bukkit.metadata.MetadataStore.class.getClassLoader(),
        new Class<?>[] { org.bukkit.metadata.MetadataStore.class },
        new java.lang.reflect.InvocationHandler() {
            private final Map<String, Map<org.bukkit.entity.Entity, List<org.bukkit.metadata.MetadataValue>>> metadata = new java.util.concurrent.ConcurrentHashMap<>();
            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (name.equals("setMetadata")) {
                    org.bukkit.entity.Entity subject = (org.bukkit.entity.Entity) args[0];
                    String key = (String) args[1];
                    org.bukkit.metadata.MetadataValue value = (org.bukkit.metadata.MetadataValue) args[2];
                    metadata.computeIfAbsent(key, k -> new java.util.concurrent.ConcurrentHashMap<>())
                            .computeIfAbsent(subject, s -> new java.util.concurrent.CopyOnWriteArrayList<>())
                            .add(value);
                    return null;
                } else if (name.equals("getMetadata")) {
                    org.bukkit.entity.Entity subject = (org.bukkit.entity.Entity) args[0];
                    String key = (String) args[1];
                    Map<org.bukkit.entity.Entity, List<org.bukkit.metadata.MetadataValue>> keyMap = metadata.get(key);
                    return keyMap != null ? keyMap.getOrDefault(subject, Collections.emptyList()) : Collections.emptyList();
                } else if (name.equals("hasMetadata")) {
                    org.bukkit.entity.Entity subject = (org.bukkit.entity.Entity) args[0];
                    String key = (String) args[1];
                    Map<org.bukkit.entity.Entity, List<org.bukkit.metadata.MetadataValue>> keyMap = metadata.get(key);
                    return keyMap != null && keyMap.containsKey(subject);
                } else if (name.equals("removeMetadata")) {
                    org.bukkit.entity.Entity subject = (org.bukkit.entity.Entity) args[0];
                    String key = (String) args[1];
                    Map<org.bukkit.entity.Entity, List<org.bukkit.metadata.MetadataValue>> keyMap = metadata.get(key);
                    if (keyMap != null) keyMap.remove(subject);
                    return null;
                }
                return null;
            }
        }
    );

    public org.bukkit.metadata.MetadataStore<org.bukkit.entity.Entity> getEntityMetadata() {
        return entityMetadata;
    }

    private final YamlConfiguration bukkitConfig = new YamlConfiguration();
    private final YamlConfiguration spigotConfig = new YamlConfiguration();
    private final YamlConfiguration commandsConfig = new YamlConfiguration();
    private final YamlConfiguration paperGlobalConfig = new YamlConfiguration();
    private final YamlConfiguration paperWorldConfig = new YamlConfiguration();

    private final Spigot spigot = new Server.Spigot() {
        @Override
        public YamlConfiguration getConfig() {
            return spigotConfig;
        }
    };
    private final StandardMessenger messenger = new StandardMessenger();
    private final org.bukkit.craftbukkit.v1_21_R1.scheduler.CraftScheduler scheduler = new org.bukkit.craftbukkit.v1_21_R1.scheduler.CraftScheduler();
    private final ItemFactory itemFactory;
    private final HelpMap helpMap = new HelpMap() {
        @Override
        public @Nullable org.bukkit.help.HelpTopic getHelpTopic(@NotNull String cmdName) {
            return null;
        }

        @Override
        public @NotNull Collection<org.bukkit.help.HelpTopic> getHelpTopics() {
            return Collections.emptyList();
        }

        @Override
        public void addTopic(@NotNull org.bukkit.help.HelpTopic topic) {
        }

        @Override
        public void clear() {
        }

        @Override
        public void registerHelpTopicFactory(@NotNull Class<?> commandClass,
                @NotNull org.bukkit.help.HelpTopicFactory<?> factory) {
        }

        @Override
        public @NotNull List<String> getIgnoredPlugins() {
            return Collections.emptyList();
        }
    };

    private final CraftConsoleCommandSender consoleSender;

    public CraftServer(MinecraftServer console, PlayerList playerList) {
        this.console = console;
        this.playerList = playerList;
        this.consoleSender = new CraftConsoleCommandSender(console);
        
        logger.info("[LunarArc] CraftServer initialized: " + getName() + " version " + getVersion() + " (Bukkit: " + getBukkitVersion() + ")");
        
        this.simplePluginManager = new SimplePluginManager(this, commandMap);
        this.pluginManager = new io.papermc.paper.plugin.manager.PaperPluginManagerImpl(this, commandMap, simplePluginManager);
        this.simplePluginManager.setInternalManager(this.pluginManager);

        this.simplePluginManager.registerInterface(io.ampznetwork.lunararc.common.server.LunarArcPluginLoader.class);

        this.unsafeValues = (UnsafeValues) java.lang.reflect.Proxy.newProxyInstance(
                UnsafeValues.class.getClassLoader(),
                new Class<?>[] { UnsafeValues.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getVersionFetcher")) {
                        // VersionFetcher was removed in Paper 1.19+; return via reflection to avoid compile-time dep
                        try {
                            Class<?> vfClass = Class.forName("com.destroystokyo.paper.util.VersionFetcher");
                            return java.lang.reflect.Proxy.newProxyInstance(vfClass.getClassLoader(),
                                new Class<?>[]{ vfClass },
                                (vp, vm, va) -> {
                                    if (vm.getName().equals("getCacheTime")) return 0L;
                                    return net.kyori.adventure.text.Component.text(
                                        io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.projectName());
                                });
                        } catch (ClassNotFoundException ignored) {
                            return null;
                        }
                    }
                    if (method.getName().equals("getDataVersion"))
                        return io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.dataVersion().orElse(0);
                    if (method.getName().equals("processClass"))
                        return args[2]; // Return class bytes unmodified
                    if (method.getName().equals("fromLegacy")) {
                        if (args != null && args.length > 0 && args[0] instanceof Material leg) {
                            if (leg == Material.AIR)
                                return Material.AIR;
                            try {
                                String name = leg.name().replace("LEGACY_", "");
                                return Material.valueOf(name);
                            } catch (Exception e) {
                                return Material.STONE;
                            }
                        }
                    }
                    if (method.getName().equals("getMaterial")) {
                        if (args != null && args.length > 0 && args[0] instanceof String name) {
                            try {
                                return Material.valueOf(name);
                            } catch (Exception e) {
                                return null;
                            }
                        }
                    }
                    if (method.getName().equals("isItem")) {
                        if (args != null && args.length > 0 && args[0] instanceof Material mat) {
                            if (mat.name().startsWith("LEGACY_")) return false;
                            try {
                                net.minecraft.resources.ResourceLocation rl =
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                        mat.getKey().getNamespace(), mat.getKey().getKey());
                                return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl);
                            } catch (Exception e) {
                                return true;
                            }
                        }
                        return false;
                    }
                    if (method.getName().equals("isBlock")) {
                        if (args != null && args.length > 0 && args[0] instanceof Material mat) {
                            if (mat.name().startsWith("LEGACY_")) return false;
                            try {
                                net.minecraft.resources.ResourceLocation rl =
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                        mat.getKey().getNamespace(), mat.getKey().getKey());
                                return net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(rl);
                            } catch (Exception e) {
                                return true;
                            }
                        }
                        return false;
                    }
                    if (method.getName().equals("createDamageSourceBuilder")) {
                        final org.bukkit.damage.DamageType dmgType = (args != null && args.length > 0
                            && args[0] instanceof org.bukkit.damage.DamageType dt) ? dt : null;
                        try {
                            Class<?> builderClass = Class.forName("org.bukkit.damage.DamageSource$Builder");
                            org.bukkit.damage.DamageSource sourceProxy = (org.bukkit.damage.DamageSource)
                                java.lang.reflect.Proxy.newProxyInstance(
                                    org.bukkit.damage.DamageSource.class.getClassLoader(),
                                    new Class<?>[] { org.bukkit.damage.DamageSource.class },
                                    (p2, m2, a2) -> {
                                        if (m2.getName().equals("getDamageType")) return dmgType;
                                        if (m2.getReturnType().equals(boolean.class)) return false;
                                        if (m2.getReturnType().equals(float.class)) return 0.0f;
                                        return null;
                                    });
                            return java.lang.reflect.Proxy.newProxyInstance(
                                builderClass.getClassLoader(),
                                new Class<?>[] { builderClass },
                                (p2, m2, a2) -> {
                                    if (m2.getName().equals("build")) return sourceProxy;
                                    if (builderClass.isAssignableFrom(m2.getReturnType())) return p2;
                                    return null;
                                });
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    if (method.getReturnType().equals(boolean.class))
                        return false;
                    if (method.getReturnType().equals(int.class))
                        return 0;
                    if (method.getReturnType().equals(byte[].class))
                        return new byte[0];
                    if (method.getReturnType().equals(Material.class))
                        return Material.STONE;
                    return null;
                });

        // Scheduler initialized inline

        this.itemFactory = (ItemFactory) java.lang.reflect.Proxy.newProxyInstance(
                ItemFactory.class.getClassLoader(),
                new Class<?>[] { ItemFactory.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getItemMeta" -> new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemMeta();
                    case "isApplicable" -> true;
                    case "equals" -> {
                        // Both empty metas are equal
                        if (args == null || args.length < 2) yield false;
                        yield args[0] == null ? args[1] == null : args[0].equals(args[1]);
                    }
                    case "asMetaFor" -> args != null && args.length > 0 ? args[0] : null;
                    case "getDefaultMeta" -> new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemMeta();
                    default -> {
                        if (method.getReturnType().equals(boolean.class)) yield false;
                        if (method.getReturnType().equals(int.class)) yield 0;
                        yield null;
                    }
                });

        this.loadConfigurations();

        Bukkit.setServer(this);

        // SimpleCommandMap may already contain Bukkit's stock version command. A
        // second register() only creates a namespaced fallback, leaving /version
        // pointed at the original command. Explicitly unregister it first so the
        // visible command gains LunarArc branding while all version APIs remain Paper.
        org.bukkit.command.Command existingVersion = commandMap.getCommand("version");
        if (existingVersion != null) {
            existingVersion.unregister(commandMap);
            commandMap.getKnownCommands().entrySet().removeIf(e -> e.getValue() == existingVersion);
        }
        commandMap.register("bukkit", new io.ampznetwork.lunararc.common.server.LunarArcVersionCommand("version"));

        org.bukkit.command.Command existingPlugins = commandMap.getCommand("plugins");
        if (existingPlugins != null) {
            existingPlugins.unregister(commandMap);
            commandMap.getKnownCommands().entrySet().removeIf(e -> e.getValue() == existingPlugins);
        }
        commandMap.register("bukkit", new org.bukkit.command.defaults.PluginsCommand("plugins"));
        commandMap.register("bukkit", new org.bukkit.command.defaults.ReloadCommand("reload"));
    }

    private void loadConfigurations() {
        File bukkitFile = new File("bukkit.yml");
        File spigotFile = new File("spigot.yml");
        File commandsFile = new File("commands.yml");
        File paperDir = new File("config");
        if (!paperDir.exists())
            paperDir.mkdirs();
        File paperGlobalFile = new File(paperDir, "paper-global.yml");
        File paperWorldFile = new File(paperDir, "paper-world-defaults.yml");

        try {
            if (bukkitFile.exists())
                bukkitConfig.load(bukkitFile);
            if (spigotFile.exists())
                spigotConfig.load(spigotFile);
            if (commandsFile.exists())
                commandsConfig.load(commandsFile);
            if (paperGlobalFile.exists())
                paperGlobalConfig.load(paperGlobalFile);
            if (paperWorldFile.exists())
                paperWorldConfig.load(paperWorldFile);

            // Bukkit Defaults
            bukkitConfig.set("settings.allow-end", true);
            bukkitConfig.set("settings.warn-on-overload", true);
            bukkitConfig.set("settings.permissions-file", "permissions.yml");
            bukkitConfig.set("settings.update-checker", true);
            bukkitConfig.set("settings.plugin-profiling", false);
            bukkitConfig.set("settings.connection-throttle", 4000);
            bukkitConfig.set("settings.query-plugins", true);
            bukkitConfig.set("settings.deprecated-verbose", "default");
            bukkitConfig.set("settings.shutdown-message", "Server closed");
            bukkitConfig.set("spawn-limits.monsters", 70);
            bukkitConfig.set("spawn-limits.animals", 10);
            bukkitConfig.set("spawn-limits.water-animals", 5);
            bukkitConfig.set("spawn-limits.ambient", 15);
            bukkitConfig.set("chunk-gc.period-in-ticks", 600);
            bukkitConfig.set("ticks-per.animal-spawns", 400);
            bukkitConfig.set("ticks-per.monster-spawns", 1);
            bukkitConfig.set("aliases", "noworld");
            bukkitConfig.save(bukkitFile);

            // Spigot Defaults
            spigotConfig.set("settings.debug", false);
            spigotConfig.set("settings.bungeecord", false);
            spigotConfig.set("settings.sample-count", 12);
            spigotConfig.set("settings.player-shuffle", 0);
            spigotConfig.set("settings.user-cache-size", 1000);
            spigotConfig.set("settings.save-user-cache-on-stop-only", false);
            spigotConfig.set("settings.moved-wrongly-threshold", 0.0625);
            spigotConfig.set("settings.moved-too-quickly-multiplier", 10.0);
            spigotConfig.set("settings.timeout-time", 60);
            spigotConfig.set("settings.restart-on-crash", true);
            spigotConfig.set("settings.restart-script", "./start.sh");
            spigotConfig.set("settings.netty-threads", 4);
            spigotConfig.set("settings.attribute.maxHealth.max", 2048.0);
            spigotConfig.set("settings.attribute.movementSpeed.max", 2048.0);
            spigotConfig.set("settings.attribute.attackDamage.max", 2048.0);
            spigotConfig.set("settings.log-villager-deaths", true);
            spigotConfig.set("settings.log-named-deaths", true);
            spigotConfig.set("messages.whitelist", "You are not whitelisted on this server!");
            spigotConfig.set("messages.unknown-command", "Unknown command. Type \"/help\" for help.");
            spigotConfig.set("messages.server-full", "The server is full!");
            spigotConfig.set("messages.outdated-client", "Outdated client! Please use {0}");
            spigotConfig.set("messages.outdated-server", "Outdated server! I'm still on {0}");
            spigotConfig.set("messages.restart", "Server is restarting");
            spigotConfig.set("advancements.disable-saving", false);
            spigotConfig.set("advancements.disabled", Collections.singletonList("minecraft:story/disabled"));
            spigotConfig.save(spigotFile);

            // Commands Defaults
            commandsConfig.set("command-block-overrides", Collections.emptyList());
            commandsConfig.set("aliases.icanhasbukkit", Collections.singletonList("version"));
            commandsConfig.save(commandsFile);

            // Paper Global Defaults
            paperGlobalConfig.set("proxies.bungee-cord.enabled", false);
            paperGlobalConfig.set("proxies.velocity.enabled", false);
            paperGlobalConfig.set("proxies.velocity.online-mode", false);
            paperGlobalConfig.set("proxies.velocity.secret", "");
            paperGlobalConfig.set("settings.chunk-loading.min-loadable-tick-rate", 1);
            paperGlobalConfig.set("settings.incoming-packet-spam-threshold", 300);
            paperGlobalConfig.save(paperGlobalFile);

            // Paper World Defaults
            paperWorldConfig.set("anticheat.obfuscation.items.enabled", false);
            paperWorldConfig.set("entities.spawning.despawn-ranges.ambient.hard", 128);
            paperWorldConfig.set("entities.spawning.despawn-ranges.ambient.soft", 32);
            paperWorldConfig.save(paperWorldFile);

        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to load/generate server configuration files", e);
        }
    }

    public MinecraftServer getServer() {
        return console;
    }

    public MinecraftServer getHandle() {
        return console;
    }

    @Override
    public @NotNull String getName() {
        return "Paper";
    }

    @Override
    public @NotNull String getVersion() {
        // Must match Paper's "git-Paper-NNN (MC: X.Y.Z)" format so that plugins
        // like WorldEdit can extract the MC version via the "(MC: ...)" pattern.
        return io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.projectVersion();
    }

    @Override
    public @NotNull String getMinecraftVersion() {
        return io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.minecraftVersion();
    }

    @Override
    public @NotNull String getBukkitVersion() {
        return io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.paperApiVersion();
    }

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public @NotNull PluginManager getPluginManager() {
        return simplePluginManager;
    }

    public Plugin[] getPlugins() {
        return pluginManager.getPlugins();
    }

    @Override
    public @NotNull ServicesManager getServicesManager() {
        return servicesManager;
    }

    private final Map<UUID, World> worldCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, CraftWorld> worldByDimension = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Canonical CraftWorld wrapper. Paper keeps one CraftWorld object for the
     * lifetime of a loaded ServerLevel; plugins rely on object/UUID identity.
     */
    private CraftWorld craftWorld(net.minecraft.server.level.ServerLevel level) {
        CraftWorld craft = worldByDimension.computeIfAbsent(level.dimension(), ignored -> new CraftWorld(level));
        worldCache.putIfAbsent(craft.getUID(), craft);
        // Compatibility alias for locations written by older LunarArc builds.
        worldCache.putIfAbsent(craft.getLegacyDimensionUID(), craft);
        return craft;
    }

    @Override
    public @NotNull Collection<? extends Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (net.minecraft.server.level.ServerPlayer player : playerList.getPlayers()) {
            Player cp = getPlayer(player.getUUID());
            if (cp != null)
                players.add(cp);
        }
        return players;
    }

    @Override
    public @NotNull List<World> getWorlds() {
        List<World> worlds = new ArrayList<>();
        for (net.minecraft.server.level.ServerLevel level : console.getAllLevels()) {
            World world = getWorld(level.dimension().location().toString());
            if (world != null)
                worlds.add(world);
        }
        return worlds;
    }

    @Override
    public @NotNull ConsoleCommandSender getConsoleSender() {
        return consoleSender;
    }

    @Override
    public @NotNull org.bukkit.command.CommandMap getCommandMap() {
        return commandMap;
    }

    public void loadPlugins() {
        logger.info("[LunarArc] Scanning plugins folder...");
        File pluginsFolder = new File("plugins");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }

        File[] files = pluginsFolder.listFiles(f -> f.getName().endsWith(".jar"));
        if (files != null) {
            logger.info("[LunarArc] Found " + files.length + " potential plugins. Loading...");
            simplePluginManager.loadPlugins(files);
        }
    }

    public void enablePlugins(org.bukkit.plugin.PluginLoadOrder type) {
        logger.info("[LunarArc] Enabling Bukkit plugins (Order: " + type + ")...");
        simplePluginManager.enablePlugins(type);
        if (type == org.bukkit.plugin.PluginLoadOrder.POSTWORLD || type == null) {
            syncCommands();
        }
    }

    public void syncCommands() {
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = console.getCommands().getDispatcher();
        if (commandMap instanceof io.ampznetwork.lunararc.common.server.LunarArcCommandMap lunarArcMap) {
            lunarArcMap.syncToBrigadier(dispatcher);
        }
        for (net.minecraft.server.level.ServerPlayer player : console.getPlayerList().getPlayers()) {
            console.getCommands().sendCommands(player);
        }
    }

    @Override
    public int getMaxPlayers() {
        return playerList.getMaxPlayers();
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        if (maxPlayers < 0) throw new IllegalArgumentException("maxPlayers must be >= 0");
        try {
            java.lang.reflect.Field field = PlayerList.class.getDeclaredField("maxPlayers");
            field.setAccessible(true);
            field.setInt(playerList, maxPlayers);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to update max players", ex);
        }
    }

    @Override
    public int getPort() {
        try {
            Object value = console.getClass().getMethod("getPort").invoke(console);
            if (value instanceof Integer port) {
                return port;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return -1;
    }


    private Object dedicatedProperties() throws ReflectiveOperationException {
        return console.getClass().getMethod("getProperties").invoke(console);
    }

    private int readDedicatedIntProperty(String fieldName, int fallback) {
        try {
            Object properties = dedicatedProperties();
            java.lang.reflect.Field field = properties.getClass().getField(fieldName);
            Object value = field.get(properties);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private boolean readDedicatedBooleanProperty(String fieldName, boolean fallback) {
        try {
            Object properties = dedicatedProperties();
            java.lang.reflect.Field field = properties.getClass().getField(fieldName);
            Object value = field.get(properties);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private String readDedicatedStringProperty(String fieldName, String fallback) {
        try {
            Object properties = dedicatedProperties();
            java.lang.reflect.Field field = properties.getClass().getField(fieldName);
            Object value = field.get(properties);
            return value == null ? fallback : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    @Override
    public long getConnectionThrottle() {
        return bukkitConfig.getLong("settings.connection-throttle", 4000L);
    }

    @Override
    public int getViewDistance() {
        return readDedicatedIntProperty("viewDistance", 10);
    }

    @Override
    public int getSimulationDistance() {
        return readDedicatedIntProperty("simulationDistance", 10);
    }

    @Override
    public @NotNull String getIp() {
        try {
            Object value = console.getClass().getMethod("getLocalIp").invoke(console);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    @Override
    public @NotNull String getWorldType() {
        String configured = readDedicatedStringProperty("levelType", null);
        if (configured != null) return configured;
        try {
            Object properties = dedicatedProperties();
            Object raw = properties.getClass().getField("properties").get(properties);
            if (raw instanceof java.util.Properties props) return props.getProperty("level-type", "minecraft:normal");
        } catch (ReflectiveOperationException ignored) {}
        return "minecraft:normal";
    }

    @Override
    public boolean getGenerateStructures() {
        try {
            return console.getWorldData().worldGenOptions().generateStructures();
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public int getSpawnRadius() {
        return readDedicatedIntProperty("spawnProtection", 16);
    }

    @Override
    public void setSpawnRadius(int value) {
    }

    @Override
    public boolean isHardcore() {
        try {
            return console.getWorldData().isHardcore();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean getAllowFlight() {
        try {
            return console.isFlightAllowed();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean getOnlineMode() {
        return console.usesAuthentication();
    }

    @Override
    public boolean getHideOnlinePlayers() {
        return readDedicatedBooleanProperty("hideOnlinePlayers", false);
    }

    @Override
    public boolean getAllowNether() {
        return readDedicatedBooleanProperty("allowNether", true);
    }

    @Override
    public boolean getAllowEnd() {
        return bukkitConfig.getBoolean("settings.allow-end", true);
    }

    @Override
    public boolean hasWhitelist() {
        return playerList.isUsingWhitelist();
    }

    @Override
    public void setWhitelist(boolean value) {
        playerList.setUsingWhiteList(value);
    }

    @Override
    public boolean isWhitelistEnforced() {
        try {
            Object value = console.getClass().getMethod("isEnforceWhitelist").invoke(console);
            if (value instanceof Boolean bool) return bool;
        } catch (ReflectiveOperationException ignored) {}
        return readDedicatedBooleanProperty("enforceWhitelist", false);
    }

    @Override
    public void setWhitelistEnforced(boolean value) {
        try {
            console.getClass().getMethod("setEnforceWhitelist", boolean.class).invoke(console, value);
            return;
        } catch (ReflectiveOperationException ignored) {}
        throw new UnsupportedOperationException("This Minecraft server does not expose runtime whitelist-enforcement mutation");
    }

    @Override
    public @NotNull Set<OfflinePlayer> getWhitelistedPlayers() {
        Set<OfflinePlayer> result = new LinkedHashSet<>();
        try {
            Object whiteList = playerList.getWhiteList();
            Object entries = whiteList.getClass().getMethod("getEntries").invoke(whiteList);
            if (entries instanceof String[] ids) {
                for (String raw : ids) {
                    try { result.add(getOfflinePlayer(UUID.fromString(raw))); } catch (IllegalArgumentException ignored) {}
                }
            } else if (entries instanceof Collection<?> collection) {
                for (Object raw : collection) {
                    if (raw instanceof String id) {
                        try { result.add(getOfflinePlayer(UUID.fromString(id))); } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {
            for (OfflinePlayer player : getOfflinePlayers()) {
                if (player.isWhitelisted()) result.add(player);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public void reloadWhitelist() {
        playerList.reloadWhiteList();
    }

    @Override
    public int broadcastMessage(@NotNull String message) {
        int count = 0;
        for (Player p : getOnlinePlayers()) { p.sendMessage(message); count++; }
        getConsoleSender().sendMessage(message);
        return count;
    }

    @Override
    public int broadcast(@NotNull String message, @NotNull String permission) {
        int count = 0;
        for (Player p : getOnlinePlayers()) {
            if (p.hasPermission(permission)) { p.sendMessage(message); count++; }
        }
        return count;
    }

    @Override
    public int broadcast(@NotNull net.kyori.adventure.text.Component message, @NotNull String permission) {
        int count = 0;
        for (Player p : getOnlinePlayers()) {
            if (p.hasPermission(permission)) { p.sendMessage(message); count++; }
        }
        return count;
    }

    @Override
    public int broadcast(@NotNull net.kyori.adventure.text.Component message) {
        int count = 0;
        for (Player p : getOnlinePlayers()) { p.sendMessage(message); count++; }
        getConsoleSender().sendMessage(message);
        return count;
    }

    @Override
    public @Nullable Player getPlayer(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        Player exact = getPlayerExact(name);
        if (exact != null) return exact;

        String lower = name.toLowerCase(Locale.ROOT);
        Player best = null;
        int bestDelta = Integer.MAX_VALUE;
        for (Player player : getOnlinePlayers()) {
            String candidate = player.getName();
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                int delta = Math.abs(candidate.length() - name.length());
                if (delta < bestDelta) {
                    best = player;
                    bestDelta = delta;
                    if (delta == 0) break;
                }
            }
        }
        return best;
    }

    @Override
    public @Nullable Player getPlayerExact(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        net.minecraft.server.level.ServerPlayer player = playerList.getPlayerByName(name);
        return player != null ? getPlayer(player.getUUID()) : null;
    }

    @Override
    public @NotNull List<Player> matchPlayer(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        List<Player> matches = new ArrayList<>();
        String lower = name.toLowerCase(Locale.ROOT);
        for (Player player : getOnlinePlayers()) {
            String candidate = player.getName();
            if (candidate.equalsIgnoreCase(name)) return Collections.singletonList(player);
            if (candidate.toLowerCase(Locale.ROOT).contains(lower)) matches.add(player);
        }
        return matches;
    }

    @Override
    public @Nullable Player getPlayer(@NotNull UUID id) {
        net.minecraft.server.level.ServerPlayer player = playerList.getPlayer(id);
        if (player == null) {
            playerCache.remove(id);
            return null;
        }
        return playerCache.computeIfAbsent(id, k -> {
            try {
                return new CraftPlayer(this, player);
            } catch (Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to initialize CraftPlayer for "
                        + player.getName().getString() + " (Is CraftPlayerMixin registered?)", e);
                return null;
            }
        });
    }

    @Override
    public @Nullable World getWorld(@NotNull String name) {
        if (name == null || name.isBlank()) return null;

        // Paper resolves Bukkit world names, not just minecraft dimension paths.
        // Search live worlds first so mod dimensions (Aether, Twilight, etc.) are
        // addressable by the exact name exposed by CraftWorld#getName().
        for (net.minecraft.server.level.ServerLevel level : console.getAllLevels()) {
            CraftWorld craft = craftWorld(level);
            if (craft.getName().equalsIgnoreCase(name)
                    || level.dimension().location().toString().equalsIgnoreCase(name)
                    || level.dimension().location().getPath().equalsIgnoreCase(name)) {
                return craft;
            }
        }

        String qualified = switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "world" -> "minecraft:overworld";
            case "world_nether" -> "minecraft:the_nether";
            case "world_the_end" -> "minecraft:the_end";
            default -> name.contains(":") ? name : "minecraft:" + name;
        };
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(qualified);
        if (rl == null) return null;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key = net.minecraft.resources.ResourceKey
                .create(net.minecraft.core.registries.Registries.DIMENSION, rl);
        net.minecraft.server.level.ServerLevel level = console.getLevel(key);
        return level == null ? null : craftWorld(level);
    }

    @Override
    public @Nullable World getWorld(@NotNull UUID uid) {
        World cached = worldCache.get(uid);
        if (cached != null) return cached;
        for (net.minecraft.server.level.ServerLevel level : console.getAllLevels()) {
            CraftWorld world = craftWorld(level);
            if (world.getUID().equals(uid) || world.getLegacyDimensionUID().equals(uid)) return world;
        }
        return null;
    }

    @Override
    public @Nullable World getWorld(@NotNull net.kyori.adventure.key.Key key) {
        return getWorld(key.namespace() + ":" + key.value());
    }

    @Override
    public @NotNull MapView createMap(@NotNull World world) {
        Objects.requireNonNull(world, "world");
        final int id = nextMapId.getAndIncrement();
        final class MapState {
            World currentWorld = world;
            int centerX = 0;
            int centerZ = 0;
            MapView.Scale scale = MapView.Scale.NORMAL;
            boolean trackingPosition = true;
            boolean unlimitedTracking = false;
            boolean locked = false;
            final List<org.bukkit.map.MapRenderer> renderers = new java.util.concurrent.CopyOnWriteArrayList<>();
        }
        final MapState state = new MapState();
        MapView view = (MapView) java.lang.reflect.Proxy.newProxyInstance(
                MapView.class.getClassLoader(), new Class<?>[] { MapView.class }, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getId" -> id;
                        case "isVirtual" -> !state.renderers.isEmpty();
                        case "getScale" -> state.scale;
                        case "setScale" -> { state.scale = (MapView.Scale) args[0]; yield null; }
                        case "getCenterX" -> state.centerX;
                        case "getCenterZ" -> state.centerZ;
                        case "setCenterX" -> { state.centerX = (Integer) args[0]; yield null; }
                        case "setCenterZ" -> { state.centerZ = (Integer) args[0]; yield null; }
                        case "getWorld" -> state.currentWorld;
                        case "setWorld" -> { state.currentWorld = (World) args[0]; yield null; }
                        case "isTrackingPosition" -> state.trackingPosition;
                        case "setTrackingPosition" -> { state.trackingPosition = (Boolean) args[0]; yield null; }
                        case "isUnlimitedTracking" -> state.unlimitedTracking;
                        case "setUnlimitedTracking" -> { state.unlimitedTracking = (Boolean) args[0]; yield null; }
                        case "isLocked" -> state.locked;
                        case "setLocked" -> { state.locked = (Boolean) args[0]; yield null; }
                        case "getRenderers" -> Collections.unmodifiableList(new ArrayList<>(state.renderers));
                        case "addRenderer" -> { state.renderers.add((org.bukkit.map.MapRenderer) args[0]); yield null; }
                        case "removeRenderer" -> state.renderers.remove(args[0]);
                        case "toString" -> "LunarArcMapView{id=" + id + ",world=" + state.currentWorld.getName() + "}";
                        case "hashCode" -> id;
                        case "equals" -> proxy == args[0];
                        default -> defaultValue(method.getReturnType());
                    };
                });
        mapViews.put(id, view);
        return view;
    }

    @Override
    public @Nullable MapView getMap(int id) {
        return mapViews.get(id);
    }

    @Override
    public void reload() {
    }

    @Override
    public void reloadData() {
        Object repository = invokeNoArg(console, "getPackRepository");
        Object selected = repository == null ? null : invokeNoArg(repository, "getSelectedIds");
        if (!(selected instanceof Collection<?> packs)) {
            selected = repository == null ? null : invokeNoArg(repository, "getSelectedPacks");
            if (selected instanceof Collection<?> selectedPacks) {
                List<String> ids = new ArrayList<>();
                for (Object pack : selectedPacks) {
                    Object id = invokeNoArg(pack, "getId");
                    if (id == null) id = invokeNoArg(pack, "getPackId");
                    if (id != null) ids.add(String.valueOf(id));
                }
                selected = ids;
            }
        }
        if (selected instanceof Collection<?> packs) {
            Object future = invokeCompatible(console, "reloadResources", packs);
            if (future instanceof java.util.concurrent.CompletableFuture<?> completable) completable.join();
        }
    }

    @Override
    public void savePlayers() {
        playerList.saveAll();
    }

    @Override
    public void shutdown() {
        console.halt(false);
    }

    @Override
    public boolean isPrimaryThread() {
        return console.isSameThread();
    }

    @Override
    public @NotNull String getMotd() {
        return console.getMotd();
    }

    @Override
    public @Nullable String getShutdownMessage() {
        return "Server closed";
    }

    @Override
    public @NotNull Spigot spigot() {
        return spigot;
    }

    @Override
    public @Nullable BukkitScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public @NotNull Messenger getMessenger() {
        return messenger;
    }

    @Override
    public void sendPluginMessage(@NotNull org.bukkit.plugin.Plugin source, @NotNull String channel, byte[] message) {
        StandardMessenger.validatePluginMessage(messenger, source, channel, message);
        for (Player player : getOnlinePlayers()) {
            player.sendPluginMessage(source, channel, message);
        }
    }

    @Override
    public @NotNull Set<String> getListeningPluginChannels() {
        Set<String> channels = new HashSet<>();
        for (Player player : getOnlinePlayers()) {
            channels.addAll(player.getListeningPluginChannels());
        }
        return Collections.unmodifiableSet(channels);
    }

    @Override
    public @NotNull HelpMap getHelpMap() {
        return helpMap;
    }

    @Override
    public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, @NotNull InventoryType type) {
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory(owner, type);
    }

    @Override
    public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, @NotNull InventoryType type,
            @NotNull String title) {
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory(
                owner, type.getDefaultSize(), type, net.kyori.adventure.text.Component.text(title));
    }

    @Override
    public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, @NotNull InventoryType type,
            @NotNull net.kyori.adventure.text.Component title) {
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory(
                owner, type.getDefaultSize(), type, title);
    }

    @Override
    public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, int size)
            throws IllegalArgumentException {
        if (size <= 0 || size % 9 != 0) throw new IllegalArgumentException("size must be positive multiple of 9");
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory(
                owner, size, net.kyori.adventure.text.Component.text("Chest"));
    }

    @Override
    public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, int size, @NotNull String title)
            throws IllegalArgumentException {
        if (size <= 0 || size % 9 != 0) throw new IllegalArgumentException("size must be positive multiple of 9");
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory(
                owner, size, net.kyori.adventure.text.Component.text(title));
    }

    @Override
    public @NotNull Inventory createInventory(@Nullable InventoryHolder owner, int size,
            @NotNull net.kyori.adventure.text.Component title) throws IllegalArgumentException {
        if (size <= 0 || size % 9 != 0) throw new IllegalArgumentException("size must be positive multiple of 9");
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory(owner, size, title);
    }

    @Override
    public @NotNull Merchant createMerchant(@Nullable String title) {
        return null;
    }

    @Override
    public @NotNull Merchant createMerchant(@NotNull net.kyori.adventure.text.Component title) {
        return null;
    }

    @Override
    public int getMonsterSpawnLimit() {
        return 70;
    }

    @Override
    public int getAnimalSpawnLimit() {
        return 10;
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return 5;
    }

    @Override
    public int getWaterAmbientSpawnLimit() {
        return 20;
    }

    @Override
    public int getWaterUndergroundCreatureSpawnLimit() {
        return 5;
    }

    @Override
    public int getAmbientSpawnLimit() {
        return 15;
    }

    @Override
    public @NotNull File getWorldContainer() {
        return new File(".");
    }

    @Override
    public @NotNull OfflinePlayer[] getOfflinePlayers() {
        Map<UUID, OfflinePlayer> players = new LinkedHashMap<>();
        for (Player online : getOnlinePlayers()) players.put(online.getUniqueId(), online);

        Set<File> dataDirs = new LinkedHashSet<>();
        for (World world : getWorlds()) {
            File folder = world.getWorldFolder();
            if (folder != null) dataDirs.add(new File(folder, "playerdata"));
        }
        dataDirs.add(new File(getWorldContainer(), "world/playerdata"));

        for (File dir : dataDirs) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".dat") && name.length() > 4);
            if (files == null) continue;
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4);
                try {
                    UUID uuid = UUID.fromString(id);
                    players.putIfAbsent(uuid, getOfflinePlayer(uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return players.values().toArray(new OfflinePlayer[0]);
    }

    @Override
    public @NotNull OfflinePlayer getOfflinePlayer(@NotNull String name) {
        Player online = getPlayer(name);
        if (online != null) return online;
        // Try the profile cache for the real Mojang UUID
        try {
            var profile = console.getProfileCache().get(name);
            if (profile.isPresent()) {
                return new org.bukkit.craftbukkit.v1_21_R1.entity.CraftOfflinePlayer(
                        profile.get().getId(), name);
            }
        } catch (Throwable ignored) {}
        return new org.bukkit.craftbukkit.v1_21_R1.entity.CraftOfflinePlayer(
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8)), name);
    }

    @Override
    public @NotNull OfflinePlayer getOfflinePlayer(@NotNull UUID id) {
        Player online = getPlayer(id);
        if (online != null) return online;
        // Try profile cache for name
        try {
            var profile = console.getProfileCache().get(id);
            if (profile.isPresent()) {
                return new org.bukkit.craftbukkit.v1_21_R1.entity.CraftOfflinePlayer(
                        id, profile.get().getName());
            }
        } catch (Throwable ignored) {}
        return new org.bukkit.craftbukkit.v1_21_R1.entity.CraftOfflinePlayer(id, null);
    }

    @Override
    public @Nullable OfflinePlayer getOfflinePlayerIfCached(@NotNull String name) {
        Player online = getPlayer(name);
        if (online != null) return online;
        try {
            var profile = console.getProfileCache().get(name);
            if (profile.isPresent()) {
                return new org.bukkit.craftbukkit.v1_21_R1.entity.CraftOfflinePlayer(
                        profile.get().getId(), name);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public @NotNull Set<String> getIPBans() {
        Set<String> result = new LinkedHashSet<>();
        for (Object raw : CraftBanList.IP_BANS.getBanEntries()) {
            if (raw instanceof BanEntry<?> entry) result.add(entry.getTarget());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public void banIP(@NotNull String address) {
        Objects.requireNonNull(address, "address");
        try {
            CraftBanList.IP_BANS.addBan(java.net.InetAddress.getByName(address), null, (java.util.Date) null, null);
        } catch (java.net.UnknownHostException ex) {
            throw new IllegalArgumentException("Invalid IP address: " + address, ex);
        }
    }

    @Override
    public void unbanIP(@NotNull String address) {
        Objects.requireNonNull(address, "address");
        try {
            CraftBanList.IP_BANS.pardon(java.net.InetAddress.getByName(address));
        } catch (java.net.UnknownHostException ex) {
            throw new IllegalArgumentException("Invalid IP address: " + address, ex);
        }
    }

    @Override
    public void banIP(@NotNull java.net.InetAddress address) {
        CraftBanList.IP_BANS.addBan(Objects.requireNonNull(address, "address"), null, (java.util.Date) null, null);
    }

    @Override
    public void unbanIP(@NotNull java.net.InetAddress address) {
        CraftBanList.IP_BANS.pardon(Objects.requireNonNull(address, "address"));
    }

    @Override
    public @NotNull Set<OfflinePlayer> getBannedPlayers() {
        Set<OfflinePlayer> result = new LinkedHashSet<>();
        for (OfflinePlayer player : getOfflinePlayers()) {
            if (player.isBanned()) result.add(player);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull BanList getBanList(@NotNull BanList.Type type) {
        return type == BanList.Type.IP ? CraftBanList.IP_BANS : CraftBanList.NAME_BANS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <B extends BanList<E>, E> B getBanList(@NotNull io.papermc.paper.ban.BanListType<B> type) {
        return (B) CraftBanList.NAME_BANS;
    }

    @Override
    public @NotNull Set<OfflinePlayer> getOperators() {
        Set<OfflinePlayer> result = new LinkedHashSet<>();
        for (OfflinePlayer player : getOfflinePlayers()) {
            if (player.isOp()) result.add(player);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public @NotNull Warning.WarningState getWarningState() {
        return Warning.WarningState.DEFAULT;
    }

    @Override
    public @Nullable CachedServerIcon getServerIcon() {
        return null;
    }

    @Override
    public @NotNull CachedServerIcon loadServerIcon(@NotNull File file) throws Exception {
        return null;
    }

    @Override
    public @NotNull CachedServerIcon loadServerIcon(@NotNull BufferedImage image) throws Exception {
        return null;
    }

    @Override
    public void setIdleTimeout(int threshold) {
        if (threshold < 0) throw new IllegalArgumentException("Idle timeout cannot be negative");
        try {
            java.lang.reflect.Method method = console.getClass().getMethod("setPlayerIdleTimeout", int.class);
            method.invoke(console, threshold);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Minecraft server does not expose player idle timeout", e);
        }
    }

    @Override
    public int getIdleTimeout() {
        try {
            java.lang.reflect.Method method = console.getClass().getMethod("getPlayerIdleTimeout");
            return ((Number) method.invoke(console)).intValue();
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    @Override
    public @NotNull UnsafeValues getUnsafe() {
        return unsafeValues;
    }

    @Override
    public @NotNull StructureManager getStructureManager() {
        return null;
    }

    @Override
    public @NotNull io.papermc.paper.threadedregions.scheduler.AsyncScheduler getAsyncScheduler() {
        return (io.papermc.paper.threadedregions.scheduler.AsyncScheduler) java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.AsyncScheduler.class.getClassLoader(),
                new Class<?>[] { io.papermc.paper.threadedregions.scheduler.AsyncScheduler.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("run") || method.getName().startsWith("create")) {
                        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = makeScheduledTaskProxy();
                        if (args != null) {
                            for (Object arg : args) {
                                if (arg instanceof java.util.function.Consumer c) {
                                    new Thread(() -> c.accept(task)).start();
                                    return task;
                                }
                                if (arg instanceof Runnable r) {
                                    new Thread(r).start();
                                    return task;
                                }
                            }
                        }
                        return task;
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }

    private static io.papermc.paper.threadedregions.scheduler.ScheduledTask makeScheduledTaskProxy() {
        return (io.papermc.paper.threadedregions.scheduler.ScheduledTask) java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.ScheduledTask.class.getClassLoader(),
                new Class<?>[] { io.papermc.paper.threadedregions.scheduler.ScheduledTask.class },
                (p, m, a) -> switch (m.getName()) {
                    case "isCancelled", "isRunning" -> false;
                    case "cancel", "getOwningPlugin" -> null;
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == (a != null && a.length > 0 ? a[0] : null);
                    case "toString" -> "LunarArcScheduledTask";
                    default -> m.getReturnType() == boolean.class ? false : null;
                });
    }

    @Override
    public @NotNull io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler getGlobalRegionScheduler() {
        return (io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler) java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler.class.getClassLoader(),
                new Class<?>[] { io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("run") || method.getName().startsWith("execute")) {
                        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = makeScheduledTaskProxy();
                        if (args != null) {
                            for (Object arg : args) {
                                if (arg instanceof java.util.function.Consumer c) {
                                    ((io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge) console)
                                            .lunararc$queueTask(() -> c.accept(task));
                                    return task;
                                }
                                if (arg instanceof Runnable r) {
                                    ((io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge) console)
                                            .lunararc$queueTask(r);
                                    return task;
                                }
                            }
                        }
                        return task;
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }

    @Override
    public @NotNull io.papermc.paper.threadedregions.scheduler.RegionScheduler getRegionScheduler() {
        return (io.papermc.paper.threadedregions.scheduler.RegionScheduler) java.lang.reflect.Proxy.newProxyInstance(
                io.papermc.paper.threadedregions.scheduler.RegionScheduler.class.getClassLoader(),
                new Class<?>[] { io.papermc.paper.threadedregions.scheduler.RegionScheduler.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("run") || method.getName().startsWith("execute")) {
                        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = makeScheduledTaskProxy();
                        if (args != null) {
                            for (Object arg : args) {
                                if (arg instanceof java.util.function.Consumer c) {
                                    ((io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge) console)
                                            .lunararc$queueTask(() -> c.accept(task));
                                    return task;
                                }
                                if (arg instanceof Runnable r) {
                                    ((io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge) console)
                                            .lunararc$queueTask(r);
                                    return task;
                                }
                            }
                        }
                        return task;
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }

    @Override
    public @NotNull org.bukkit.entity.EntityFactory getEntityFactory() {
        return (org.bukkit.entity.EntityFactory) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.entity.EntityFactory.class.getClassLoader(),
                new Class<?>[] { org.bukkit.entity.EntityFactory.class }, (proxy, method, args) -> {
                    if ((method.getName().equals("createEntity") || method.getName().equals("spawn"))
                            && args != null && args.length >= 2 && args[0] instanceof Location location
                            && args[1] instanceof Class<?> entityClass) {
                        World world = Objects.requireNonNull(location.getWorld(), "location world");
                        try {
                            java.lang.reflect.Method spawn = world.getClass().getMethod("spawn", Location.class, Class.class);
                            return spawn.invoke(world, location, entityClass);
                        } catch (ReflectiveOperationException ex) {
                            throw new IllegalArgumentException("Unable to create entity " + entityClass.getName(), ex);
                        }
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemFactory getItemFactory() {
        return itemFactory;
    }

    @Override
    public @NotNull org.bukkit.potion.PotionBrewer getPotionBrewer() {
        return (org.bukkit.potion.PotionBrewer) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.potion.PotionBrewer.class.getClassLoader(),
                new Class<?>[] { org.bukkit.potion.PotionBrewer.class }, (proxy, method, args) -> {
                    if (method.getName().equals("createEffect") && args != null && args.length >= 3
                            && args[0] instanceof org.bukkit.potion.PotionEffectType type
                            && args[1] instanceof Integer duration && args[2] instanceof Integer amplifier) {
                        return new org.bukkit.potion.PotionEffect(type, duration, amplifier);
                    }
                    if (Collection.class.isAssignableFrom(method.getReturnType())) return Collections.emptyList();
                    return defaultValue(method.getReturnType());
                });
    }

    @Override
    public @NotNull com.destroystokyo.paper.entity.ai.MobGoals getMobGoals() {
        return null;
    }

    @Override
    public @NotNull org.bukkit.ServerLinks getServerLinks() {
        return null;
    }

    private Object dataPackManagerProxy(Class<?> api) {
        return java.lang.reflect.Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("getEnabledPacks") || name.equals("getEnabledDataPacks")) return getInitialEnabledPacks();
            if (name.equals("getDisabledPacks") || name.equals("getDisabledDataPacks")) return getInitialDisabledPacks();
            if (name.equals("getPacks") || name.equals("getDataPacks")) {
                LinkedHashSet<String> all = new LinkedHashSet<>();
                all.addAll(getInitialEnabledPacks());
                all.addAll(getInitialDisabledPacks());
                return Collections.unmodifiableSet(all);
            }
            if ((name.equals("isEnabled") || name.equals("isDataPackEnabled")) && args != null && args.length > 0) {
                return getInitialEnabledPacks().contains(String.valueOf(args[0]));
            }
            if (name.toLowerCase(Locale.ROOT).contains("reload")) {
                reloadData();
                return defaultValue(method.getReturnType());
            }
            if (Collection.class.isAssignableFrom(method.getReturnType())) return Collections.emptyList();
            return defaultValue(method.getReturnType());
        });
    }

    @Override
    public @Nullable org.bukkit.packs.DataPackManager getDataPackManager() {
        return (org.bukkit.packs.DataPackManager) dataPackManagerProxy(org.bukkit.packs.DataPackManager.class);
    }

    @Override
    public @NotNull io.papermc.paper.datapack.DatapackManager getDatapackManager() {
        return (io.papermc.paper.datapack.DatapackManager) dataPackManagerProxy(io.papermc.paper.datapack.DatapackManager.class);
    }

    private List<String> readInitialDataPacks(String methodName) {
        try {
            Object properties = dedicatedProperties();
            java.lang.reflect.Field field = properties.getClass().getField("initialDataPackConfiguration");
            Object configuration = field.get(properties);
            Object value = configuration.getClass().getMethod(methodName).invoke(configuration);
            if (value instanceof Collection<?> collection) {
                List<String> result = new ArrayList<>(collection.size());
                for (Object entry : collection) result.add(String.valueOf(entry));
                return Collections.unmodifiableList(result);
            }
        } catch (ReflectiveOperationException ignored) {}
        return Collections.emptyList();
    }

    @Override
    public @NotNull List<String> getInitialDisabledPacks() {
        return readInitialDataPacks("getDisabled");
    }

    @Override
    public @NotNull List<String> getInitialEnabledPacks() {
        return readInitialDataPacks("getEnabled");
    }

    @Override
    public @NotNull org.bukkit.ServerTickManager getServerTickManager() {
        return (org.bukkit.ServerTickManager) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.ServerTickManager.class.getClassLoader(), new Class<?>[] { org.bukkit.ServerTickManager.class },
                (proxy, method, args) -> {
                    Object tickRateManager = invokeNoArg(console, "tickRateManager");
                    if (tickRateManager == null) tickRateManager = invokeNoArg(console, "getTickRateManager");
                    String n = method.getName();
                    if (n.equals("getTickRate")) {
                        Object v = invokeNoArg(tickRateManager, "tickrate");
                        if (v instanceof Number number) return number.floatValue();
                        return 20.0F;
                    }
                    if (n.equals("isFrozen")) {
                        Object v = invokeNoArg(tickRateManager, "isFrozen");
                        return v instanceof Boolean b && b;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    @Override
    public boolean isStopping() {
        return console.isStopped();
    }

    @Override
    public boolean isLoggingIPs() {
        try {
            Object value = console.getClass().getMethod("logIPs").invoke(console);
            if (value instanceof Boolean bool) return bool;
        } catch (ReflectiveOperationException ignored) {}
        return readDedicatedBooleanProperty("logIPs", true);
    }

    @Override
    public boolean isTickingWorlds() {
        try {
            java.lang.reflect.Field field = MinecraftServer.class.getDeclaredField("isIteratingOverLevels");
            field.setAccessible(true);
            Object value = field.get(console);
            if (value instanceof Boolean bool) return bool;
        } catch (ReflectiveOperationException ignored) {}
        return true;
    }

    private Object serverResourcePackInfo() {
        try {
            Object optional = console.getClass().getMethod("getServerResourcePack").invoke(console);
            if (optional instanceof java.util.Optional<?> opt) return opt.orElse(null);
            return optional;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }


    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        if (type == char.class) return '\0';
        return null;
    }

    private Object invokeCompatible(Object target, String name, Object... args) {
        if (target == null) return null;
        outer: for (java.lang.reflect.Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (args[i] != null && !types[i].isInstance(args[i]) && !types[i].isPrimitive()) continue outer;
            }
            try { return method.invoke(target, args); } catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    @Override
    public boolean isResourcePackRequired() {
        try {
            Object value = console.getClass().getMethod("isResourcePackRequired").invoke(console);
            if (value instanceof Boolean bool) return bool;
        } catch (ReflectiveOperationException ignored) {}
        Object info = serverResourcePackInfo();
        Object required = invokeNoArg(info, "isRequired");
        if (!(required instanceof Boolean)) required = invokeNoArg(info, "required");
        return required instanceof Boolean bool && bool;
    }

    @Override
    public @NotNull String getResourcePackHash() {
        Object value = invokeNoArg(serverResourcePackInfo(), "hash");
        return value == null ? "" : value.toString().toUpperCase(Locale.ROOT);
    }

    @Override
    public @Nullable String getResourcePack() {
        Object value = invokeNoArg(serverResourcePackInfo(), "url");
        return value == null ? "" : value.toString();
    }

    @Override
    public @Nullable String getResourcePackPrompt() {
        Object value = invokeNoArg(serverResourcePackInfo(), "prompt");
        if (value instanceof java.util.Optional<?> optional) value = optional.orElse(null);
        if (value == null) return null;
        try {
            Class<?> craftChat = Class.forName("org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage");
            return String.valueOf(craftChat.getMethod("fromComponent", net.minecraft.network.chat.Component.class)
                    .invoke(null, value));
        } catch (ReflectiveOperationException ignored) {
            return value.toString();
        }
    }

    @Override
    public boolean isAcceptingTransfers() {
        return false;
    }

    @Override
    public boolean isEnforcingSecureProfiles() {
        return false;
    }

    @Override
    public boolean shouldSendChatPreviews() {
        return false;
    }

    @Override
    public double[] getTPS() {
        // Vanilla does not keep Paper's 1/5/15-minute rolling TPS samples. Prefer
        // Paper-injected recentTps when present; otherwise derive a useful current
        // rate from the server's rolling tick-time window rather than hard-coding 20.
        try {
            java.lang.reflect.Field field = console.getClass().getField("recentTps");
            Object value = field.get(console);
            if (value instanceof double[] tps && tps.length >= 3) return tps.clone();
        } catch (ReflectiveOperationException ignored) {}
        double avg = getAverageTickTime();
        double current = avg <= 0.0 ? 20.0 : Math.min(20.0, 1000.0 / avg);
        return new double[] { current, current, current };
    }

    @Override
    public long[] getTickTimes() {
        try {
            java.lang.reflect.Field field;
            try {
                field = console.getClass().getField("tickTimes");
            } catch (NoSuchFieldException ex) {
                field = MinecraftServer.class.getDeclaredField("tickTimes");
                field.setAccessible(true);
            }
            Object value = field.get(console);
            if (value instanceof long[] times) return times.clone();
        } catch (ReflectiveOperationException ignored) {}
        return new long[0];
    }

    @Override
    public double getAverageTickTime() {
        try {
            Object value = console.getClass().getMethod("getAverageTickTimeNanos").invoke(console);
            if (value instanceof Number number) return number.doubleValue() / 1_000_000.0D;
        } catch (ReflectiveOperationException ignored) {}
        long[] times = getTickTimes();
        if (times.length == 0) return 0.0D;
        long total = 0L;
        int count = 0;
        for (long time : times) {
            if (time > 0L) {
                total += time;
                count++;
            }
        }
        return count == 0 ? 0.0D : (total / (double) count) / 1_000_000.0D;
    }

    @Override
    public int getCurrentTick() {
        try {
            Object value = console.getClass().getMethod("getTickCount").invoke(console);
            if (value instanceof Number number) return number.intValue();
        } catch (ReflectiveOperationException ignored) {}
        try {
            java.lang.reflect.Field field = MinecraftServer.class.getDeclaredField("tickCount");
            field.setAccessible(true);
            return field.getInt(console);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    @Override
    public boolean reloadCommandAliases() {
        return true;
    }

    @Override
    public void reloadPermissions() {
    }

    @Override
    public @NotNull <T extends Keyed> Iterable<Tag<T>> getTags(@NotNull String registry, @NotNull Class<T> clazz) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(clazz, "clazz");
        List<Tag<T>> tags = new ArrayList<>();
        if (clazz == Material.class && "items".equals(registry)) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getTags().forEach(pair -> {
                net.minecraft.resources.ResourceLocation id = pair.getFirst().location();
                Tag<T> tag = getTag(registry, NamespacedKey.fromString(id.toString()), clazz);
                if (tag != null) tags.add(tag);
            });
        } else if (clazz == Material.class && "blocks".equals(registry)) {
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getTags().forEach(pair -> {
                net.minecraft.resources.ResourceLocation id = pair.getFirst().location();
                Tag<T> tag = getTag(registry, NamespacedKey.fromString(id.toString()), clazz);
                if (tag != null) tags.add(tag);
            });
        }
        return Collections.unmodifiableList(tags);
    }

    @Override
    public @Nullable org.bukkit.scoreboard.Criteria getScoreboardCriteria(@NotNull String name) {
        return null;
    }

    @Override
    public @NotNull com.destroystokyo.paper.profile.PlayerProfile createProfile(@NotNull UUID uuid) {
        return createProfile(uuid, null);
    }

    @Override
    public @NotNull com.destroystokyo.paper.profile.PlayerProfile createProfile(@NotNull String name) {
        return createProfile(null, name);
    }

    @Override
    public @NotNull com.destroystokyo.paper.profile.PlayerProfile createProfile(@Nullable UUID uuid,
            @Nullable String name) {
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(uuid, name);
    }

    @Override
    public @NotNull com.destroystokyo.paper.profile.PlayerProfile createProfileExact(@Nullable UUID uuid,
            @Nullable String name) {
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(uuid, name);
    }

    @Override
    public @NotNull net.kyori.adventure.text.Component motd() {
        return net.kyori.adventure.text.Component.text(getMotd());
    }

    @Override
    public void motd(@NotNull net.kyori.adventure.text.Component motd) {
        Objects.requireNonNull(motd, "motd");
        setMotd(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(motd));
    }

    @Override
    public @NotNull net.kyori.adventure.text.Component shutdownMessage() {
        return net.kyori.adventure.text.Component.text(getShutdownMessage());
    }

    @Override
    public @NotNull org.bukkit.GameMode getDefaultGameMode() {
        try {
            return org.bukkit.GameMode.getByValue(console.getDefaultGameType().getId());
        } catch (Throwable ignored) {
            return org.bukkit.GameMode.SURVIVAL;
        }
    }

    @Override
    public void setDefaultGameMode(@NotNull org.bukkit.GameMode mode) {
        Objects.requireNonNull(mode, "mode");
        try {
            console.setDefaultGameType(net.minecraft.world.level.GameType.byId(mode.getValue()));
        } catch (Throwable ex) {
            throw new IllegalStateException("Unable to set the default game mode", ex);
        }
    }

    @Override
    public int getMaxChainedNeighborUpdates() {
        return 1000000;
    }

    @Override
    public void setMotd(@NotNull String motd) {
        Objects.requireNonNull(motd, "motd");
        try {
            console.getClass().getMethod("setMotd", String.class).invoke(console, motd);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("This Minecraft server does not expose runtime MOTD mutation", ex);
        }
    }

    @Override
    public boolean suggestPlayerNamesWhenNullTabCompletions() {
        return true;
    }

    @Override
    public @Nullable String getPermissionMessage() {
        return "";
    }

    @Override
    public @NotNull net.kyori.adventure.text.Component permissionMessage() {
        return net.kyori.adventure.text.Component.empty();
    }

    @Override
    public @NotNull Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull CommandSender createCommandSender(
            @NotNull Consumer<? super net.kyori.adventure.text.Component> feedback) {
        return null;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull Entity entity) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull World world, int x, int z) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull Location location) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull World world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull Location location, int radius) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull World world, @NotNull io.papermc.paper.math.Position position,
            int radius) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@NotNull World world, @NotNull io.papermc.paper.math.Position position) {
        return true;
    }

    @Override
    public @NotNull String getUpdateFolder() {
        return "update";
    }

    @Override
    public @NotNull File getPluginsFolder() {
        return new File("plugins");
    }

    @Override
    public @NotNull File getUpdateFolderFile() {
        return new File(getPluginsFolder(), getUpdateFolder());
    }

    @Override
    public @Nullable org.bukkit.command.PluginCommand getPluginCommand(@NotNull String name) {
        org.bukkit.command.Command command = commandMap.getCommand(name);
        if (command instanceof org.bukkit.command.PluginCommand) {
            return (org.bukkit.command.PluginCommand) command;
        }
        return null;
    }

    @Override
    public boolean dispatchCommand(@NotNull CommandSender sender, @NotNull String commandLine) {
        return io.ampznetwork.lunararc.common.server.LunarArcCommandRouter.dispatch(
                this, sender, commandLine);
    }


    @Override
    public @NotNull List<Entity> selectEntities(@NotNull CommandSender sender, @NotNull String selector) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(selector, "selector");
        try {
            net.minecraft.commands.CommandSourceStack source = console.createCommandSourceStack();
            if (sender instanceof CraftPlayer craftPlayer) source = craftPlayer.getHandle().createCommandSourceStack();
            Class<?> parserClass = Class.forName("net.minecraft.commands.arguments.selector.EntitySelectorParser");
            Object parser = parserClass.getConstructor(com.mojang.brigadier.StringReader.class, boolean.class)
                    .newInstance(new com.mojang.brigadier.StringReader(selector), true);
            Object entitySelector = parserClass.getMethod("parse").invoke(parser);
            Object entities = entitySelector.getClass().getMethod("findEntities", net.minecraft.commands.CommandSourceStack.class)
                    .invoke(entitySelector, source);
            List<Entity> result = new ArrayList<>();
            if (entities instanceof Iterable<?> iterable) {
                for (Object nmsEntity : iterable) {
                    Object bukkit = invokeNoArg(nmsEntity, "getBukkitEntity");
                    if (bukkit instanceof Entity entity) result.add(entity);
                }
            }
            return Collections.unmodifiableList(result);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Invalid or unsupported entity selector: " + selector, ex);
        }
    }

    @Override
    public @Nullable <T extends Keyed> Registry<T> getRegistry(@NotNull Class<T> type) {
        if (type == null) return null;
        return io.ampznetwork.lunararc.common.server.LunarArcRegistryAccess.INSTANCE.getRegistry(type);
    }

    @Override
    public @Nullable <T extends Keyed> Tag<T> getTag(@NotNull String registry, @NotNull NamespacedKey tag,
            @NotNull Class<T> clazz) {
        java.util.LinkedHashSet<T> values = new java.util.LinkedHashSet<>();
        net.minecraft.resources.ResourceLocation location = net.minecraft.resources.ResourceLocation.parse(tag.toString());

        if (clazz == Material.class && ("items".equals(registry) || "blocks".equals(registry))) {
            if ("items".equals(registry)) {
                var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, location);
                var holders = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(key);
                if (holders.isEmpty()) return null;
                for (var holder : holders.get()) {
                    var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(holder.value());
                    Material material = Material.matchMaterial(id.toString());
                    if (material != null) values.add(clazz.cast(material));
                }
            } else {
                var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, location);
                var holders = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getTag(key);
                if (holders.isEmpty()) return null;
                for (var holder : holders.get()) {
                    var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(holder.value());
                    Material material = Material.matchMaterial(id.toString());
                    if (material != null) values.add(clazz.cast(material));
                }
            }
        } else {
            return null;
        }

        java.util.Set<T> immutable = java.util.Collections.unmodifiableSet(values);
        return new Tag<>() {
            @Override public @NotNull NamespacedKey getKey() { return tag; }
            @Override public boolean isTagged(@NotNull T item) { return immutable.contains(item); }
            @Override public @NotNull java.util.Set<T> getValues() { return immutable; }
        };
    }

    @Override
    public @Nullable LootTable getLootTable(@NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key");
        final net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey());
        Object reloadable = invokeNoArg(console, "reloadableRegistries");
        Object lookup = reloadable == null ? null : invokeNoArg(reloadable, "getLootTable");
        // Lookup shape differs between vanilla/Paper mappings. If a direct key lookup is available, reject missing tables.
        Object nms = invokeCompatible(lookup, "get", id);
        if (nms instanceof Optional<?> optional) nms = optional.orElse(null);
        final Object handle = nms;
        if (lookup != null && handle == null) return null;
        return (LootTable) java.lang.reflect.Proxy.newProxyInstance(
                LootTable.class.getClassLoader(), new Class<?>[] { LootTable.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getKey")) return key;
                    if (method.getName().equals("toString")) return "LunarArcLootTable{" + key + "}";
                    if (Collection.class.isAssignableFrom(method.getReturnType())) return Collections.emptyList();
                    return defaultValue(method.getReturnType());
                });
    }

    @Override
    public @NotNull BlockData createBlockData(@NotNull Material material) {
        return createBlockData(material, (Consumer<? super BlockData>) null);
    }

    @Override
    public @NotNull BlockData createBlockData(@NotNull Material material,
            @Nullable Consumer<? super BlockData> consumer) {
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.parse(
                material.getKey().toString());
        net.minecraft.world.level.block.Block block =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
        net.minecraft.world.level.block.state.BlockState state = (block != null)
                ? block.defaultBlockState()
                : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        BlockData bd = org.bukkit.craftbukkit.v1_21_R1.block.data.CraftBlockData.create(state);
        if (consumer != null) consumer.accept(bd);
        return bd;
    }

    @Override
    public @NotNull BlockData createBlockData(@NotNull String data) throws IllegalArgumentException {
        // Parse "minecraft:stone[waterlogged=false]" style strings
        String blockStr = data.contains("[") ? data.substring(0, data.indexOf('[')) : data;
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.parse(blockStr);
        net.minecraft.world.level.block.Block block =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
        net.minecraft.world.level.block.state.BlockState state = (block != null)
                ? block.defaultBlockState()
                : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        return org.bukkit.craftbukkit.v1_21_R1.block.data.CraftBlockData.create(state);
    }

    @Override
    public @NotNull BlockData createBlockData(@Nullable Material material, @Nullable String data)
            throws IllegalArgumentException {
        if (material != null) return createBlockData(material);
        if (data != null) return createBlockData(data);
        return org.bukkit.craftbukkit.v1_21_R1.block.data.CraftBlockData.create(
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    }

    @Override
    public int getMaxWorldSize() {
        return 29999984;
    }

    @Override
    public @Nullable Entity getEntity(@NotNull UUID uuid) {
        // Search the player cache first (fastest path)
        Entity cached = playerCache.get(uuid);
        if (cached != null) return cached;
        // Use reflection for both getEntity(UUID) and getBukkitEntity(): the former
        // may not be on the vanilla NMS compile classpath; the latter is Paper-specific.
        for (net.minecraft.server.level.ServerLevel level : console.getAllLevels()) {
            try {
                java.lang.reflect.Method getEntity = level.getClass().getMethod("getEntity", java.util.UUID.class);
                Object nmsEntity = getEntity.invoke(level, uuid);
                if (nmsEntity != null) {
                    java.lang.reflect.Method getBukkit = nmsEntity.getClass().getMethod("getBukkitEntity");
                    return (Entity) getBukkit.invoke(nmsEntity);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Advancement wrapAdvancement(Object holder) {
        if (holder == null) return null;
        return (Advancement) java.lang.reflect.Proxy.newProxyInstance(
                Advancement.class.getClassLoader(), new Class<?>[] { Advancement.class }, (proxy, method, args) -> {
                    Object id = invokeNoArg(holder, "id");
                    NamespacedKey key = id == null ? null : namespacedKey(String.valueOf(id));
                    Object value = invokeNoArg(holder, "value");
                    return switch (method.getName()) {
                        case "getKey" -> key;
                        case "getCriteria" -> {
                            Object criteria = value == null ? null : invokeNoArg(value, "criteria");
                            if (criteria instanceof Map<?, ?> map) {
                                List<String> names = new ArrayList<>();
                                for (Object entry : map.keySet()) names.add(String.valueOf(entry));
                                yield Collections.unmodifiableList(names);
                            }
                            yield Collections.emptyList();
                        }
                        case "toString" -> "LunarArcAdvancement{" + key + "}";
                        case "hashCode" -> Objects.hashCode(key);
                        case "equals" -> proxy == args[0] || (args[0] instanceof Advancement a && Objects.equals(key, a.getKey()));
                        default -> defaultValue(method.getReturnType());
                    };
                });
    }

    private NamespacedKey namespacedKey(String value) {
        if (value == null) return null;
        int split = value.indexOf(':');
        return split < 0 ? new NamespacedKey("minecraft", value)
                : new NamespacedKey(value.substring(0, split), value.substring(split + 1));
    }

    private Collection<?> liveAdvancementHolders() {
        try {
            Object manager = console.getClass().getMethod("getAdvancements").invoke(console);
            for (String methodName : List.of("getAllAdvancements", "getAllAdvancementsIterable", "getAllAdvancementsCollection")) {
                try {
                    Object value = manager.getClass().getMethod(methodName).invoke(manager);
                    if (value instanceof Collection<?> c) return c;
                    if (value instanceof Iterable<?> iterable) {
                        List<Object> all = new ArrayList<>();
                        iterable.forEach(all::add);
                        return all;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            // 1.21.x AdvancementTree stores all nodes; expose their holders if that is the available shape.
            Object tree = invokeNoArg(manager, "tree");
            if (tree != null) {
                for (String methodName : List.of("nodes", "all", "roots")) {
                    Object nodes = invokeNoArg(tree, methodName);
                    if (nodes instanceof Iterable<?> iterable) {
                        List<Object> holders = new ArrayList<>();
                        for (Object node : iterable) {
                            Object holder = invokeNoArg(node, "holder");
                            if (holder != null) holders.add(holder);
                        }
                        if (!holders.isEmpty()) return holders;
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {}
        return Collections.emptyList();
    }

    @Override
    public @Nullable Advancement getAdvancement(@NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key");
        for (Object holder : liveAdvancementHolders()) {
            Object id = invokeNoArg(holder, "id");
            if (id != null && key.toString().equals(String.valueOf(id))) return wrapAdvancement(holder);
        }
        try {
            Object manager = console.getClass().getMethod("getAdvancements").invoke(console);
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey());
            for (String methodName : List.of("get", "getAdvancement")) {
                try {
                    Object holder = manager.getClass().getMethod(methodName, net.minecraft.resources.ResourceLocation.class).invoke(manager, id);
                    if (holder instanceof Optional<?> optional) holder = optional.orElse(null);
                    if (holder != null) return wrapAdvancement(holder);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }

    @Override
    public @NotNull Iterator<Advancement> advancementIterator() {
        List<Advancement> advancements = new ArrayList<>();
        for (Object holder : liveAdvancementHolders()) {
            Advancement advancement = wrapAdvancement(holder);
            if (advancement != null) advancements.add(advancement);
        }
        return Collections.unmodifiableList(advancements).iterator();
    }

    @Override
    public boolean removeBossBar(@NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key");
        KeyedBossBar removed = bossBars.remove(key);
        if (removed != null) removed.removeAll();
        return removed != null;
    }

    @Override
    public @Nullable KeyedBossBar getBossBar(@NotNull NamespacedKey key) {
        return bossBars.get(Objects.requireNonNull(key, "key"));
    }

    @Override
    public @NotNull Iterator<KeyedBossBar> getBossBars() {
        return Collections.unmodifiableCollection(new ArrayList<>(bossBars.values())).iterator();
    }

    @Override
    public @NotNull KeyedBossBar createBossBar(@NotNull NamespacedKey key, @Nullable String title,
            @NotNull BarColor color, @NotNull BarStyle style, @NotNull BarFlag... flags) {
        Objects.requireNonNull(key, "key");
        if (bossBars.containsKey(key)) throw new IllegalArgumentException("Boss bar already exists: " + key);
        KeyedBossBar bar = io.ampznetwork.lunararc.common.server.LunarArcBossBar.createKeyed(key, title, color, style, flags);
        bossBars.put(key, bar);
        return bar;
    }

    @Override
    public @NotNull BossBar createBossBar(@Nullable String title, @NotNull BarColor color, @NotNull BarStyle style,
            @NotNull BarFlag... flags) {
        return io.ampznetwork.lunararc.common.server.LunarArcBossBar.create(title, color, style, flags);
    }

    @Override
    public int getSpawnLimit(@NotNull SpawnCategory category) {
        return 70;
    }

    @Override
    public int getTicksPerSpawns(@NotNull SpawnCategory category) {
        return 1;
    }

    @Override
    public int getTicksPerAnimalSpawns() {
        return 400;
    }

    @Override
    public int getTicksPerMonsterSpawns() {
        return 1;
    }

    @Override
    public int getTicksPerWaterSpawns() {
        return 1;
    }

    @Override
    public int getTicksPerWaterAmbientSpawns() {
        return 1;
    }

    @Override
    public int getTicksPerWaterUndergroundCreatureSpawns() {
        return 1;
    }

    @Override
    public int getTicksPerAmbientSpawns() {
        return 1;
    }

    @Override
    public @NotNull PlayerProfile createPlayerProfile(@Nullable UUID uniqueId, @Nullable String name) {
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(uniqueId, name);
    }

    @Override
    public @NotNull PlayerProfile createPlayerProfile(@NotNull UUID uniqueId) {
        String name = null;
        try {
            var profile = console.getProfileCache().get(uniqueId);
            if (profile.isPresent()) name = profile.get().getName();
        } catch (Throwable ignored) {}
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(uniqueId, name);
    }

    @Override
    public @NotNull PlayerProfile createPlayerProfile(@NotNull String name) {
        UUID id = getPlayerUniqueId(name);
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(id, name);
    }

    @Override
    public @Nullable UUID getPlayerUniqueId(@NotNull String name) {
        // Check online players first
        Player online = getPlayer(name);
        if (online != null) return online.getUniqueId();
        // Try profile cache
        try {
            var profile = console.getProfileCache().get(name);
            if (profile.isPresent()) return profile.get().getId();
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public @NotNull ChunkGenerator.ChunkData createChunkData(@NotNull World world) {
        return null;
    }

    @Override
    public @NotNull Map<String, String[]> getCommandAliases() {
        return Collections.emptyMap();
    }

    private NamespacedKey recipeKey(Recipe recipe) {
        return recipe instanceof Keyed keyed ? keyed.getKey() : null;
    }

    @Override
    public boolean addRecipe(@Nullable Recipe recipe) {
        return addRecipe(recipe, true);
    }

    @Override
    public boolean addRecipe(@Nullable Recipe recipe, boolean update) {
        if (recipe == null) return false;
        NamespacedKey key = recipeKey(recipe);
        if (key == null) throw new IllegalArgumentException("Recipe must implement Keyed");
        if (runtimeRecipes.putIfAbsent(key, recipe) != null) return false;
        if (update) updateRecipes();
        return true;
    }

    @Override
    public boolean removeRecipe(@NotNull NamespacedKey key) {
        return removeRecipe(key, true);
    }

    @Override
    public boolean removeRecipe(@NotNull NamespacedKey key, boolean update) {
        Objects.requireNonNull(key, "key");
        boolean removed = runtimeRecipes.remove(key) != null;
        if (removed && update) updateRecipes();
        return removed;
    }

    @Override
    public @Nullable Recipe getRecipe(@NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key");
        Recipe runtime = runtimeRecipes.get(key);
        if (runtime != null) return runtime;
        return null;
    }

    @Override
    public @NotNull List<Recipe> getRecipesFor(@NotNull ItemStack result) {
        Objects.requireNonNull(result, "result");
        List<Recipe> matches = new ArrayList<>();
        for (Recipe recipe : runtimeRecipes.values()) {
            ItemStack recipeResult = recipe.getResult();
            if (recipeResult != null && recipeResult.isSimilar(result)) matches.add(recipe);
        }
        return Collections.unmodifiableList(matches);
    }

    @Override
    public @NotNull Iterator<Recipe> recipeIterator() {
        return Collections.unmodifiableCollection(new ArrayList<>(runtimeRecipes.values())).iterator();
    }

    @Override
    public void clearRecipes() {
        runtimeRecipes.clear();
        updateRecipes();
    }

    @Override
    public void resetRecipes() {
        runtimeRecipes.clear();
        try {
            Object resources = invokeNoArg(console, "getServerResources");
            if (resources != null) {
                Object recipes = invokeNoArg(resources, "getRecipeManager");
                if (recipes != null) {
                    try { recipes.getClass().getMethod("finalizeRecipeLoading").invoke(recipes); }
                    catch (ReflectiveOperationException ignored) {}
                }
            }
        } finally {
            updateRecipes();
        }
    }

    @Override
    public void updateRecipes() {
        try {
            playerList.getClass().getMethod("reloadRecipeData").invoke(playerList);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to refresh recipes for connected players", ex);
        }
    }

    private boolean emptyCraftSlot(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private boolean choiceMatches(RecipeChoice choice, ItemStack item) {
        if (choice == null) return emptyCraftSlot(item);
        if (emptyCraftSlot(item)) return false;
        try { return choice.test(item); } catch (Throwable ignored) { return false; }
    }

    private boolean matchesShapeless(ShapelessRecipe recipe, ItemStack[] items) {
        List<RecipeChoice> remaining = new ArrayList<>(recipe.getChoiceList());
        for (ItemStack item : items) {
            if (emptyCraftSlot(item)) continue;
            int hit = -1;
            for (int i = 0; i < remaining.size(); i++) {
                if (choiceMatches(remaining.get(i), item)) { hit = i; break; }
            }
            if (hit < 0) return false;
            remaining.remove(hit);
        }
        return remaining.isEmpty();
    }

    private boolean matchesShaped(ShapedRecipe recipe, ItemStack[] items) {
        String[] shape = recipe.getShape();
        Map<Character, RecipeChoice> choices = recipe.getChoiceMap();
        int width = 0;
        for (String row : shape) width = Math.max(width, row.length());
        int height = shape.length;
        // Paper's crafting API receives a 3x3 matrix. Try every legal translation of the recipe.
        for (int oy = 0; oy <= 3 - height; oy++) {
            for (int ox = 0; ox <= 3 - width; ox++) {
                boolean ok = true;
                for (int y = 0; y < 3 && ok; y++) {
                    for (int x = 0; x < 3; x++) {
                        ItemStack item = items[y * 3 + x];
                        int sx = x - ox, sy = y - oy;
                        char symbol = (sy >= 0 && sy < height && sx >= 0 && sx < shape[sy].length())
                                ? shape[sy].charAt(sx) : ' ';
                        RecipeChoice choice = symbol == ' ' ? null : choices.get(symbol);
                        if (choice == null ? !emptyCraftSlot(item) : !choiceMatches(choice, item)) { ok = false; break; }
                    }
                }
                if (ok) return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemCraftResult craftItemResult(@NotNull ItemStack[] items,
            @NotNull World world) {
        return craftItemResult(items, world, null);
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemCraftResult craftItemResult(@NotNull ItemStack[] items,
            @NotNull World world, @Nullable Player player) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(world, "world");
        ItemStack result = craftItem(items, world, player);
        final ItemStack crafted = result == null ? new ItemStack(Material.AIR) : result;
        final List<ItemStack> remaining = java.util.stream.IntStream.range(0, items.length)
                .mapToObj(i -> new ItemStack(Material.AIR)).toList();
        return (org.bukkit.inventory.ItemCraftResult) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.inventory.ItemCraftResult.class.getClassLoader(),
                new Class<?>[] { org.bukkit.inventory.ItemCraftResult.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getResult", "result" -> crafted.clone();
                    case "getRemainingItems", "remainingItems" -> remaining;
                    default -> defaultValue(method.getReturnType());
                });
    }

    @Override
    public @Nullable ItemStack craftItem(@NotNull ItemStack[] items, @NotNull World world) {
        return craftItem(items, world, null);
    }

    @Override
    public @Nullable ItemStack craftItem(@NotNull ItemStack[] items, @NotNull World world, @Nullable Player player) {
        Recipe recipe = getCraftingRecipe(items, world);
        return recipe == null ? null : recipe.getResult().clone();
    }

    @Override
    public @Nullable Recipe getCraftingRecipe(@NotNull ItemStack[] items, @NotNull World world) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(world, "world");
        if (items.length != 9) throw new IllegalArgumentException("Crafting matrix must contain exactly 9 items");
        for (Recipe recipe : runtimeRecipes.values()) {
            if (recipe instanceof ShapedRecipe shaped && matchesShaped(shaped, items)) return recipe;
            if (recipe instanceof ShapelessRecipe shapeless && matchesShapeless(shapeless, items)) return recipe;
        }
        return null;
    }

    @Override
    public @NotNull World createWorld(@NotNull WorldCreator creator) {
        Objects.requireNonNull(creator, "creator");
        World existing = getWorld(creator.name());
        if (existing != null) return existing;
        // Dynamic world creation requires constructing a ServerLevel with the active loader's
        // registry/dimension lifecycle. Keep that operation below the shared Bukkit layer.
        Object bridge = console instanceof io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge b ? b : null;
        if (bridge != null) {
            Object created = invokeCompatible(bridge, "lunararc$createWorld", creator);
            if (created instanceof World world) return world;
            if (created instanceof net.minecraft.server.level.ServerLevel level) return craftWorld(level);
        }
        throw new IllegalStateException("This LunarArc platform adapter does not expose dynamic world creation yet: " + creator.name());
    }

    @Override
    public boolean unloadWorld(@NotNull String name, boolean save) {
        World world = getWorld(Objects.requireNonNull(name, "name"));
        return world != null && unloadWorld(world, save);
    }

    @Override
    public boolean unloadWorld(@NotNull World world, boolean save) {
        Objects.requireNonNull(world, "world");
        net.minecraft.server.level.ServerLevel level = null;
        for (net.minecraft.server.level.ServerLevel candidate : console.getAllLevels()) {
            if (craftWorld(candidate).getUID().equals(world.getUID())) { level = candidate; break; }
        }
        if (level == null || level.dimension() == net.minecraft.world.level.Level.OVERWORLD) return false;
        if (!world.getPlayers().isEmpty()) return false;
        if (save) {
            try { level.save(null, true, false); }
            catch (Throwable ex) { logger.log(java.util.logging.Level.WARNING, "Failed to save world " + world.getName(), ex); return false; }
        }
        try {
            java.lang.reflect.Field levelsField = MinecraftServer.class.getDeclaredField("levels");
            levelsField.setAccessible(true);
            Object levels = levelsField.get(console);
            if (levels instanceof Map<?, ?> raw) {
                @SuppressWarnings("unchecked") Map<Object,Object> map = (Map<Object,Object>) raw;
                Object removed = map.remove(level.dimension());
                if (removed != null) {
                    worldCache.remove(world.getUID());
                    return true;
                }
            }
        } catch (ReflectiveOperationException ex) {
            logger.log(java.util.logging.Level.WARNING, "Unable to remove world from live server registry: " + world.getName(), ex);
        }
        return false;
    }

    @Override
    public @NotNull WorldBorder createWorldBorder() {
        return new CraftWorldBorder(new net.minecraft.world.level.border.WorldBorder());
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public @NotNull ItemStack createExplorerMap(@NotNull World world, @NotNull Location location,
            @NotNull org.bukkit.StructureType structureType) {
        return new ItemStack(org.bukkit.Material.FILLED_MAP);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public @NotNull ItemStack createExplorerMap(@NotNull World world, @NotNull Location location,
            @NotNull org.bukkit.StructureType structureType, int radius, boolean findUnexplored) {
        return new ItemStack(org.bukkit.Material.FILLED_MAP);
    }

    @Override
    public @NotNull ItemStack createExplorerMap(@NotNull World world, @NotNull Location location,
            @NotNull org.bukkit.generator.structure.StructureType structureType,
            @NotNull org.bukkit.map.MapCursor.Type mapCursorType, int radius, boolean findUnexplored) {
        return null;
    }

    @Override
    public @NotNull ScoreboardManager getScoreboardManager() {
        return io.ampznetwork.lunararc.common.server.LunarArcScoreboardManager.getInstance();
    }

    @Override
    public @NotNull org.bukkit.packs.ResourcePack getServerResourcePack() {
        return null;
    }

    @Override
    public void updateResources() {
        try {
            playerList.getClass().getMethod("reloadResources").invoke(playerList);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to refresh server resources for connected players", ex);
        }
    }

    public @NotNull FeatureFlagConfig getFeatureFlagConfig() {
        return (FeatureFlagConfig) java.lang.reflect.Proxy.newProxyInstance(
            FeatureFlagConfig.class.getClassLoader(),
            new Class<?>[] { FeatureFlagConfig.class },
            (proxy, method, args) -> {
                if (method.getReturnType().equals(Set.class)) return Collections.emptySet();
                if (method.getReturnType().equals(boolean.class)) return false;
                return null;
            }
        );
    }
}
