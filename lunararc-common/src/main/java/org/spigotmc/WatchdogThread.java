package org.spigotmc;

/**
 * Spigot WatchdogThread stub for plugin compatibility (WorldEdit SpigotWatchdog).
 * WorldEdit's SpigotWatchdog reflectively reads {@code instance} and {@code lastTick}.
 */
public class WatchdogThread extends Thread {

    public static WatchdogThread instance;
    public volatile long lastTick = 0L;

    private WatchdogThread() {
        super("Spigot Watchdog Thread");
        setDaemon(true);
    }

    public static void doStart(int timeoutTime, boolean restart) {
        if (instance == null) {
            instance = new WatchdogThread();
            instance.start();
        }
    }

    public static void tick() {
        if (instance != null) {
            instance.lastTick = System.currentTimeMillis();
        }
    }

    public static void hasStarted(boolean started) {}

    @Override
    public void run() {
        // No-op watchdog — LunarArc does not need Spigot's watchdog
    }
}
