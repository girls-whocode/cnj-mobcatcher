package net.cnjgaming.mobcatcher.manager;

import net.cnjgaming.mobcatcher.CNJMobCatcherPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatcherItemManager {

    private final CNJMobCatcherPlugin plugin;

    private final NamespacedKey catcherKey;
    private final NamespacedKey usesKey;
    private final NamespacedKey entityTypeKey;
    private final NamespacedKey customNameKey;
    private final NamespacedKey storedCountKey;
    private final NamespacedKey variantKey;
    private final NamespacedKey eggKey;
    private final NamespacedKey eggTypeKey;
    private final NamespacedKey eggCustomNameKey;
    private final NamespacedKey eggVariantKey;
    private final NamespacedKey eggPropertiesKey;

    public CatcherItemManager(CNJMobCatcherPlugin plugin) {
        this.plugin = plugin;
        this.catcherKey = new NamespacedKey(plugin, "mob_catcher");
        this.usesKey = new NamespacedKey(plugin, "uses");
        this.entityTypeKey = new NamespacedKey(plugin, "captured_entity_type");
        this.customNameKey = new NamespacedKey(plugin, "captured_custom_name");
        this.storedCountKey = new NamespacedKey(plugin, "stored_count");
        this.variantKey = new NamespacedKey(plugin, "captured_variant");
        this.eggKey = new NamespacedKey(plugin, "captured_mob_egg");
        this.eggTypeKey = new NamespacedKey(plugin, "captured_mob_egg_type");
        this.eggCustomNameKey = new NamespacedKey(plugin, "captured_mob_egg_custom_name");
        this.eggVariantKey = new NamespacedKey(plugin, "captured_mob_egg_variant");
        this.eggPropertiesKey = new NamespacedKey(plugin, "captured_mob_egg_properties");
    }

    public ItemStack createEmptyCatcher(int uses) {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.matchMaterial(config.getString("item.material", "AMETHYST_SHARD"));
        if (material == null) {
            material = Material.AMETHYST_SHARD;
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color(config.getString("item.name", "&dMob Catcher")));
        meta.setLore(buildEmptyLore(uses));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(catcherKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(usesKey, PersistentDataType.INTEGER, uses);
        pdc.set(storedCountKey, PersistentDataType.INTEGER, 0);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isMobCatcher(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(catcherKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public int getUses(ItemStack item) {
        if (!isMobCatcher(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer value = meta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public int getStoredCount(ItemStack item) {
        if (!isMobCatcher(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer value = meta.getPersistentDataContainer().get(storedCountKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public boolean hasStoredMobs(ItemStack item) {
        return getStoredCount(item) > 0;
    }

    public boolean isFilled(ItemStack item) {
        return hasStoredMobs(item);
    }

    public boolean hasUsesRemaining(ItemStack item) {
        int uses = getUses(item);
        return uses == -1 || uses > 0;
    }

    public boolean consumeOneUse(ItemStack item) {
        if (!isMobCatcher(item)) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer uses = pdc.get(usesKey, PersistentDataType.INTEGER);
        int currentUses = uses == null ? 0 : uses;

        if (currentUses == 0) {
            return false;
        }

        if (currentUses > 0) {
            currentUses--;
            pdc.set(usesKey, PersistentDataType.INTEGER, currentUses);
        }

        int storedCount = getStoredCount(item);
        String mob = pdc.get(entityTypeKey, PersistentDataType.STRING);
        String storedVariant = pdc.get(variantKey, PersistentDataType.STRING);
        if (storedCount > 0 && mob != null) {
            meta.setLore(buildFilledLore(currentUses, mob, storedCount, storedVariant));
        } else {
            meta.setLore(buildEmptyLore(currentUses));
        }

        item.setItemMeta(meta);
        return true;
    }

    public void setUses(ItemStack item, int uses) {
        if (!isMobCatcher(item)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(usesKey, PersistentDataType.INTEGER, uses);

        int storedCount = getStoredCount(item);
        String mob = pdc.get(entityTypeKey, PersistentDataType.STRING);
        String storedVariant = pdc.get(variantKey, PersistentDataType.STRING);

        if (storedCount > 0 && mob != null) {
            meta.setLore(buildFilledLore(uses, mob, storedCount, storedVariant));
        } else {
            meta.setLore(buildEmptyLore(uses));
        }

        item.setItemMeta(meta);
    }

    public boolean storeCapturedMob(ItemStack item, EntityType type, String customName, String variant) {
        if (!isMobCatcher(item)) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Integer uses = pdc.get(usesKey, PersistentDataType.INTEGER);
        int currentUses = uses == null ? 0 : uses;

        if (currentUses == 0) {
            return false;
        }

        Integer stored = pdc.get(storedCountKey, PersistentDataType.INTEGER);
        int currentStored = stored == null ? 0 : stored;

        String existingType = pdc.get(entityTypeKey, PersistentDataType.STRING);
        String existingVariant = pdc.get(variantKey, PersistentDataType.STRING);
        String normalizedVariant = (variant == null || variant.isBlank()) ? "DEFAULT" : variant;

        if (currentStored > 0) {
            if (existingType != null && !existingType.equals(type.name())) {
                return false;
            }
            if (existingVariant != null && !existingVariant.equals(normalizedVariant)) {
                return false;
            }
        }

        pdc.set(entityTypeKey, PersistentDataType.STRING, type.name());
        pdc.set(variantKey, PersistentDataType.STRING, normalizedVariant);

        if (customName != null && !customName.isBlank()) {
            pdc.set(customNameKey, PersistentDataType.STRING, customName);
        } else {
            pdc.remove(customNameKey);
        }

        currentStored++;
        pdc.set(storedCountKey, PersistentDataType.INTEGER, currentStored);

        if (currentUses > 0) {
            currentUses--;
            pdc.set(usesKey, PersistentDataType.INTEGER, currentUses);
        }

        meta.setLore(buildFilledLore(currentUses, type.name(), currentStored, normalizedVariant));
        item.setItemMeta(meta);
        return true;
    }

    public String getVariantSignature(Entity entity) {
        if (entity instanceof Sheep sheep) {
            return sheep.getColor().name();
        }

        if (entity instanceof Frog frog) {
            return frog.getVariant().name();
        }

        if (entity instanceof Axolotl axolotl) {
            return axolotl.getVariant().name();
        }

        if (entity instanceof Villager villager) {
            return "TYPE_" + villager.getVillagerType().name();
        }

        return "DEFAULT";
    }

    public String captureProperties(Entity entity) {
        Map<String, String> properties = new HashMap<>();

        if (entity instanceof Villager villager) {
            properties.put("villagerType", villager.getVillagerType().name());
            properties.put("villagerProfession", villager.getProfession().name());
            properties.put("villagerLevel", String.valueOf(villager.getVillagerLevel()));
        }

        if (entity instanceof Axolotl axolotl) {
            properties.put("axolotlVariant", axolotl.getVariant().name());
        }

        if (entity instanceof Frog frog) {
            properties.put("frogVariant", frog.getVariant().name());
        }

        if (entity instanceof Sheep sheep) {
            properties.put("sheepColor", sheep.getColor().name());
        }

        return encodeProperties(properties);
    }

    public ItemStack createCapturedEgg(EntityType type, String customName, String variant, String properties) {
        Material eggMaterial = Material.matchMaterial(type.name() + "_SPAWN_EGG");
        if (eggMaterial == null) {
            eggMaterial = Material.EGG;
        }

        ItemStack egg = new ItemStack(eggMaterial, 1);
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return egg;

        String prettyMob = pretty(type.name());
        String prettyVariant = prettyVariant(variant);

        meta.setDisplayName(color("&6Captured " + prettyMob));

        List<String> lore = new ArrayList<>();
        lore.add(color("&7Contains: &f" + prettyMob));
        if (!prettyVariant.isEmpty()) {
            lore.add(color("&7Variant: &f" + prettyVariant));
        }
        lore.add(color("&eRight-click a block to release."));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(eggKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(eggTypeKey, PersistentDataType.STRING, type.name());
        pdc.set(eggVariantKey, PersistentDataType.STRING, normalizeVariant(variant));

        if (customName != null && !customName.isBlank()) {
            pdc.set(eggCustomNameKey, PersistentDataType.STRING, customName);
        }

        if (properties != null && !properties.isBlank()) {
            pdc.set(eggPropertiesKey, PersistentDataType.STRING, properties);
        }

        egg.setItemMeta(meta);
        return egg;
    }

    public boolean isCapturedMobEgg(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(eggKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public EntityType getEggEntityType(ItemStack item) {
        if (!isCapturedMobEgg(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String raw = meta.getPersistentDataContainer().get(eggTypeKey, PersistentDataType.STRING);
        if (raw == null) return null;

        try {
            return EntityType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getEggCustomName(ItemStack item) {
        if (!isCapturedMobEgg(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(eggCustomNameKey, PersistentDataType.STRING);
    }

    public String getEggVariant(ItemStack item) {
        if (!isCapturedMobEgg(item)) return "DEFAULT";

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "DEFAULT";

        String value = meta.getPersistentDataContainer().get(eggVariantKey, PersistentDataType.STRING);
        return value == null ? "DEFAULT" : value;
    }

    public String getEggProperties(ItemStack item) {
        if (!isCapturedMobEgg(item)) return "";

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";

        String value = meta.getPersistentDataContainer().get(eggPropertiesKey, PersistentDataType.STRING);
        return value == null ? "" : value;
    }

    public void applyCapturedProperties(Entity entity, String variant, String encodedProperties) {
        String normalizedVariant = normalizeVariant(variant);

        if (entity instanceof Sheep sheep && !normalizedVariant.equals("DEFAULT")) {
            try {
                sheep.setColor(org.bukkit.DyeColor.valueOf(normalizedVariant));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (entity instanceof Frog frog && !normalizedVariant.equals("DEFAULT")) {
            try {
                frog.setVariant(Frog.Variant.valueOf(normalizedVariant));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (entity instanceof Axolotl axolotl && !normalizedVariant.equals("DEFAULT")) {
            try {
                axolotl.setVariant(Axolotl.Variant.valueOf(normalizedVariant));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (entity instanceof Villager villager && normalizedVariant.startsWith("TYPE_")) {
            String typeRaw = normalizedVariant.substring("TYPE_".length());
            try {
                villager.setVillagerType(Villager.Type.valueOf(typeRaw));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Map<String, String> properties = decodeProperties(encodedProperties);
        if (properties.isEmpty()) {
            return;
        }

        if (entity instanceof Villager villager) {
            String villagerType = properties.get("villagerType");
            if (villagerType != null) {
                try {
                    villager.setVillagerType(Villager.Type.valueOf(villagerType));
                } catch (IllegalArgumentException ignored) {
                }
            }

            String profession = properties.get("villagerProfession");
            if (profession != null) {
                try {
                    villager.setProfession(Villager.Profession.valueOf(profession));
                } catch (IllegalArgumentException ignored) {
                }
            }

            String level = properties.get("villagerLevel");
            if (level != null) {
                try {
                    villager.setVillagerLevel(Integer.parseInt(level));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (entity instanceof Axolotl axolotl) {
            String axolotlVariant = properties.get("axolotlVariant");
            if (axolotlVariant != null) {
                try {
                    axolotl.setVariant(Axolotl.Variant.valueOf(axolotlVariant));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (entity instanceof Frog frog) {
            String frogVariant = properties.get("frogVariant");
            if (frogVariant != null) {
                try {
                    frog.setVariant(Frog.Variant.valueOf(frogVariant));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (entity instanceof Sheep sheep) {
            String sheepColor = properties.get("sheepColor");
            if (sheepColor != null) {
                try {
                    sheep.setColor(org.bukkit.DyeColor.valueOf(sheepColor));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public String getStoredVariant(ItemStack item) {
        if (!isMobCatcher(item)) return "DEFAULT";

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "DEFAULT";

        String value = meta.getPersistentDataContainer().get(variantKey, PersistentDataType.STRING);
        return value == null ? "DEFAULT" : value;
    }

    public EntityType getCapturedEntityType(ItemStack item) {
        if (!hasStoredMobs(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String raw = meta.getPersistentDataContainer().get(entityTypeKey, PersistentDataType.STRING);
        if (raw == null) return null;

        try {
            return EntityType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getCapturedCustomName(ItemStack item) {
        if (!hasStoredMobs(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(customNameKey, PersistentDataType.STRING);
    }

    public void removeOneStoredMob(ItemStack item) {
        if (!isMobCatcher(item)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Integer stored = pdc.get(storedCountKey, PersistentDataType.INTEGER);
        int currentStored = stored == null ? 0 : stored;

        if (currentStored <= 0) {
            return;
        }

        currentStored--;
        pdc.set(storedCountKey, PersistentDataType.INTEGER, currentStored);

        String mobType = pdc.get(entityTypeKey, PersistentDataType.STRING);
        String storedVariant = pdc.get(variantKey, PersistentDataType.STRING);
        Integer uses = pdc.get(usesKey, PersistentDataType.INTEGER);
        int currentUses = uses == null ? 0 : uses;

        if (currentStored <= 0) {
            pdc.remove(entityTypeKey);
            pdc.remove(customNameKey);
            pdc.remove(variantKey);
            meta.setLore(buildEmptyLore(currentUses));
        } else {
            meta.setLore(buildFilledLore(currentUses, mobType == null ? "UNKNOWN" : mobType, currentStored, storedVariant));
        }

        item.setItemMeta(meta);
    }

    private List<String> buildEmptyLore(int uses) {
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Use this to capture a mob."));
        lore.add("");
        lore.add(color("&7Stored: &f0"));
        lore.add(color("&7Uses Remaining: &e" + formatUses(uses)));
        return lore;
    }

    private List<String> buildFilledLore(int uses, String mob, int storedCount, String variant) {
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Captured: &f" + pretty(mob)));

        if (variant != null && !variant.equalsIgnoreCase("DEFAULT")) {
            lore.add(color("&7Variant: &f" + pretty(variant)));
        }

        lore.add(color("&7Stored: &f" + storedCount));
        lore.add(color("&7Uses Remaining: &e" + formatUses(uses)));
        lore.add("");
        lore.add(color("&eRight-click a block to release one."));
        return lore;
    }

    private String formatUses(int uses) {
        return uses == -1 ? "Unlimited" : String.valueOf(uses);
    }

    private String pretty(String input) {
        String lower = input.toLowerCase().replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return out.toString().trim();
    }

    public String prettyVariant(String variant) {
        if (variant == null || variant.isBlank() || variant.equalsIgnoreCase("DEFAULT")) {
            return "";
        }
        if (variant.startsWith("TYPE_")) {
            return pretty(variant.substring("TYPE_".length()));
        }
        return pretty(variant);
    }

    private String normalizeVariant(String variant) {
        return (variant == null || variant.isBlank()) ? "DEFAULT" : variant;
    }

    private String encodeProperties(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append(';');
            }

            sb.append(escapePropertyPart(key)).append('=').append(escapePropertyPart(value));
        }
        return sb.toString();
    }

    private Map<String, String> decodeProperties(String encodedProperties) {
        Map<String, String> out = new HashMap<>();
        if (encodedProperties == null || encodedProperties.isBlank()) {
            return out;
        }

        String[] pairs = encodedProperties.split(";");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq <= 0 || eq >= pair.length() - 1) {
                continue;
            }

            String rawKey = pair.substring(0, eq);
            String rawValue = pair.substring(eq + 1);
            out.put(unescapePropertyPart(rawKey), unescapePropertyPart(rawValue));
        }
        return out;
    }

    private String escapePropertyPart(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=");
    }

    private String unescapePropertyPart(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                out.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }

        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }

    private String color(String s) {
        return s.replace("&", "§");
    }
}