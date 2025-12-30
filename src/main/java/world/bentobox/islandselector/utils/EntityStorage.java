package world.bentobox.islandselector.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import world.bentobox.islandselector.IslandSelector;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles entity serialization and storage for slot switching and relocation.
 *
 * Since FAWE's entity handling is unreliable (null NBT errors, async issues),
 * this class provides a separate, reliable mechanism for saving and restoring
 * entities during island operations.
 *
 * Entities are serialized using Bukkit's serialization API and stored as
 * compressed files alongside the schematic files.
 */
public class EntityStorage {

    private final IslandSelector addon;

    // Cached max health attribute - determined at runtime for MC version compatibility
    // MC 1.21+ uses MAX_HEALTH (and Attribute is an interface), older versions use GENERIC_MAX_HEALTH (enum)
    private static Attribute maxHealthAttribute;

    static {
        maxHealthAttribute = resolveMaxHealthAttribute();
    }

    /**
     * Resolve the max health attribute across different MC versions.
     * MC 1.21+: Attribute is an interface with static fields (MAX_HEALTH)
     * Older MC: Attribute is an enum with GENERIC_MAX_HEALTH
     */
    private static Attribute resolveMaxHealthAttribute() {
        // Try accessing as a static field first (works for both interface and enum)
        String[] fieldNames = {"MAX_HEALTH", "GENERIC_MAX_HEALTH"};

        for (String fieldName : fieldNames) {
            try {
                java.lang.reflect.Field field = Attribute.class.getField(fieldName);
                Object value = field.get(null);
                if (value instanceof Attribute) {
                    return (Attribute) value;
                }
            } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
                // Try next field name
            }
        }

        // Fallback: try Registry lookup (MC 1.21+ style)
        try {
            org.bukkit.Registry<Attribute> registry = org.bukkit.Registry.ATTRIBUTE;
            if (registry != null) {
                // Try both namespaced keys
                for (String key : new String[]{"max_health", "generic.max_health"}) {
                    try {
                        Attribute attr = registry.get(org.bukkit.NamespacedKey.minecraft(key));
                        if (attr != null) {
                            return attr;
                        }
                    } catch (Exception ignored) {
                        // Try next key
                    }
                }
            }
        } catch (Exception ignored) {
            // Registry not available or lookup failed
        }

        return null;
    }

    public EntityStorage(IslandSelector addon) {
        this.addon = addon;
    }

    // ==================== MC VERSION COMPATIBILITY HELPERS ====================

    /**
     * Get the string key for a Villager Profession.
     * Works with both old enum API and new interface/Keyed API (MC 1.21+).
     */
    private static String getProfessionKey(Villager.Profession profession) {
        if (profession == null) return null;
        try {
            // MC 1.21+: Profession implements Keyed, use getKey()
            if (profession instanceof org.bukkit.Keyed) {
                return ((org.bukkit.Keyed) profession).getKey().getKey();
            }
        } catch (Exception ignored) {}
        // Fallback: use reflection to call name() for older enum-based API
        try {
            java.lang.reflect.Method nameMethod = profession.getClass().getMethod("name");
            return (String) nameMethod.invoke(profession);
        } catch (Exception ignored) {}
        return profession.toString();
    }

    /**
     * Get the string key for a Villager Type.
     * Works with both old enum API and new interface/Keyed API (MC 1.21+).
     */
    private static String getVillagerTypeKey(Villager.Type villagerType) {
        if (villagerType == null) return null;
        try {
            // MC 1.21+: Type implements Keyed, use getKey()
            if (villagerType instanceof org.bukkit.Keyed) {
                return ((org.bukkit.Keyed) villagerType).getKey().getKey();
            }
        } catch (Exception ignored) {}
        // Fallback: use reflection to call name() for older enum-based API
        try {
            java.lang.reflect.Method nameMethod = villagerType.getClass().getMethod("name");
            return (String) nameMethod.invoke(villagerType);
        } catch (Exception ignored) {}
        return villagerType.toString();
    }

    /**
     * Look up a Villager Profession by key string.
     * Works with both old enum API and new Registry API (MC 1.21+).
     */
    private static Villager.Profession getProfessionByKey(String key) {
        if (key == null) return null;

        // Try Registry lookup first (MC 1.21+)
        try {
            org.bukkit.Registry<Villager.Profession> registry = org.bukkit.Registry.VILLAGER_PROFESSION;
            if (registry != null) {
                // Try lowercase key (registry format)
                Villager.Profession profession = registry.get(org.bukkit.NamespacedKey.minecraft(key.toLowerCase()));
                if (profession != null) return profession;
            }
        } catch (Exception ignored) {}

        // Try accessing as static field (works for both old enum and new interface)
        try {
            java.lang.reflect.Field field = Villager.Profession.class.getField(key.toUpperCase());
            Object value = field.get(null);
            if (value instanceof Villager.Profession) {
                return (Villager.Profession) value;
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Look up a Villager Type by key string.
     * Works with both old enum API and new Registry API (MC 1.21+).
     */
    private static Villager.Type getVillagerTypeByKey(String key) {
        if (key == null) return null;

        // Try Registry lookup first (MC 1.21+)
        try {
            org.bukkit.Registry<Villager.Type> registry = org.bukkit.Registry.VILLAGER_TYPE;
            if (registry != null) {
                // Try lowercase key (registry format)
                Villager.Type type = registry.get(org.bukkit.NamespacedKey.minecraft(key.toLowerCase()));
                if (type != null) return type;
            }
        } catch (Exception ignored) {}

        // Try accessing as static field (works for both old enum and new interface)
        try {
            java.lang.reflect.Field field = Villager.Type.class.getField(key.toUpperCase());
            Object value = field.get(null);
            if (value instanceof Villager.Type) {
                return (Villager.Type) value;
            }
        } catch (Exception ignored) {}

        return null;
    }

    // ==================== IN-MEMORY CAPTURE/RESTORE (FOR RELOCATION) ====================

    /**
     * Capture all non-player entities in a region to an in-memory list.
     * This is used for relocation where we don't need to save to disk.
     * Must be called from the main thread.
     *
     * @param world The world
     * @param center The center location
     * @param range The range from center
     * @return List of serialized entities (opaque - pass to restoreEntitiesInMemory)
     */
    public List<Object> captureEntitiesInMemory(World world, Location center, int range) {
        List<Object> captured = new ArrayList<>();

        if (world == null || center == null) {
            return captured;
        }

        if (!Bukkit.isPrimaryThread()) {
            addon.logError("EntityStorage.captureEntitiesInMemory must be called from main thread!");
            return captured;
        }

        int minX = center.getBlockX() - range;
        int maxX = center.getBlockX() + range;
        int minZ = center.getBlockZ() - range;
        int maxZ = center.getBlockZ() + range;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }

            Location loc = entity.getLocation();

            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {

                try {
                    SerializedEntity serialized = serializeEntity(entity, center);
                    if (serialized != null) {
                        captured.add(serialized);
                    }
                } catch (Exception e) {
                    addon.logWarning("Failed to capture entity " + entity.getType() + ": " + e.getMessage());
                }
            }
        }

        addon.log("Captured " + captured.size() + " entities in memory for relocation");
        return captured;
    }

    /**
     * Restore entities from an in-memory capture at a new location.
     * This is used for relocation where entities were captured with captureEntitiesInMemory.
     * Must be called from the main thread.
     *
     * NOTE: This does NOT clear existing entities first - call removeEntities() before this if needed.
     *
     * @param capturedEntities The list from captureEntitiesInMemory
     * @param newCenter The new center location to restore at
     * @return Number of entities restored
     */
    public int restoreEntitiesInMemory(List<Object> capturedEntities, Location newCenter) {
        if (capturedEntities == null || capturedEntities.isEmpty() || newCenter == null) {
            return 0;
        }

        if (!Bukkit.isPrimaryThread()) {
            addon.logError("EntityStorage.restoreEntitiesInMemory must be called from main thread!");
            return 0;
        }

        World world = newCenter.getWorld();
        if (world == null) {
            return 0;
        }

        int restored = 0;
        for (Object obj : capturedEntities) {
            if (!(obj instanceof SerializedEntity)) {
                continue;
            }

            SerializedEntity serialized = (SerializedEntity) obj;
            try {
                Entity entity = deserializeEntity(serialized, world, newCenter);
                if (entity != null) {
                    restored++;
                }
            } catch (Exception e) {
                addon.logWarning("Failed to restore entity " + serialized.entityType + ": " + e.getMessage());
            }
        }

        addon.log("Restored " + restored + " of " + capturedEntities.size() + " entities from memory");
        return restored;
    }

    // ==================== FILE-BASED SAVE/LOAD ====================

    /**
     * Capture all non-player entities in a region and serialize them to a file.
     * Must be called from the main thread.
     *
     * @param world The world
     * @param center The center location
     * @param range The range from center
     * @param file The file to save to (will use .entities extension)
     * @return Number of entities saved, or -1 on error
     */
    public int saveEntities(World world, Location center, int range, File file) {
        if (world == null || center == null) {
            return -1;
        }

        // Ensure we're on the main thread
        if (!Bukkit.isPrimaryThread()) {
            addon.logError("EntityStorage.saveEntities must be called from main thread!");
            return -1;
        }

        List<SerializedEntity> entities = new ArrayList<>();

        int minX = center.getBlockX() - range;
        int maxX = center.getBlockX() + range;
        int minZ = center.getBlockZ() - range;
        int maxZ = center.getBlockZ() + range;

        for (Entity entity : world.getEntities()) {
            // Skip players
            if (entity instanceof Player) {
                continue;
            }

            Location loc = entity.getLocation();

            // Check if entity is within the region
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {

                try {
                    SerializedEntity serialized = serializeEntity(entity, center);
                    if (serialized != null) {
                        entities.add(serialized);
                    }
                } catch (Exception e) {
                    addon.logWarning("Failed to serialize entity " + entity.getType() + ": " + e.getMessage());
                }
            }
        }

        // Save to file
        File entityFile = getEntityFile(file);
        try {
            entityFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(entityFile);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(gzos)) {

                oos.writeObject(entities);
            }

            addon.log("Saved " + entities.size() + " entities to " + entityFile.getName());
            return entities.size();

        } catch (Exception e) {
            addon.logError("Failed to save entities: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Load entities from a file and spawn them in the world.
     * Must be called from the main thread.
     *
     * IMPORTANT: This method assumes the area has already been cleared of entities!
     * Call removeEntities() before calling this method to prevent duplication.
     *
     * @param world The world to spawn entities in
     * @param center The center location (entities are spawned relative to this)
     * @param file The schematic file (will look for .entities file)
     * @return Number of entities restored, or -1 on error
     */
    @SuppressWarnings("unchecked")
    public int loadEntities(World world, Location center, File file) {
        if (world == null || center == null) {
            return -1;
        }

        // Ensure we're on the main thread
        if (!Bukkit.isPrimaryThread()) {
            addon.logError("EntityStorage.loadEntities must be called from main thread!");
            return -1;
        }

        File entityFile = getEntityFile(file);
        if (!entityFile.exists()) {
            addon.log("No entity file found: " + entityFile.getName() + " (this is normal for new islands)");
            return 0;
        }

        List<SerializedEntity> entities;
        try (FileInputStream fis = new FileInputStream(entityFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(gzis)) {

            entities = (List<SerializedEntity>) ois.readObject();

        } catch (Exception e) {
            addon.logError("Failed to load entities: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        // Calculate the range based on saved entities (approximate)
        int maxRange = 200; // Default range
        if (!entities.isEmpty()) {
            double maxDist = 0;
            for (SerializedEntity se : entities) {
                double dist = Math.max(Math.abs(se.relX), Math.abs(se.relZ));
                if (dist > maxDist) maxDist = dist;
            }
            maxRange = (int) maxDist + 10;
        }

        // CRITICAL: Remove any existing entities in the area FIRST to prevent duplication
        // This handles cases where FAWE might have already placed some entities
        addon.log("Clearing existing entities before restoration to prevent duplication...");
        int removedBeforeRestore = removeEntities(world, center, maxRange);
        addon.log("Removed " + removedBeforeRestore + " existing entities before restoring saved entities");

        int restored = 0;
        for (SerializedEntity serialized : entities) {
            try {
                Entity entity = deserializeEntity(serialized, world, center);
                if (entity != null) {
                    restored++;
                }
            } catch (Exception e) {
                addon.logWarning("Failed to restore entity " + serialized.entityType + ": " + e.getMessage());
            }
        }

        addon.log("Restored " + restored + " of " + entities.size() + " entities from " + entityFile.getName());
        return restored;
    }

    /**
     * Remove all non-player entities in a region.
     * Must be called from the main thread.
     *
     * Handles special cases like item frames which would otherwise drop items.
     *
     * @param world The world
     * @param center The center location
     * @param range The range from center
     * @return Number of entities removed
     */
    public int removeEntities(World world, Location center, int range) {
        if (world == null || center == null) {
            return 0;
        }

        if (!Bukkit.isPrimaryThread()) {
            addon.logError("EntityStorage.removeEntities must be called from main thread!");
            return 0;
        }

        int minX = center.getBlockX() - range;
        int maxX = center.getBlockX() + range;
        int minZ = center.getBlockZ() - range;
        int maxZ = center.getBlockZ() + range;

        // Collect entities to remove (to avoid ConcurrentModificationException)
        List<Entity> toRemove = new ArrayList<>();

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }

            Location loc = entity.getLocation();
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                toRemove.add(entity);
            }
        }

        int removed = 0;
        for (Entity entity : toRemove) {
            try {
                // Special handling for item frames - clear their contents first
                // This prevents them from dropping items when removed
                if (entity instanceof ItemFrame) {
                    ItemFrame frame = (ItemFrame) entity;
                    frame.setItem(null, false); // false = don't drop the item
                }

                // For all hanging entities (paintings, item frames, glow item frames)
                // we need to make sure they don't drop anything
                if (entity instanceof Hanging) {
                    // Hanging entities might drop themselves - use setDropChance approach
                    // But the safest way is to just remove() after clearing contents
                }

                entity.remove();
                removed++;
            } catch (Exception e) {
                addon.logWarning("Failed to remove entity " + entity.getType() + ": " + e.getMessage());
            }
        }

        addon.log("Removed " + removed + " entities in region");
        return removed;
    }

    /**
     * Get the entity file path from a schematic file path
     */
    private File getEntityFile(File schematicFile) {
        String path = schematicFile.getAbsolutePath();
        if (path.endsWith(".schem")) {
            path = path.substring(0, path.length() - 6) + ".entities";
        } else {
            path = path + ".entities";
        }
        return new File(path);
    }

    /**
     * Serialize an entity to a storable format
     */
    private SerializedEntity serializeEntity(Entity entity, Location regionCenter) {
        SerializedEntity serialized = new SerializedEntity();
        serialized.entityType = entity.getType().name();

        // Store relative position to region center
        Location entityLoc = entity.getLocation();
        serialized.relX = entityLoc.getX() - regionCenter.getX();
        serialized.relY = entityLoc.getY() - regionCenter.getY();
        serialized.relZ = entityLoc.getZ() - regionCenter.getZ();
        serialized.yaw = entityLoc.getYaw();
        serialized.pitch = entityLoc.getPitch();

        // Serialize entity data using Bukkit
        try {
            // Serialize the entity's custom name if it has one
            if (entity.getCustomName() != null) {
                serialized.customName = entity.getCustomName();
            }
            serialized.customNameVisible = entity.isCustomNameVisible();

            // Store velocity
            serialized.velX = entity.getVelocity().getX();
            serialized.velY = entity.getVelocity().getY();
            serialized.velZ = entity.getVelocity().getZ();

            // Store LivingEntity data (health, etc)
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                serialized.health = living.getHealth();
                if (maxHealthAttribute != null) {
                    AttributeInstance maxHealthAttr = living.getAttribute(maxHealthAttribute);
                    if (maxHealthAttr != null) {
                        serialized.maxHealth = maxHealthAttr.getBaseValue();
                    }
                }
                serialized.remainingAir = living.getRemainingAir();
                serialized.noDamageTicks = living.getNoDamageTicks();

                // Store if entity is a baby
                if (entity instanceof Ageable) {
                    serialized.isAdult = ((Ageable) entity).isAdult();
                    serialized.age = ((Ageable) entity).getAge();
                    serialized.ageLock = ((Ageable) entity).getAgeLock();
                }

                // Store tamed status
                if (entity instanceof Tameable) {
                    Tameable tameable = (Tameable) entity;
                    serialized.isTamed = tameable.isTamed();
                    if (tameable.getOwner() != null) {
                        serialized.ownerUUID = tameable.getOwner().getUniqueId().toString();
                    }
                }
            }

            // Special handling for Villagers - save profession, trades, level, experience
            if (entity instanceof Villager) {
                Villager villager = (Villager) entity;
                serialized.villagerProfession = getProfessionKey(villager.getProfession());
                serialized.villagerType = getVillagerTypeKey(villager.getVillagerType());
                serialized.villagerLevel = villager.getVillagerLevel();
                serialized.villagerExperience = villager.getVillagerExperience();

                // Serialize trades
                List<MerchantRecipe> recipes = villager.getRecipes();
                if (recipes != null && !recipes.isEmpty()) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
                            boos.writeInt(recipes.size());
                            for (MerchantRecipe recipe : recipes) {
                                // Serialize each recipe component
                                boos.writeObject(recipe.getResult());
                                boos.writeInt(recipe.getIngredients().size());
                                for (ItemStack ingredient : recipe.getIngredients()) {
                                    boos.writeObject(ingredient);
                                }
                                boos.writeInt(recipe.getUses());
                                boos.writeInt(recipe.getMaxUses());
                                boos.writeBoolean(recipe.hasExperienceReward());
                                boos.writeInt(recipe.getVillagerExperience());
                                boos.writeFloat(recipe.getPriceMultiplier());
                            }
                        }
                        serialized.villagerTrades = baos.toByteArray();
                    } catch (Exception e) {
                        addon.logWarning("Failed to serialize villager trades: " + e.getMessage());
                    }
                }
            }

            // Special handling for item frames - save the item they contain
            if (entity instanceof ItemFrame) {
                ItemFrame frame = (ItemFrame) entity;
                ItemStack item = frame.getItem();
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    // Serialize the item to bytes
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
                            boos.writeObject(item);
                        }
                        serialized.itemFrameData = baos.toByteArray();
                        serialized.itemFrameRotation = frame.getRotation().ordinal();
                        serialized.itemFrameFixed = frame.isFixed();
                        serialized.itemFrameVisible = frame.isVisible();
                    } catch (Exception e) {
                        addon.logWarning("Failed to serialize item frame contents: " + e.getMessage());
                    }
                }

                // Store the facing direction for hanging entities
                if (entity instanceof Hanging) {
                    serialized.hangingFace = ((Hanging) entity).getFacing().name();
                }
            }

            // Store facing direction for all hanging entities (paintings, etc)
            if (entity instanceof Hanging && serialized.hangingFace == null) {
                serialized.hangingFace = ((Hanging) entity).getFacing().name();
            }

        } catch (Exception e) {
            // Non-critical - entity will be spawned with defaults
            addon.logWarning("Error serializing entity " + entity.getType() + ": " + e.getMessage());
        }

        return serialized;
    }

    /**
     * Deserialize and spawn an entity
     */
    private Entity deserializeEntity(SerializedEntity serialized, World world, Location regionCenter) {
        EntityType type;
        try {
            type = EntityType.valueOf(serialized.entityType);
        } catch (IllegalArgumentException e) {
            addon.logWarning("Unknown entity type: " + serialized.entityType);
            return null;
        }

        // Skip certain entity types that shouldn't be restored
        if (type == EntityType.PLAYER || type == EntityType.UNKNOWN) {
            return null;
        }

        // Skip dropped items and experience orbs - they shouldn't persist across slot switches
        // Note: EntityType name varies by MC version - check by name string for compatibility
        String typeName = type.name();
        if (typeName.equals("ITEM") || typeName.equals("DROPPED_ITEM") || typeName.equals("EXPERIENCE_ORB")) {
            return null;
        }

        // Calculate absolute position
        Location spawnLoc = new Location(
            world,
            regionCenter.getX() + serialized.relX,
            regionCenter.getY() + serialized.relY,
            regionCenter.getZ() + serialized.relZ,
            serialized.yaw,
            serialized.pitch
        );

        Entity entity;
        try {
            // Spawn the entity
            entity = world.spawnEntity(spawnLoc, type);
        } catch (Exception e) {
            addon.logWarning("Failed to spawn entity " + type + " at " + spawnLoc + ": " + e.getMessage());
            return null;
        }

        // Restore custom name
        if (serialized.customName != null) {
            entity.setCustomName(serialized.customName);
            entity.setCustomNameVisible(serialized.customNameVisible);
        }

        // Restore LivingEntity data
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            // Restore max health first (so we can set health properly)
            if (serialized.maxHealth > 0 && maxHealthAttribute != null) {
                AttributeInstance maxHealthAttr = living.getAttribute(maxHealthAttribute);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(serialized.maxHealth);
                }
            }

            // Restore health
            if (serialized.health > 0) {
                double maxHealth = 20.0; // Default
                if (maxHealthAttribute != null) {
                    AttributeInstance maxHealthAttr = living.getAttribute(maxHealthAttribute);
                    if (maxHealthAttr != null) {
                        maxHealth = maxHealthAttr.getValue();
                    }
                }
                living.setHealth(Math.min(serialized.health, maxHealth));
            }

            if (serialized.remainingAir != 0) {
                living.setRemainingAir(serialized.remainingAir);
            }
            if (serialized.noDamageTicks > 0) {
                living.setNoDamageTicks(serialized.noDamageTicks);
            }

            // Restore age for Ageable entities
            if (entity instanceof Ageable) {
                Ageable ageable = (Ageable) entity;
                if (serialized.isAdult) {
                    ageable.setAdult();
                } else {
                    ageable.setBaby();
                }
                ageable.setAge(serialized.age);
                ageable.setAgeLock(serialized.ageLock);
            }

            // Restore tamed status
            if (entity instanceof Tameable && serialized.isTamed) {
                Tameable tameable = (Tameable) entity;
                tameable.setTamed(true);
                if (serialized.ownerUUID != null) {
                    try {
                        UUID ownerUUID = UUID.fromString(serialized.ownerUUID);
                        tameable.setOwner(Bukkit.getOfflinePlayer(ownerUUID));
                    } catch (Exception e) {
                        // Invalid UUID - ignore
                    }
                }
            }
        }

        // Restore Villager data
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;

            // Restore profession
            if (serialized.villagerProfession != null) {
                Villager.Profession profession = getProfessionByKey(serialized.villagerProfession);
                if (profession != null) {
                    villager.setProfession(profession);
                } else {
                    addon.logWarning("Unknown villager profession: " + serialized.villagerProfession);
                }
            }

            // Restore villager type (biome variant)
            if (serialized.villagerType != null) {
                Villager.Type vType = getVillagerTypeByKey(serialized.villagerType);
                if (vType != null) {
                    villager.setVillagerType(vType);
                } else {
                    addon.logWarning("Unknown villager type: " + serialized.villagerType);
                }
            }

            // Restore level and experience
            if (serialized.villagerLevel > 0) {
                villager.setVillagerLevel(serialized.villagerLevel);
            }
            if (serialized.villagerExperience > 0) {
                villager.setVillagerExperience(serialized.villagerExperience);
            }

            // Restore trades
            if (serialized.villagerTrades != null) {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(serialized.villagerTrades);
                    try (BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
                        int recipeCount = bois.readInt();
                        List<MerchantRecipe> recipes = new ArrayList<>();

                        for (int i = 0; i < recipeCount; i++) {
                            ItemStack result = (ItemStack) bois.readObject();
                            int ingredientCount = bois.readInt();
                            List<ItemStack> ingredients = new ArrayList<>();
                            for (int j = 0; j < ingredientCount; j++) {
                                ingredients.add((ItemStack) bois.readObject());
                            }
                            int uses = bois.readInt();
                            int maxUses = bois.readInt();
                            boolean expReward = bois.readBoolean();
                            int villagerExp = bois.readInt();
                            float priceMultiplier = bois.readFloat();

                            MerchantRecipe recipe = new MerchantRecipe(result, uses, maxUses, expReward, villagerExp, priceMultiplier);
                            recipe.setIngredients(ingredients);
                            recipes.add(recipe);
                        }

                        villager.setRecipes(recipes);
                    }
                } catch (Exception e) {
                    addon.logWarning("Failed to restore villager trades: " + e.getMessage());
                }
            }
        }

        // Restore item frame contents
        if (entity instanceof ItemFrame && serialized.itemFrameData != null) {
            try {
                ItemFrame frame = (ItemFrame) entity;
                ByteArrayInputStream bais = new ByteArrayInputStream(serialized.itemFrameData);
                try (BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
                    ItemStack item = (ItemStack) bois.readObject();
                    frame.setItem(item, false); // false = don't play sound
                }

                // Restore rotation
                if (serialized.itemFrameRotation >= 0 && serialized.itemFrameRotation < org.bukkit.Rotation.values().length) {
                    frame.setRotation(org.bukkit.Rotation.values()[serialized.itemFrameRotation]);
                }

                frame.setFixed(serialized.itemFrameFixed);
                frame.setVisible(serialized.itemFrameVisible);
            } catch (Exception e) {
                addon.logWarning("Failed to restore item frame contents: " + e.getMessage());
            }
        }

        // Restore facing direction for hanging entities (if not already set by spawn location)
        if (entity instanceof Hanging && serialized.hangingFace != null) {
            try {
                org.bukkit.block.BlockFace face = org.bukkit.block.BlockFace.valueOf(serialized.hangingFace);
                ((Hanging) entity).setFacingDirection(face, true);
            } catch (Exception e) {
                // Face may be invalid for this location - ignore
            }
        }

        return entity;
    }

    /**
     * Serializable container for entity data.
     *
     * IMPORTANT: Keep serialVersionUID = 1 for backwards compatibility with
     * existing .entities files. New fields must be Optional or have sensible
     * defaults when null (which is what happens when deserializing old files).
     */
    private static class SerializedEntity implements Serializable {
        // Keep at 1 for backwards compatibility - new fields will just be null/default when loading old files
        private static final long serialVersionUID = 1L;

        String entityType;
        double relX, relY, relZ;
        float yaw, pitch;
        double velX, velY, velZ;
        String customName;
        boolean customNameVisible;

        // LivingEntity data
        double health;
        double maxHealth;
        int remainingAir;
        int noDamageTicks;

        // Ageable entity data
        boolean isAdult = true;  // Default to adult
        int age;
        boolean ageLock;

        // Tameable entity data
        boolean isTamed;
        String ownerUUID;

        // Villager specific data
        String villagerProfession;
        String villagerType;
        int villagerLevel;
        int villagerExperience;
        byte[] villagerTrades;    // Serialized trade recipes

        // Item frame specific data (will be null when loading old entity files - that's OK)
        byte[] itemFrameData;      // Serialized ItemStack
        int itemFrameRotation;     // Rotation ordinal
        boolean itemFrameFixed;    // Is the item frame fixed?
        boolean itemFrameVisible;  // Is the item frame visible?

        // Hanging entity facing direction (will be null when loading old entity files - that's OK)
        String hangingFace;        // BlockFace name
    }
}
