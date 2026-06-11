package net.cnjgaming.mobcatcher.listener;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import net.cnjgaming.mobcatcher.manager.CatcherItemManager;
import net.cnjgaming.mobcatcher.util.MessageUtil;
import net.cnjgaming.mobcatcher.util.RuleUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ReleaseListener implements Listener {

    private final CNJMobCatcherPlugin plugin;
    private final CatcherItemManager catcherItemManager;

    public ReleaseListener(CNJMobCatcherPlugin plugin, CatcherItemManager catcherItemManager) {
        this.plugin = plugin;
        this.catcherItemManager = catcherItemManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRelease(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!player.hasPermission("cnj.mobcatcher.release")) return;

        if (catcherItemManager.isCapturedMobEgg(item)) {
            handleCustomEggRelease(event, player, item);
            return;
        }

        if (!catcherItemManager.isMobCatcher(item)) return;
        if (!catcherItemManager.hasStoredMobs(item)) return;

        releaseStoredMob(event, player, item);
    }

    private void releaseStoredMob(PlayerInteractEvent event, Player player, ItemStack item) {
        if (event.getClickedBlock() == null) {
            event.setCancelled(true);
            return;
        }

        if (blockIfWorldRestricted(player, event)) return;

        EntityType type = catcherItemManager.getCapturedEntityType(item);
        if (type == null) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.INVALID_DATA, "&cThis catcher contains invalid mob data.");
            return;
        }

        Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        if (blockIfClaimRestricted(player, spawnLoc, event)) return;

        if (!(spawnLoc.getWorld().spawnEntity(spawnLoc, type) instanceof LivingEntity living)) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.FAILED_RELEASE, "&cFailed to release mob.");
            return;
        }

        String customName = catcherItemManager.getCapturedCustomName(item);
        if (customName != null && !customName.isBlank()) {
            living.setCustomName("§f" + customName);
            living.setCustomNameVisible(true);
        }

        String variant = catcherItemManager.getStoredVariant(item);
        catcherItemManager.applyCapturedProperties(living, variant, "");

        catcherItemManager.removeOneStoredMob(item);

        setHeldItemAfterStorageRelease(player, item);
        playReleaseFeedback(player, spawnLoc);
        sendReleasedMessage(player, type, variant);
        event.setCancelled(true);
    }

    private void handleCustomEggRelease(PlayerInteractEvent event, Player player, ItemStack item) {
        if (blockIfWorldRestricted(player, event)) return;

        if (event.getClickedBlock() == null) {
            event.setCancelled(true);
            return;
        }

        Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        if (blockIfClaimRestricted(player, spawnLoc, event)) return;

        EntityType type = catcherItemManager.getEggEntityType(item);
        if (type == null) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.INVALID_EGG_DATA, "&cThis egg contains invalid mob data.");
            event.setCancelled(true);
            return;
        }

        if (!(spawnLoc.getWorld().spawnEntity(spawnLoc, type) instanceof LivingEntity living)) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.FAILED_RELEASE, "&cFailed to release mob.");
            event.setCancelled(true);
            return;
        }

        String customName = catcherItemManager.getEggCustomName(item);
        if (customName != null && !customName.isBlank()) {
            living.setCustomName("§f" + customName);
            living.setCustomNameVisible(true);
        }

        String variant = catcherItemManager.getEggVariant(item);
        String properties = catcherItemManager.getEggProperties(item);
        catcherItemManager.applyCapturedProperties(living, variant, properties);

        consumeOneEgg(player, item);
        playReleaseFeedback(player, spawnLoc);
        sendReleasedMessage(player, type, variant);
        event.setCancelled(true);
    }

    private boolean blockIfWorldRestricted(Player player, PlayerInteractEvent event) {
        boolean blocked = !player.hasPermission("cnj.mobcatcher.bypass.worlds")
                && RuleUtil.isWorldBlocked(plugin, player.getWorld().getName(), "release");
        if (blocked) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.WORLD_BLOCKED_RELEASE, "&cYou cannot release mobs in this world.");
            event.setCancelled(true);
        }
        return blocked;
    }

    private boolean blockIfClaimRestricted(Player player, Location spawnLoc, PlayerInteractEvent event) {
        boolean blocked = !player.hasPermission("cnj.mobcatcher.bypass.claims")
                && RuleUtil.isBlockedByOtherClaim(plugin, player, spawnLoc);
        if (blocked) {
            MessageUtil.send(plugin, player, MessageUtil.Keys.CLAIM_BLOCKED_RELEASE, "&cYou cannot release mobs on someone else's claim.");
            event.setCancelled(true);
        }
        return blocked;
    }

    private void setHeldItemAfterStorageRelease(Player player, ItemStack item) {
        if (catcherItemManager.getStoredCount(item) == 0 && catcherItemManager.getUses(item) == 0) {
            player.getInventory().setItemInMainHand(new ItemStack(org.bukkit.Material.AIR));
        } else {
            player.getInventory().setItemInMainHand(item);
        }
        player.updateInventory();
    }

    private void consumeOneEgg(Player player, ItemStack item) {
        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(org.bukkit.Material.AIR));
        } else {
            item.setAmount(amount - 1);
            player.getInventory().setItemInMainHand(item);
        }
        player.updateInventory();
    }

    private void playReleaseFeedback(Player player, Location spawnLoc) {
        player.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 12);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.0f);
    }

    private void sendReleasedMessage(Player player, EntityType type, String variant) {
        String prettyMob = MessageUtil.prettyLabel(type.name());
        String prettyVariant = catcherItemManager.prettyVariant(variant);
        MessageUtil.send(
            plugin,
            player,
            MessageUtil.Keys.RELEASED,
            "&aReleased &f%mob%%variant%&a.",
            "%mob%",
            prettyMob,
            "%variant%",
            prettyVariant.isEmpty() ? "" : " &7(" + prettyVariant + "&7)"
        );
    }
}