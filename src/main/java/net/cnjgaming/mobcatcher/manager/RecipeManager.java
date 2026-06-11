package net.cnjgaming.mobcatcher.manager;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

public class RecipeManager {

    private final CNJMobCatcherPlugin plugin;
    private final CatcherItemManager catcherItemManager;

    public RecipeManager(CNJMobCatcherPlugin plugin, CatcherItemManager catcherItemManager) {
        this.plugin = plugin;
        this.catcherItemManager = catcherItemManager;
    }

    public void registerRecipes() {
        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) {
            plugin.getLogger().info("Crafting recipe is disabled in config.");
            return;
        }

        int craftedUses = plugin.getConfig().getInt("uses.default-crafted-uses", -1);
        ItemStack result = catcherItemManager.createEmptyCatcher(craftedUses);

        NamespacedKey key = new NamespacedKey(plugin, "mob_catcher");
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        List<String> shape = plugin.getConfig().getStringList("crafting.shape");
        if (shape.size() != 3) {
            plugin.getLogger().warning("Invalid crafting.shape in config. Expected exactly 3 rows, got " + shape.size());
            return;
        }

        for (int i = 0; i < 3; i++) {
            String row = shape.get(i);
            if (row == null || row.isEmpty()) {
                plugin.getLogger().warning("Invalid crafting.shape row " + (i + 1) + ": empty or null string. Config may be corrupted.");
                return;
            }
        }

        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        ConfigurationSection ingredients = plugin.getConfig().getConfigurationSection("crafting.ingredients");
        if (ingredients == null) {
            plugin.getLogger().warning("Missing crafting.ingredients section in config.");
            return;
        }

        for (String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1) {
                plugin.getLogger().warning("Invalid ingredient key: " + symbol + " (must be 1 character)");
                continue;
            }

            String materialName = ingredients.getString(symbol);
            if (materialName == null) {
                plugin.getLogger().warning("Missing material for recipe symbol: " + symbol);
                continue;
            }

            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Invalid material in recipe config: " + materialName);
                continue;
            }

            recipe.setIngredient(symbol.charAt(0), material);
        }

        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);

        plugin.getLogger().info("Registered Mob Catcher crafting recipe.");
    }
}