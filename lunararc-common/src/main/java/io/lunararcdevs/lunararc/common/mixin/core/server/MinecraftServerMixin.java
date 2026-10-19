package io.lunararcdevs.lunararc.common.mixin.core.server;

import org.spongepowered.asm.mixin.injection.Redirect;
import io.lunararcdevs.lunararc.common.bridge.CommandSourceBridge;
import io.lunararcdevs.lunararc.common.bridge.MinecraftServerBridge;
import io.lunararcdevs.lunararc.common.config.LunarArcConfig;
import org.bukkit.craftbukkit.command.CraftCommandMap;
import io.lunararcdevs.lunararc.common.mod.server.LunarArcServer;
import io.lunararcdevs.lunararc.common.mod.server.LunarArcRollingAverage;
import io.lunararcdevs.lunararc.common.mod.util.log.LunarArcConsole;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Mixin(value = MinecraftServer.class, priority = Integer.MAX_VALUE)
public abstract class MinecraftServerMixin implements MinecraftServerBridge, CommandSourceBridge {

    @Unique
    private static final Logger lunararc$logger = LoggerFactory.getLogger("LunarArc");

    @Unique
    private final Queue<Runnable> lunararc$taskQueue = new ConcurrentLinkedQueue<>();

    @Unique
    private CraftServer lunararc$craftServer;

    @Unique
    private WorldLoader.DataLoadContext lunararc$dataLoadContext;

    @Unique
    private long lunararc$bukkitStartupStartedNanos;

    @Unique
    private boolean lunararc$serverLoadEventFired;

    @Unique
    private boolean lunararc$startupPluginsEnabled;

    @Unique
    private boolean lunararc$postWorldPluginsEnabled;

    @Unique
    private boolean lunararc$tickingWorlds;

    @Unique
    private long lunararc$tpsSampleStartedNanos = net.minecraft.util.Util.getNanos();

    @Unique
    private final LunarArcRollingAverage lunararc$tps1 = new LunarArcRollingAverage(60);

    @Unique
    private final LunarArcRollingAverage lunararc$tps5 = new LunarArcRollingAverage(60 * 5);

    @Unique
    private final LunarArcRollingAverage lunararc$tps15 = new LunarArcRollingAverage(60 * 15);

    @Shadow
    private int tickCount;

    @Shadow
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Override
    public void lunararc$addLevel(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        ServerLevel previous = this.levels.putIfAbsent(level.dimension(), level);
        if (previous != null && previous != level) {
            throw new IllegalStateException("A ServerLevel is already registered for " + level.dimension().identifier());
        }
        if (previous == null) {
            this.lunararc$loaderLevelLoad(level);
            this.lunararc$loaderMarkLevelsDirty();
        }
    }

    @Override
    public void lunararc$removeLevel(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        if (this.levels.remove(level.dimension(), level)) {
            this.lunararc$loaderLevelUnload(level);
            this.lunararc$loaderMarkLevelsDirty();
        }
    }

    @Override
    public void lunararc$initializeDynamicLevel(ServerLevel level, PrimaryLevelData data, boolean bonusChest) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(data, "data");
        if (!data.isInitialized()) {
            lunararc$setInitialSpawn(level, data, bonusChest, data.isDebugWorld());
            data.setInitialized(true);
        }
        CraftServer craftServer = this.lunararc$requireCraftServer();
        craftServer.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(craftServer.getCraftWorld(level)));
    }

    @Override
    public void lunararc$prepareDynamicLevel(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        this.lunararc$loaderMarkLevelsDirty();
        this.lunararc$loaderReinstatePersistentChunks(level);
        level.getChunkSource().setSpawnSettings(true);
    }

    @org.spongepowered.asm.mixin.gen.Invoker("setInitialSpawn")
    private static void lunararc$invokeSetInitialSpawn(ServerLevel level, net.minecraft.world.level.storage.ServerLevelData data,
            boolean bonusChest, boolean debug, net.minecraft.server.level.progress.LevelLoadListener listener) {
        throw new AssertionError();
    }

    @Unique
    private static void lunararc$setInitialSpawn(ServerLevel level, net.minecraft.world.level.storage.ServerLevelData data,
            boolean bonusChest, boolean debug) {
        lunararc$invokeSetInitialSpawn(level, data, bonusChest, debug, new net.minecraft.server.level.progress.LevelLoadListener() {
            @Override public void start(net.minecraft.server.level.progress.LevelLoadListener.Stage stage, int total) {}
            @Override public void update(net.minecraft.server.level.progress.LevelLoadListener.Stage stage, int current, int total) {}
            @Override public void finish(net.minecraft.server.level.progress.LevelLoadListener.Stage stage) {}
            @Override public void updateFocus(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, net.minecraft.world.level.ChunkPos chunk) {}
        });
    }

    @Override
    public void lunararc$queueTask(Runnable runnable) {
        this.lunararc$taskQueue.add(Objects.requireNonNull(runnable, "runnable"));
    }

    @Override
    public CraftServer lunararc$getCraftServer() {
        return this.lunararc$craftServer;
    }

    @Override
    public void lunararc$setCraftServer(CraftServer craftServer) {
        Objects.requireNonNull(craftServer, "craftServer");
        if (this.lunararc$craftServer != null && this.lunararc$craftServer != craftServer) {
            throw new IllegalStateException("CraftServer already attached to this MinecraftServer");
        }
        this.lunararc$craftServer = craftServer;
    }

    @Override
    public boolean lunararc$isTickingWorlds() {
        return this.lunararc$tickingWorlds;
    }

    @Override
    public WorldLoader.DataLoadContext lunararc$getDataLoadContext() {
        WorldLoader.DataLoadContext context = this.lunararc$dataLoadContext;
        if (context == null) {
            throw new IllegalStateException("WorldLoader.DataLoadContext is not available");
        }
        return context;
    }

    public double[] recentTps = new double[3];

    public long nextTickTime = 0L;

    @Unique private final java.util.Set<String> lunararc$pluginsBlockingSleep = new java.util.HashSet<>();

    @Override
    public void lunararc$setPluginAllowingSleep(String pluginName, boolean value) {
        if (!value) this.lunararc$pluginsBlockingSleep.add(pluginName);
        else this.lunararc$pluginsBlockingSleep.remove(pluginName);
    }

    /** Paper's allowPausing API: a plugin that disallows sleeping keeps the empty-server pause counter at zero. */
    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;isSprinting()Z", ordinal = 0), require = 0)
    private boolean lunararc$pluginBlockedSleep(net.minecraft.server.ServerTickRateManager manager) {
        if (!this.lunararc$pluginsBlockingSleep.isEmpty()) {
            this.lunararc$pluginsBlockingSleep.removeIf(name -> !org.bukkit.Bukkit.getPluginManager().isPluginEnabled(name));
        }
        return manager.isSprinting() || !this.lunararc$pluginsBlockingSleep.isEmpty();
    }

    @Override
    public double[] lunararc$getTps() {

        return new double[] { this.recentTps[0], this.recentTps[1], this.recentTps[2] };
    }

    @Unique
    private long lunararc$tickStartedNanos;

    @Unique
    private void lunararc$drainQueuedTasks() {
        Runnable task;
        while ((task = this.lunararc$taskQueue.poll()) != null) {
            try {
                task.run();
            } catch (Throwable exception) {
                lunararc$logger.error("Error executing queued LunarArc task", exception);
            }
        }
    }

    @Inject(method = "tickServer", at = @At("HEAD"), require = 0)
    private void lunararc$tickStart(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        // A server paused for being empty returns from tickServer before tickChildren, so the end-of-tick drain never
        // runs. A joining player's login finishes through this queue and only then counts as online, which is what
        // unpauses the server, so it would hang on "Connecting" forever. Drain here too.
        this.lunararc$drainQueuedTasks();
        this.lunararc$tickStartedNanos = System.nanoTime();
        if (com.destroystokyo.paper.event.server.ServerTickStartEvent.getHandlerList().getRegisteredListeners().length == 0
                || this.lunararc$craftServer == null) return;
        this.lunararc$craftServer.getPluginManager()
                .callEvent(new com.destroystokyo.paper.event.server.ServerTickStartEvent(this.tickCount + 1));
    }

    @Inject(method = "tickServer", at = @At("RETURN"), require = 0)
    private void lunararc$tickEnd(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (com.destroystokyo.paper.event.server.ServerTickEndEvent.getHandlerList().getRegisteredListeners().length == 0
                || this.lunararc$craftServer == null) return;
        long elapsed = System.nanoTime() - this.lunararc$tickStartedNanos;
        this.lunararc$craftServer.getPluginManager().callEvent(new com.destroystokyo.paper.event.server.ServerTickEndEvent(
                this.tickCount, elapsed / 1_000_000.0D, Math.max(0L, 50_000_000L - elapsed)));
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    private void lunararc$beginTickChildren(CallbackInfo ci) {
        this.lunararc$tickingWorlds = true;
    }

    @Inject(method = "tickChildren", at = @At("TAIL"))
    private void lunararc$processTasks(CallbackInfo ci) {
        this.lunararc$tickingWorlds = false;
        if (this.tickCount > 0 && this.tickCount % 20 == 0) {
            long now = net.minecraft.util.Util.getNanos();
            long elapsed = Math.max(1L, now - this.lunararc$tpsSampleStartedNanos);
            BigDecimal currentTps = BigDecimal.valueOf(20_000_000_000L)
                    .divide(BigDecimal.valueOf(elapsed), 30, RoundingMode.HALF_UP);
            this.lunararc$tps1.add(currentTps, elapsed);
            this.lunararc$tps5.add(currentTps, elapsed);
            this.lunararc$tps15.add(currentTps, elapsed);
            this.lunararc$tpsSampleStartedNanos = now;
            // Publish to the CraftBukkit-named field in the same place, so a plugin reading
            // recentTps directly and one calling getTPS() can never disagree.
            this.recentTps[0] = this.lunararc$tps1.getAverage();
            this.recentTps[1] = this.lunararc$tps5.getAverage();
            this.recentTps[2] = this.lunararc$tps15.getAverage();
        }

        this.lunararc$drainQueuedTasks();

        CraftServer craftServer = this.lunararc$craftServer;
        if (craftServer != null) {
            ((CraftScheduler) craftServer.getScheduler()).mainThreadHeartbeat(this.tickCount);
        }

        io.papermc.paper.adventure.providers.ClickCallbackProviderImpl.ADVENTURE_CLICK_MANAGER.handleQueue(this.tickCount);
        io.papermc.paper.adventure.providers.ClickCallbackProviderImpl.DIALOG_CLICK_MANAGER.handleQueue(this.tickCount);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lunararc$onInit(CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.server.LunarArcLogger.quietNettyNativeProbes();

        this.lunararc$dataLoadContext = io.lunararcdevs.lunararc.common.mod.util.LunarArcWorldLoaderCapture.take();
        LunarArcServer.attach((MinecraftServer) (Object) this);

        io.lunararcdevs.lunararc.common.LunarArcPaths.initialize();
        io.lunararcdevs.lunararc.common.LunarArcPaths.platformRuntime(
                io.lunararcdevs.lunararc.common.mod.server.LunarArcServer.platformName());
        LunarArcConfig.load();
        io.lunararcdevs.lunararc.api.LunarArcServer.init();

    }

    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void lunararc$beforeWorldLoad(CallbackInfo ci) {
        if (this.lunararc$startupPluginsEnabled) return;
        this.lunararc$startupPluginsEnabled = true;

        CraftServer craftServer = this.lunararc$requireCraftServer();
        this.lunararc$bukkitStartupStartedNanos = System.nanoTime();
        io.lunararcdevs.lunararc.common.server.LunarArcWorldVersionStamp.stamp((MinecraftServer) (Object) this);

        try {
            craftServer.loadPlugins();
        } catch (io.lunararcdevs.lunararc.common.config.IncompatibleSoftwareException fatal) {

            ((MinecraftServer) (Object) this).halt(false);
            throw fatal;
        }

        this.lunararc$enablePlugins(craftServer, PluginLoadOrder.STARTUP);
    }


    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void lunararc$afterWorldLoad(CallbackInfo ci) {
        if (this.lunararc$postWorldPluginsEnabled) return;
        this.lunararc$postWorldPluginsEnabled = true;

        // Developer aid: -Dlunararc.auditMixins=true force-loads every mixin target so a broken injection fails here, at boot,
        // instead of the first time that class happens to load in play.
        if (Boolean.getBoolean("lunararc.auditMixins")) {
            try {
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
                lunararc$logger.info("Mixin audit finished: every mixin target applied cleanly");
            } catch (Throwable failure) {
                lunararc$logger.error("Mixin audit found a mixin that does not apply", failure);
            }
        }

        MinecraftServer minecraftServer = (MinecraftServer) (Object) this;
        CraftServer craftServer = this.lunararc$requireCraftServer();

        for (net.minecraft.server.level.ServerLevel level : minecraftServer.getAllLevels()) {
            if (!craftServer.worldLoadEventFired.add(level.dimension())) continue;
            org.bukkit.craftbukkit.CraftWorld craftWorld = craftServer.getCraftWorld(level);
            craftServer.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(craftWorld));
            craftServer.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(craftWorld));
        }

        this.lunararc$enablePlugins(craftServer, PluginLoadOrder.POSTWORLD);

        long commandSyncStarted = System.nanoTime();
        this.lunararc$firePaperCommandLifecycle(minecraftServer);
        this.lunararc$syncCommands(craftServer, minecraftServer);
        long commandSyncMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - commandSyncStarted);
        LunarArcConsole.success(lunararc$logger, "Bukkit command tree finalized in {}ms", commandSyncMillis);

        if (!this.lunararc$serverLoadEventFired) {
            this.lunararc$serverLoadEventFired = true;
            craftServer.getPluginManager().callEvent(new org.bukkit.event.server.ServerLoadEvent(
                    org.bukkit.event.server.ServerLoadEvent.LoadType.STARTUP));
        }


        io.lunararcdevs.lunararc.common.server.LunarArcTier3RuntimeProbe.run(craftServer);

        io.lunararcdevs.lunararc.common.server.LunarArcEssentialsItemBridge.populateModdedItems(craftServer);

        io.lunararcdevs.lunararc.common.server.LunarArcAntiXrayOreBridge.mergeModdedOres(craftServer);


        long startupMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.lunararc$bukkitStartupStartedNanos);
        LunarArcConsole.success(lunararc$logger, "Bukkit/Paper {} compatibility layer ready ({}ms)", io.lunararcdevs.lunararc.common.compat.PaperCompatibility.RUNTIME_MINECRAFT, startupMillis);

    }

    @Unique
    private void lunararc$enablePlugins(CraftServer craftServer, PluginLoadOrder order) {
        LunarArcConsole.info(lunararc$logger, "Beginning plugin enable phase {}", order);
        craftServer.enablePlugins(order);
        LunarArcConsole.success(lunararc$logger, "Completed plugin enable phase {}", order);
    }

    @Unique
    private void lunararc$firePaperCommandLifecycle(MinecraftServer minecraftServer) {
        net.minecraft.commands.Commands minecraftCommands = minecraftServer.getCommands();
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher =
                minecraftCommands.getDispatcher();
        io.papermc.paper.command.brigadier.PaperCommands registrar =
                new io.papermc.paper.command.brigadier.PaperCommands(dispatcher);
        io.lunararcdevs.lunararc.common.server.LunarArcReloadableRegistrarEvent<io.papermc.paper.command.brigadier.Commands> event =
                new io.lunararcdevs.lunararc.common.server.LunarArcReloadableRegistrarEvent<>(
                        registrar,
                        io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.Cause.INITIAL);
        io.lunararcdevs.lunararc.common.server.LunarArcLifecycleEventRunner.fire(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS, event);
    }

    @Unique
    private void lunararc$syncCommands(CraftServer craftServer, MinecraftServer minecraftServer) {
        net.minecraft.commands.Commands commands = minecraftServer.getCommands();
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher =
                commands.getDispatcher();

        if (craftServer.getCommandMap() instanceof CraftCommandMap lunarArcMap) {
            lunarArcMap.syncToBrigadier(dispatcher);
        }

        for (net.minecraft.server.level.ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            commands.sendCommands(player);
        }
    }

    @Override
    public org.bukkit.command.CommandSender lunararc$getBukkitSender(net.minecraft.commands.CommandSourceStack stack) {
        return this.lunararc$requireCraftServer().getConsoleSender();
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void lunararc$onStop(CallbackInfo ci) {

        CraftServer craftServer = this.lunararc$craftServer;
        if (craftServer != null) {
            craftServer.disablePluginsForShutdown();

            craftServer.clearPluginsForShutdown();
            craftServer.shutdownSchedulers();
        }

        io.lunararcdevs.lunararc.common.network.LunarArcPluginMessageOwnership.clear();
        io.lunararcdevs.lunararc.common.server.LunarArcLifecycleEventRunner.resetServerState();
        org.bukkit.plugin.java.PluginClassLoader.shutdownSharedLoaders();
        io.lunararcdevs.lunararc.common.server.LunarArcContext.clearServerReferences();
    }

    @Inject(method = "stopServer", at = @At("RETURN"))
    private void lunararc$afterStop(CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.server.LunarArcProfileCacheWriter.flush();

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            StringBuilder culprits = new StringBuilder();
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.isDaemon() || thread == Thread.currentThread()) continue;
                if (thread.getName().equals("DestroyJavaVM")) continue;
                if (culprits.length() > 0) culprits.append(", ");
                culprits.append('\'').append(thread.getName()).append('\'');
            }
            org.slf4j.LoggerFactory.getLogger("LunarArc").info(
                    "Shutdown finished; exiting the JVM now. Plugin thread(s) that were never stopped by their plugin: {}",
                    culprits.length() > 0 ? culprits : "none found (already exiting on its own)");
            Runtime.getRuntime().halt(0);
        }, "LunarArc Shutdown Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    @Inject(method = "getServerModName", remap = false, cancellable = true, at = @At("RETURN"))
    private void lunararc$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + " lunararc/"
                + io.lunararcdevs.lunararc.common.server.LunarArcVersionInfo.lunarArcVersion());
    }


    @org.spongepowered.asm.mixin.injection.Inject(method = "setUsingWhitelist", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), require = 1)
    private void lunararc$whitelistToggle(boolean enabled, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.whitelistToggle(enabled);
    }
}
