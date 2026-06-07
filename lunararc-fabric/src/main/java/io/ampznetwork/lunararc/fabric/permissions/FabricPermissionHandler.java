package io.ampznetwork.lunararc.fabric.permissions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Delegates Fabric permission checks to Bukkit's permission system.
 * Called from FabricBridge.hasPermission() to check mod-side permission nodes.
 */
public class FabricPermissionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-Fabric");

    public static boolean hasPermission(UUID playerUUID, String permission) {
        try {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) return player.hasPermission(permission);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerUUID);
            return offline.isOp();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasPermission(UUID playerUUID, String permission, boolean defaultValue) {
        try {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                if (player.isPermissionSet(permission)) return player.hasPermission(permission);
                return defaultValue;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerUUID);
            return offline.isOp();
        } catch (Throwable t) {
            return defaultValue;
        }
    }
}
