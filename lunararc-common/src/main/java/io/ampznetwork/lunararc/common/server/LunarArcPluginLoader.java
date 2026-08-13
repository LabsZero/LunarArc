package io.ampznetwork.lunararc.common.server;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class LunarArcPluginLoader implements PluginLoader {
    private final Server server;
    private final Yaml yaml = new Yaml();
    private final Pattern[] fileFilters = new Pattern[] { Pattern.compile("\\.jar$") };
    private static final Logger logger = LoggerFactory.getLogger("LunarArc");

    public LunarArcPluginLoader(Server server) {
        this.server = server;
    }

    public Server getServerInstance() {
        return server;
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        if (!file.exists()) {
            throw new InvalidPluginException(new java.io.FileNotFoundException(file.getPath()));
        }

        PluginDescriptionFile description;
        try {
            description = getPluginDescription(file);
            io.ampznetwork.lunararc.common.config.PluginBlacklist.BlacklistEntry blacklist =
                    io.ampznetwork.lunararc.common.config.PluginBlacklist.check(
                            description.getName(), description.getVersion());
            if (blacklist != null) {
                throw new InvalidPluginException("Plugin " + description.getFullName()
                        + " is incompatible with LunarArc: " + blacklist.reason);
            }
        } catch (InvalidDescriptionException e) {
            logger.error("[LunarArc] Error reading plugin description from " + file.getName(), e);
            throw new InvalidPluginException(e);
        }

        File dataFolder = new File(file.getParentFile(), description.getName());
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        try {
            org.bukkit.plugin.java.PluginClassLoader loader = new org.bukkit.plugin.java.PluginClassLoader(this, getClass().getClassLoader(),
                    description, dataFolder, file);
            JavaPlugin plugin = LunarArcPaperPluginSupport.createBootstrappedPlugin(
                    loader, file, description, dataFolder);
            if (plugin == null) {
                Class<?> mainClass = loader.findClass(description.getMain(), false);
                Class<? extends JavaPlugin> pluginClass = mainClass.asSubclass(JavaPlugin.class);
                java.lang.reflect.Constructor<? extends JavaPlugin> constructor = pluginClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                plugin = constructor.newInstance();
            }

            // Use Bukkit/Paper's own plugin.yml command parser instead of recreating
            // PluginCommand metadata and ownership manually.
            try {
                java.util.List<org.bukkit.command.Command> commands =
                        org.bukkit.command.PluginCommandYamlParser.parse(plugin);
                if (!commands.isEmpty()) {
                    server.getCommandMap().registerAll(description.getName(), commands);
                }
            } catch (Throwable ex) {
                server.getLogger().log(java.util.logging.Level.SEVERE,
                        "Failed to register commands for " + description.getName(), ex);
                throw new InvalidPluginException(ex);
            }

            return plugin;
        } catch (Throwable e) {
            Throwable cause = e;
            if (e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) {
                cause = ite.getCause();
            }
            if (cause instanceof UnsupportedClassVersionError versionError) {
                String message = friendlyJavaVersionMessage(file.getName(), versionError);
                server.getLogger().severe(message);
                throw new InvalidPluginException(message, versionError);
            }
            server.getLogger().log(java.util.logging.Level.SEVERE,
                    "Failed to load plugin " + file.getName() + ": " + cause.getMessage(), cause);
            throw new InvalidPluginException(cause);
        }
    }

    private static String friendlyJavaVersionMessage(String fileName, UnsupportedClassVersionError error) {
        int runtimeFeature = Runtime.version().feature();
        int requiredFeature = -1;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("class file version ([0-9]+)(?:\\.[0-9]+)?")
                .matcher(String.valueOf(error.getMessage()));
        if (matcher.find()) {
            try {
                int classVersion = Integer.parseInt(matcher.group(1));
                requiredFeature = classVersion - 44;
            } catch (NumberFormatException ignored) {
            }
        }
        if (requiredFeature > 0) {
            return "[LunarArc] Cannot load " + fileName + ": plugin requires Java "
                    + requiredFeature + ", but this server is running Java " + runtimeFeature + ".";
        }
        return "[LunarArc] Cannot load " + fileName
                + ": plugin was compiled for a newer Java runtime; this server is running Java "
                + runtimeFeature + ".";
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            boolean isPaper = false;
            if (entry == null) {
                entry = jar.getJarEntry("paper-plugin.yml");
                isPaper = true;
            }

            if (entry == null) {
                throw new InvalidDescriptionException("Jar does not contain plugin.yml or paper-plugin.yml");
            }

            try (InputStream stream = jar.getInputStream(entry)) {
                if (!isPaper) {
                    return new PluginDescriptionFile(stream);
                } else {
                    // Manual parsing for paper-plugin.yml to bridge to PluginDescriptionFile
                    Map<String, Object> map = yaml.load(stream);
                    String name = map.getOrDefault("name", "").toString();
                    String version = map.getOrDefault("version", "").toString();
                    String main = map.getOrDefault("main-class", map.getOrDefault("main", "")).toString();
                    
                    if (name.isEmpty() || main.isEmpty()) {
                        throw new InvalidDescriptionException("paper-plugin.yml is missing 'name' or 'main-class'");
                    }

                    Map<String, Object> compatMap = new java.util.HashMap<>(map);
                    compatMap.put("main", main);
                    translatePaperDependencies(compatMap, map.get("dependencies"));

                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    yaml.dump(compatMap, new java.io.OutputStreamWriter(baos));
                    return new PluginDescriptionFile(new java.io.ByteArrayInputStream(baos.toByteArray()));
                }
            }
        } catch (Exception e) {
            throw new InvalidDescriptionException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void translatePaperDependencies(Map<String, Object> target, Object dependenciesValue) {
        if (!(dependenciesValue instanceof Map<?, ?> dependencies)) return;
        Object serverValue = dependencies.get("server");
        if (!(serverValue instanceof Map<?, ?> serverDependencies)) return;

        java.util.LinkedHashSet<String> depend = stringSet(target.get("depend"));
        java.util.LinkedHashSet<String> softDepend = stringSet(target.get("softdepend"));
        java.util.LinkedHashSet<String> loadBefore = stringSet(target.get("loadbefore"));

        for (Map.Entry<?, ?> entry : serverDependencies.entrySet()) {
            String dependencyName = String.valueOf(entry.getKey());
            if (dependencyName.isBlank()) continue;

            boolean required = true;
            String load = "OMIT";
            boolean joinClasspath = true;
            if (entry.getValue() instanceof Map<?, ?> dependency) {
                Object requiredValue = dependency.get("required");
                if (requiredValue != null) required = Boolean.parseBoolean(String.valueOf(requiredValue));
                Object loadValue = dependency.get("load");
                if (loadValue != null) load = String.valueOf(loadValue).toUpperCase(java.util.Locale.ROOT);
                Object joinValue = dependency.get("join-classpath");
                if (joinValue != null) joinClasspath = Boolean.parseBoolean(String.valueOf(joinValue));
            }

            // Preserve Paper's explicit order without accidentally creating a
            // dependency cycle in the legacy metadata carrier. BEFORE means the
            // dependency loads first; AFTER means this plugin loads first. OMIT has
            // undefined order, so required dependencies use the conservative legacy
            // depend behavior and optional dependencies do not impose an order.
            if ("AFTER".equals(load)) {
                loadBefore.add(dependencyName);
            } else if ("BEFORE".equals(load) || "OMIT".equals(load)) {
                if (required) depend.add(dependencyName);
                else if ("BEFORE".equals(load)) softDepend.add(dependencyName);
            }

            // join-classpath is enforced by PluginClassLoader from paper-plugin.yml.
        }

        if (!depend.isEmpty()) target.put("depend", new java.util.ArrayList<>(depend));
        if (!softDepend.isEmpty()) target.put("softdepend", new java.util.ArrayList<>(softDepend));
        if (!loadBefore.isEmpty()) target.put("loadbefore", new java.util.ArrayList<>(loadBefore));
    }

    private static java.util.LinkedHashSet<String> stringSet(Object value) {
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) if (item != null) result.add(String.valueOf(item));
        }
        return result;
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener,
            Plugin plugin) {
        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new java.util.HashMap<>();
        java.util.Set<java.lang.reflect.Method> methods;
        try {
            java.lang.reflect.Method[] publicMethods = listener.getClass().getMethods();
            java.lang.reflect.Method[] privateMethods = listener.getClass().getDeclaredMethods();
            methods = new java.util.HashSet<>(publicMethods.length + privateMethods.length, 1.0f);
            for (java.lang.reflect.Method method : publicMethods) {
                methods.add(method);
            }
            for (java.lang.reflect.Method method : privateMethods) {
                methods.add(method);
            }
        } catch (NoClassDefFoundError e) {
            plugin.getLogger()
                    .severe("Plugin " + plugin.getDescription().getFullName() + " has failed to register events for "
                            + listener.getClass() + " because " + e.getMessage() + " does not exist.");
            return ret;
        }

        for (java.lang.reflect.Method method : methods) {
            org.bukkit.event.EventHandler eh = method.getAnnotation(org.bukkit.event.EventHandler.class);
            if (eh == null)
                continue;
            if (method.isBridge() || method.isSynthetic())
                continue;

            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1
                    || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                plugin.getLogger()
                        .severe(plugin.getDescription().getFullName()
                                + " attempted to register an invalid EventHandler method signature \""
                                + method.toGenericString() + "\" in " + listener.getClass());
                continue;
            }

            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            Set<RegisteredListener> eventSet = ret.computeIfAbsent(eventClass, k -> new java.util.HashSet<>());

            EventExecutor executor = (ignored, event) -> {
                try {
                    if (!eventClass.isAssignableFrom(event.getClass()))
                        return;
                    method.invoke(listener, event);
                } catch (java.lang.reflect.InvocationTargetException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    throw new EventException(cause);
                } catch (Exception ex) {
                    throw new EventException(ex);
                }
            };

            eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
        }
        return ret;
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        if (!plugin.isEnabled()) {
            try {
                if (plugin instanceof org.bukkit.plugin.java.JavaPlugin jp) {
                    var method = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredMethod("setEnabled", boolean.class);
                    method.setAccessible(true);
                    method.invoke(jp, true);
                    // setEnabled(true) calls onEnable(); if the plugin's handleCrash() pattern
                    // ran (e.g. EssentialsX), isEnabled() will be false even though no exception
                    // escaped — log it so the user can see the original error in the plugin's log.
                    if (!jp.isEnabled()) {
                        server.getLogger().severe("[LunarArc] Plugin " + plugin.getName()
                                + " failed to enable (check its log above for the root cause).");
                    } else {
                        repairWorldEditItemRegistry(plugin);
                        // Match Bukkit/Paper lifecycle semantics. LuckPerms uses this
                        // event to notice Vault enabling and installs its Vault hooks.
                        server.getPluginManager().callEvent(new org.bukkit.event.server.PluginEnableEvent(plugin));
                    }
                }
            } catch (Exception e) {
                Throwable cause = e;
                if (e instanceof java.lang.reflect.InvocationTargetException ite) {
                    cause = ite.getCause();
                }
                server.getLogger().log(java.util.logging.Level.SEVERE, "Error enabling plugin " + plugin.getName(), cause);
                if (cause instanceof RuntimeException re) throw re;
                throw new RuntimeException(cause);
            }
        }
    }

    private void repairWorldEditItemRegistry(Plugin plugin) {
        if (!"WorldEdit".equalsIgnoreCase(plugin.getName())) return;
        try {
            ClassLoader cl = plugin.getClass().getClassLoader();
            Class<?> itemTypeClass = Class.forName("com.sk89q.worldedit.world.item.ItemType", true, cl);
            Object registry = itemTypeClass.getField("REGISTRY").get(null);
            java.lang.reflect.Method get = registry.getClass().getMethod("get", String.class);
            java.lang.reflect.Method register = registry.getClass().getMethod("register", String.class, Object.class);
            java.lang.reflect.Constructor<?> ctor = itemTypeClass.getConstructor(String.class);
            int added = 0;
            for (org.bukkit.Material material : org.bukkit.Material.values()) {
                if (material.isLegacy() || !material.isItem()) continue;
                String key = material.getKey().toString();
                if (get.invoke(registry, key) != null) continue;
                register.invoke(registry, key, ctor.newInstance(key));
                added++;
            }
            if (added > 0) server.getLogger().info("[LunarArc] Repaired WorldEdit item registry (" + added + " items).");
        } catch (NoSuchMethodException e) {
            // NamespacedRegistry erases the value parameter to Keyed on some WE builds.
            try {
                ClassLoader cl = plugin.getClass().getClassLoader();
                Class<?> itemTypeClass = Class.forName("com.sk89q.worldedit.world.item.ItemType", true, cl);
                Object registry = itemTypeClass.getField("REGISTRY").get(null);
                java.lang.reflect.Method get = registry.getClass().getMethod("get", String.class);
                java.lang.reflect.Method register = java.util.Arrays.stream(registry.getClass().getMethods())
                        .filter(m -> m.getName().equals("register") && m.getParameterCount() == 2)
                        .findFirst().orElseThrow();
                java.lang.reflect.Constructor<?> ctor = itemTypeClass.getConstructor(String.class);
                for (org.bukkit.Material material : org.bukkit.Material.values()) {
                    if (material.isLegacy() || !material.isItem()) continue;
                    String key = material.getKey().toString();
                    if (get.invoke(registry, key) == null) register.invoke(registry, key, ctor.newInstance(key));
                }
            } catch (Throwable error) {
                server.getLogger().warning("[LunarArc] Could not repair WorldEdit item registry: " + error);
            }
        } catch (Throwable error) {
            server.getLogger().warning("[LunarArc] Could not repair WorldEdit item registry: " + error);
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (plugin == null || !plugin.isEnabled()) return;

        try {
            if (plugin instanceof org.bukkit.plugin.java.JavaPlugin javaPlugin) {
                java.lang.reflect.Method method =
                        org.bukkit.plugin.java.JavaPlugin.class.getDeclaredMethod("setEnabled", boolean.class);
                method.setAccessible(true);
                method.invoke(javaPlugin, false);
                server.getPluginManager().callEvent(new org.bukkit.event.server.PluginDisableEvent(plugin));
            }
        } catch (Exception e) {
            server.getLogger().log(java.util.logging.Level.SEVERE,
                    "Error disabling plugin " + plugin.getName(), e);
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(plugin);
            server.getScheduler().cancelTasks(plugin);
            server.getServicesManager().unregisterAll(plugin);
            server.getMessenger().unregisterIncomingPluginChannel(plugin);
            server.getMessenger().unregisterOutgoingPluginChannel(plugin);
            if (server.getCommandMap() instanceof LunarArcCommandMap lunarCommands) {
                lunarCommands.unregisterPlugin(plugin);
            }
            ClassLoader loader = plugin.getClass().getClassLoader();
            if (loader instanceof java.io.Closeable closeable) {
                try { closeable.close(); } catch (java.io.IOException ignored) {}
            }
        }
    }
}
