package net.cnjgaming.mobcatcher.listener;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import net.cnjgaming.mobcatcher.manager.CatcherItemManager;
import net.cnjgaming.mobcatcher.util.MessageUtil;
import net.cnjgaming.mobcatcher.util.RuleUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CaptureListener implements Listener {

    private enum CaptureMode {
        STORAGE,
        EGG
    }

    private final CNJMobCatcherPlugin plugin;
    private final CatcherItemManager catcherItemManager;

    public CaptureListener(CNJMobCatcherPlugin plugin, CatcherItemManager catcherItemManager) {
        this.plugin = plugin;
        this.catcherItemManager = catcherItemManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCapture(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("cnj.mobcatcher.capture")) return;
        if (!catcherItemManager.isMobCatcher(item)) return;

        event.setCancelled(true);

        if (isCaptureBlockedByWorld(player)) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.WORLD_BLOCKED_CAPTURE, "&cYou cannot capture mobs in this world.");
            return;
        }

        if (!catcherItemManager.hasUsesRemaining(item)) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.NO_USES, "&cThis catcher has no uses left.");
            return;
        }

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof LivingEntity living)) return;
        if (clicked instanceof Player) return;

        if (isBlacklistedForPlayer(player, clicked)) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.BLOCKED_MOB, "&cYou cannot capture that mob.");
            return;
        }

        String customName = getPlainName(living);

        String variant = catcherItemManager.getVariantSignature(clicked);
        String properties = catcherItemManager.captureProperties(clicked);
        CaptureMode mode = resolveMode(player);

        if (mode == CaptureMode.EGG) {
            captureAsEgg(player, item, clicked, customName, variant, properties);
            return;
        }

        captureAsStorage(player, item, clicked, customName, variant);
    }

    private boolean isCaptureBlockedByWorld(Player player) {
        return !player.hasPermission("cnj.mobcatcher.bypass.worlds")
                && RuleUtil.isWorldBlocked(plugin, player.getWorld().getName(), "capture");
    }

    private boolean isBlacklistedForPlayer(Player player, Entity clicked) {
        if (player.hasPermission("cnj.mobcatcher.bypass.blacklist")) {
            return false;
        }

        Set<String> blacklist = new HashSet<>();
        for (String value : plugin.getConfig().getStringList("capture.blacklist.entity-types")) {
            blacklist.add(value.toUpperCase());
        }
        return blacklist.contains(clicked.getType().name());
    }

    private String getPlainName(LivingEntity living) {
        Component nameComponent = living.customName();
        if (nameComponent == null) {
            return null;
        }
        return PlainTextComponentSerializer.plainText().serialize(nameComponent);
    }

    private void captureAsEgg(Player player, ItemStack item, Entity clicked, String customName, String variant, String properties) {
        boolean consumed = catcherItemManager.consumeOneUse(item);
        if (!consumed) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.NO_USES, "&cThis catcher has no uses left.");
            return;
        }

        ItemStack egg = catcherItemManager.createCapturedEgg(clicked.getType(), customName, variant, properties);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(egg);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        updateHeldItem(player, item);
        clicked.remove();
        playCaptureFeedback(player, clicked);
        sendCapturedEggMessage(player, clicked.getType().name(), variant);
    }

    private void captureAsStorage(Player player, ItemStack item, Entity clicked, String customName, String variant) {
        boolean stored = catcherItemManager.storeCapturedMob(item, clicked.getType(), customName, variant);
        if (!stored) {
            sendStoreFailureMessage(player, item);
            return;
        }

        updateHeldItem(player, item);
        clicked.remove();
        playCaptureFeedback(player, clicked);
        sendCapturedStorageMessage(player, clicked.getType().name(), variant, catcherItemManager.getStoredCount(item));
    }

    private void sendStoreFailureMessage(Player player, ItemStack item) {
        int storedCount = catcherItemManager.getStoredCount(item);
        int maxStored = plugin.getConfig().getInt("limits.max-stored-per-catcher", 100);
        int usesRemaining = catcherItemManager.getUses(item);

        if (storedCount >= maxStored) {
            String msg = plugin.getConfig().getString(
                MessageUtil.Keys.CATCHER_FULL,
                "&cThis catcher has &e%catcher-uses% &cuses, but can only hold &e%max-stored% &cmobs. Release some before capturing more."
            );

            msg = msg.replace("%catcher-uses%", usesRemaining == -1 ? "Unlimited" : String.valueOf(usesRemaining));
            msg = msg.replace("%max-stored%", String.valueOf(maxStored));
            MessageUtil.sendRaw(plugin, player, msg);
            return;
        }

        if (catcherItemManager.hasStoredMobs(item)) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.SAME_STACK_ONLY, "&cThis shard can only stack the same mob type and variant.");
            return;
        }

        MessageUtil.send(plugin, player, MessageUtil.Keys.NO_USES, "&cThis catcher has no uses left.");
    }

    private void updateHeldItem(Player player, ItemStack item) {
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();
    }

    private void playCaptureFeedback(Player player, Entity clicked) {
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, clicked.getLocation().add(0, 1, 0), 10);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void sendCapturedEggMessage(Player player, String entityTypeName, String variant) {
        String prettyMob = MessageUtil.prettyLabel(entityTypeName);
        String prettyVariant = catcherItemManager.prettyVariant(variant);
        MessageUtil.send(
            plugin,
            player,
            MessageUtil.Keys.CAPTURED_EGG,
            "&aCaptured &f%mob%%variant% &ainto an egg.",
            "%mob%",
            prettyMob,
            "%variant%",
            prettyVariant.isEmpty() ? "" : " &7(" + prettyVariant + "&7)"
        );
    }

    private void sendCapturedStorageMessage(Player player, String entityTypeName, String variant, int storedCount) {
        String prettyMob = MessageUtil.prettyLabel(entityTypeName);
        String prettyVariant = catcherItemManager.prettyVariant(variant);
        MessageUtil.send(
            plugin,
            player,
            MessageUtil.Keys.CAPTURED,
            "&aCaptured &f%mob%%variant% &a(&f%stored%&a stored)",
            "%mob%",
            prettyMob,
            "%variant%",
            prettyVariant.isEmpty() ? "" : " &7(" + prettyVariant + "&7)",
            "%stored%",
            String.valueOf(storedCount)
        );
    }

    private CaptureMode resolveMode(Player player) {
        String configuredMode = plugin.getConfig().getString("capture.mode", "STORAGE");
        if (configuredMode == null) {
            return CaptureMode.STORAGE;
        }

        configuredMode = configuredMode.toUpperCase();
        if (configuredMode.equals("STORAGE")) {
            return CaptureMode.STORAGE;
        }
        if (configuredMode.equals("EGG")) {
            return CaptureMode.EGG;
        }

        String fallback = plugin.getConfig().getString("capture.hybrid.default", "STORAGE");
        CaptureMode fallbackMode = "EGG".equalsIgnoreCase(fallback) ? CaptureMode.EGG : CaptureMode.STORAGE;

        boolean hasStoragePerm = player.hasPermission("cnj.mobcatcher.mode.storage");
        boolean hasEggPerm = player.hasPermission("cnj.mobcatcher.mode.egg");
        boolean sneakForEgg = plugin.getConfig().getBoolean("capture.hybrid.admin-sneak-for-egg", true);

        if (hasStoragePerm && hasEggPerm && sneakForEgg) {
            return player.isSneaking() ? CaptureMode.EGG : CaptureMode.STORAGE;
        }

        if (hasEggPerm && !hasStoragePerm) {
            return CaptureMode.EGG;
        }

        if (hasStoragePerm && !hasEggPerm) {
            return CaptureMode.STORAGE;
        }

        return fallbackMode;
    }
}