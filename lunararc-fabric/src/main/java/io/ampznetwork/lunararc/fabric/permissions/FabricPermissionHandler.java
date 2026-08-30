package io.ampznetwork.lunararc.fabric.permissions;

import io.ampznetwork.lunararc.common.permission.LunarArcBukkitPermissions;
import java.util.UUID;

public final class FabricPermissionHandler {
    private FabricPermissionHandler() {}

    public static boolean hasPermission(UUID playerUUID, String permission) {
        return LunarArcBukkitPermissions.hasPermission(playerUUID, permission);
    }

    public static boolean hasPermission(UUID playerUUID, String permission, boolean defaultValue) {
        return LunarArcBukkitPermissions.hasPermission(playerUUID, permission, defaultValue);
    }
}
