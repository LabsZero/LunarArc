package io.ampznetwork.lunararc.common.mixin.core.server;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.server.players.PlayerList;

@Mixin(value = MinecraftServer.class, priority = Integer.MAX_VALUE)
public abstract class MinecraftServerMixin implements MinecraftServerBridge {

    @Unique
    private static final Logger lunararc$logger = LoggerFactory.getLogger("LunarArc");

    @Unique
    private final Queue<Runnable> lunararc$taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void lunararc$queueTask(Runnable runnable) {
        this.lunararc$taskQueue.add(runnable);
    }

    @Shadow private int tickCount;

    @Inject(method = "tickChildren", at = @At("TAIL"))
    private void lunararc$processTasks(CallbackInfo ci) {
        // Run internal tasks
        Runnable task;
        while ((task = this.lunararc$taskQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                lunararc$logger.error("Error executing queued task", e);
            }
        }

        // Run Bukkit scheduler
        try {
            org.bukkit.Server server = org.bukkit.Bukkit.getServer();
            if (server != null) {
                org.bukkit.scheduler.BukkitScheduler scheduler = server.getScheduler();
                if (scheduler instanceof org.bukkit.craftbukkit.v1_21_R1.scheduler.CraftScheduler craftScheduler) {
                    craftScheduler.mainThreadHeartbeat(this.tickCount);
                }
            }
        } catch (Throwable t) {
            lunararc$logger.error("Error in Bukkit scheduler heartbeat", t);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lunararc$onInit(CallbackInfo ci) {
        lunararc$logger.info("Initializing Hybrid Bridge...");

        // Start capturing console output early so BlockMedic can always upload something.
        io.ampznetwork.lunararc.common.telemetry.BlockMedicReporter.startConsoleCapture();

        LunarArcConfig.load();
        io.ampznetwork.lunararc.common.config.PluginBlacklist.load();
        io.ampznetwork.lunararc.api.LunarArcServer.init();
    }

    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z", shift = At.Shift.AFTER))
    private void lunararc$afterServerInit(CallbackInfo ci) {
        lunararc$logger.info("Creating CraftServer bridge via platform factory...");

        org.bukkit.craftbukkit.v1_21_R1.CraftServer craftServer =
                LunarArcPlatform.createCraftServer((MinecraftServer) (Object) this);
        LunarArcPlatform.setServer(craftServer);

        io.ampznetwork.lunararc.common.server.LunarArcBuiltinCommands.register(craftServer);
        loadPlugins();
        enablePlugins(org.bukkit.plugin.PluginLoadOrder.STARTUP);
        // loadLevel() is called inside initServer(), so the world is already loaded
        // by the time this inject fires — enable POSTWORLD here rather than in a
        // separate loadLevel() TAIL inject (which would fire before the bridge exists).
        enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD);
    }

    @Unique
    private void enablePlugins(org.bukkit.plugin.PluginLoadOrder type) {
        org.bukkit.Server server = org.bukkit.Bukkit.getServer();
        if (server instanceof org.bukkit.craftbukkit.v1_21_R1.CraftServer craftServer) {
            craftServer.enablePlugins(type);
            lunararc$syncCommands();
        }
    }

    @Unique
    private void lunararc$syncCommands() {
        try {
            org.bukkit.Server server = org.bukkit.Bukkit.getServer();
            if (server instanceof org.bukkit.craftbukkit.v1_21_R1.CraftServer craftServer) {
                net.minecraft.commands.Commands commands = ((net.minecraft.server.MinecraftServer) (Object) this).getCommands();
                com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = ((io.ampznetwork.lunararc.common.mixin.core.command.CommandsAccessor) commands).getDispatcher();

                org.bukkit.command.CommandMap commandMap = craftServer.getCommandMap();
                if (commandMap instanceof io.ampznetwork.lunararc.common.server.LunarArcCommandMap lunarArcMap) {
                    lunarArcMap.syncToBrigadier(dispatcher);
                }
            }
        } catch (Throwable t) {
            lunararc$logger.error("Error syncing Bukkit commands", t);
        }
    }

    /**
     * @author LunarArc
     * @reason Override NeoForge/vanilla mod name so plugins see "paper" as the server brand.
     */
    @Overwrite
    public String getServerModName() {
        return "paper";
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void lunararc$onStop(CallbackInfo ci) {
        try {
            // Use reflection to avoid hard references that crash the Mixin transformer if
            // Bukkit is missing
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object server = bukkitClass.getMethod("getServer").invoke(null);
            if (server == null)
                return;

            Object pluginManager = server.getClass().getMethod("getPluginManager").invoke(server);
            Object[] plugins = (Object[]) pluginManager.getClass().getMethod("getPlugins").invoke(pluginManager);

            lunararc$logger.info("Disabling {} plugins...", plugins.length);
            java.lang.reflect.Method disableMethod = pluginManager.getClass().getMethod("disablePlugin",
                    Class.forName("org.bukkit.plugin.Plugin"));

            for (Object plugin : plugins) {
                try {
                    disableMethod.invoke(pluginManager, plugin);
                } catch (Exception e) {
                    lunararc$logger.error("Error disabling plugin", e);
                }
            }
        } catch (Throwable ignored) {
            // Bukkit not present or not initialized yet
        }
    }

    private void loadPlugins() {
        try {
            lunararc$logger.info("[LunarArc] Initializing Bukkit plugin loading...");
            org.bukkit.Server server = org.bukkit.Bukkit.getServer();
            if (server instanceof org.bukkit.craftbukkit.v1_21_R1.CraftServer craftServer) {
                craftServer.loadPlugins();
                lunararc$syncCommands();
            } else {
                lunararc$logger.error("Bukkit Server is not CraftServer instance!");
            }
        } catch (Throwable e) {
            lunararc$logger.error("Critical error in plugin loading sequence", e);
        }
    }
}
