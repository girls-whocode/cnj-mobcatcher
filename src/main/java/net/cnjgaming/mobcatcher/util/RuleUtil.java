package net.cnjgaming.mobcatcher.util;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class RuleUtil {

    private RuleUtil() {
    }

    public static boolean isWorldBlocked(CNJMobCatcherPlugin plugin, String worldName, String action) {
        if (worldName == null || action == null) {
            return false;
        }

        if (!plugin.getConfig().getBoolean("worlds.apply." + action, true)) {
            return false;
        }

        String mode = plugin.getConfig().getString("worlds.mode", "ALLOWLIST");
        List<String> worlds = plugin.getConfig().getStringList("worlds.list");

        if (worlds.isEmpty()) {
            return false;
        }

        boolean listed = worlds.stream().anyMatch(w -> w.equalsIgnoreCase(worldName));
        String normalized = mode == null ? "ALLOWLIST" : mode.toUpperCase(Locale.ROOT);

        if ("DENYLIST".equals(normalized)) {
            return listed;
        }

        return !listed;
    }

    public static boolean isBlockedByOtherClaim(CNJMobCatcherPlugin plugin, Player player, Location location) {
        if (!plugin.getConfig().getBoolean("claims.enforce-on-release", false)) {
            return false;
        }

        if (plugin.getConfig().getBoolean("claims.allow-on-other-claims", false)) {
            return false;
        }

        UUID ownerId = findClaimOwner(location);
        if (ownerId == null) {
            return false;
        }

        return !ownerId.equals(player.getUniqueId());
    }

    private static UUID findClaimOwner(Location location) {
        Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention == null || !griefPrevention.isEnabled()) {
            return null;
        }

        try {
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Field instanceField = gpClass.getField("instance");
            Object gpInstance = instanceField.get(null);
            if (gpInstance == null) {
                return null;
            }

            Field dataStoreField = gpClass.getField("dataStore");
            Object dataStore = dataStoreField.get(gpInstance);
            if (dataStore == null) {
                return null;
            }

            Method claimLookup = null;
            for (Method method : dataStore.getClass().getMethods()) {
                if ("getClaimAt".equals(method.getName())) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length >= 1 && Location.class.isAssignableFrom(params[0])) {
                        claimLookup = method;
                        break;
                    }
                }
            }

            if (claimLookup == null) {
                return null;
            }

            Object claim;
            int paramCount = claimLookup.getParameterCount();
            if (paramCount == 1) {
                claim = claimLookup.invoke(dataStore, location);
            } else if (paramCount == 2) {
                claim = claimLookup.invoke(dataStore, location, false);
            } else {
                claim = claimLookup.invoke(dataStore, location, false, null);
            }

            if (claim == null) {
                return null;
            }

            try {
                Method getOwnerID = claim.getClass().getMethod("getOwnerID");
                Object value = getOwnerID.invoke(claim);
                return value instanceof UUID ? (UUID) value : null;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Field ownerField = claim.getClass().getField("ownerID");
                Object value = ownerField.get(claim);
                return value instanceof UUID ? (UUID) value : null;
            } catch (NoSuchFieldException ignored) {
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }
}
