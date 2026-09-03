package org.spigotmc;


public class WatchdogThread extends Thread {

    public static volatile WatchdogThread instance;
    public volatile long lastTick = System.currentTimeMillis();
    public volatile boolean stopping;
    private final int timeoutTime;
    private final boolean restart;

    private WatchdogThread(int timeoutTime, boolean restart) {
        super("Spigot Watchdog Thread");
        this.timeoutTime = timeoutTime;
        this.restart = restart;
        setDaemon(true);
    }

    public static synchronized void doStart(int timeoutTime, boolean restart) {
        if (instance != null && instance.isAlive()) return;
        instance = new WatchdogThread(timeoutTime, restart);
        instance.start();
    }

    public static void tick() {
        WatchdogThread current = instance;
        if (current != null) current.lastTick = System.currentTimeMillis();
    }

    public static synchronized void doStop() {
        WatchdogThread current = instance;
        if (current != null) {
            current.stopping = true;
            current.interrupt();
        }
        instance = null;
    }

    public static void hasStarted(boolean started) {
        if (started) tick();
    }

    public int getTimeoutTime() { return timeoutTime; }
    public boolean isRestart() { return restart; }

    @Override
    public void run() {
        while (!stopping) {
            try { Thread.sleep(1000L); } catch (InterruptedException ignored) {}
        }
    }
}
