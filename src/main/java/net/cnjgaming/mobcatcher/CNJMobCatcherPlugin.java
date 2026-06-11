package net.cnjgaming.mobcatcher;

import net.cnjgaming.mobcatcher.command.MobCatcherCommand;
import net.cnjgaming.mobcatcher.listener.CaptureListener;
import net.cnjgaming.mobcatcher.listener.ReleaseListener;
import net.cnjgaming.mobcatcher.manager.CatcherItemManager;
import net.cnjgaming.mobcatcher.manager.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CNJMobCatcherPlugin extends JavaPlugin {

    private CatcherItemManager catcherItemManager;
    private RecipeManager recipeManager;
    private final NamespacedKey recipeKey = new NamespacedKey(this, "mob_catcher");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        validateAndNormalizeConfig();

        logDependencyState();

        this.catcherItemManager = new CatcherItemManager(this);
        this.recipeManager = new RecipeManager(this, catcherItemManager);

        registerListeners();

        if (!registerCommand()) {
            getLogger().severe("Command 'mobcatcher' is missing from plugin.yml. Disabling plugin to prevent partial startup.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCraftingRecipe();

        String mode = getConfig().getString("capture.mode", "STORAGE");
        boolean craftingEnabled = getConfig().getBoolean("crafting.enabled", true);
        getLogger().info("CNJMobCatcher enabled. Mode=" + mode + ", Crafting=" + (craftingEnabled ? "enabled" : "disabled"));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CaptureListener(this, catcherItemManager), this);
        getServer().getPluginManager().registerEvents(new ReleaseListener(this, catcherItemManager), this);
    }

    private boolean registerCommand() {
        PluginCommand pluginCommand = getCommand("mobcatcher");
        if (pluginCommand == null) {
            return false;
        }

        MobCatcherCommand command = new MobCatcherCommand(this, catcherItemManager, recipeManager);
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        return true;
    }

    private void registerCraftingRecipe() {
        try {
            recipeManager.registerRecipes();
        } catch (Exception ex) {
            getLogger().warning("Failed to register crafting recipe: " + ex.getMessage());
            getLogger().warning("Plugin will continue without the crafting recipe.");
        }
    }

    private void logDependencyState() {
        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            getLogger().info("GriefPrevention detected. Claim checks are available when enabled in config.");
        } else {
            getLogger().info("GriefPrevention not detected. Claim checks will be skipped.");
        }
    }

    private void validateAndNormalizeConfig() {
        FileConfiguration config = getConfig();
        boolean changed = false;

        String configuredMaterial = config.getString("item.material", "AMETHYST_SHARD");
        Material parsedMaterial = configuredMaterial == null ? null : Material.matchMaterial(configuredMaterial);
        if (parsedMaterial == null) {
            getLogger().warning("Config item.material='" + configuredMaterial + "' is invalid. Falling back to AMETHYST_SHARD.");
            config.set("item.material", "AMETHYST_SHARD");
            changed = true;
        }

        List<Integer> allowedGiveAmounts = config.getIntegerList("uses.allowed-give-amounts");
        if (allowedGiveAmounts.isEmpty()) {
            getLogger().warning("Config uses.allowed-give-amounts is empty. Restoring safe defaults.");
            config.set("uses.allowed-give-amounts", new ArrayList<>(Arrays.asList(1, 3, 5, 10, 25, 50, 75, 100, -1)));
            changed = true;
        }

        String captureMode = config.getString("capture.mode", "STORAGE");
        if (!isAllowedValue(captureMode, "STORAGE", "EGG", "HYBRID")) {
            getLogger().warning("Config capture.mode='" + captureMode + "' is invalid. Falling back to STORAGE.");
            config.set("capture.mode", "STORAGE");
            changed = true;
        }

        String hybridDefault = config.getString("capture.hybrid.default", "STORAGE");
        if (!isAllowedValue(hybridDefault, "STORAGE", "EGG")) {
            getLogger().warning("Config capture.hybrid.default='" + hybridDefault + "' is invalid. Falling back to STORAGE.");
            config.set("capture.hybrid.default", "STORAGE");
            changed = true;
        }

        String worldsMode = config.getString("worlds.mode", "ALLOWLIST");
        if (!isAllowedValue(worldsMode, "ALLOWLIST", "DENYLIST")) {
            getLogger().warning("Config worlds.mode='" + worldsMode + "' is invalid. Falling back to ALLOWLIST.");
            config.set("worlds.mode", "ALLOWLIST");
            changed = true;
        }

        if (changed) {
            saveConfig();
            getLogger().info("Config contained invalid values and was normalized with safe defaults.");
        }
    }

    private boolean isAllowedValue(String value, String... allowedValues) {
        if (value == null) return false;

        String candidate = value.toUpperCase(Locale.ROOT);
        for (String allowed : allowedValues) {
            if (candidate.equals(allowed)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onDisable() {
        Bukkit.removeRecipe(recipeKey);
        getLogger().info("CNJMobCatcher disabled.");
    }

    public CatcherItemManager getCatcherItemManager() {
        return catcherItemManager;
    }
}