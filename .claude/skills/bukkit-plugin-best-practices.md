# Bukkit/BentoBox Plugin Development Best Practices

This skill provides guidelines for avoiding common bugs in Bukkit and BentoBox plugin development.

## When to Use This Skill

Use this skill when:
- Writing new Bukkit/Spigot/Paper plugins
- Developing BentoBox addons
- Reviewing existing plugin code for bugs
- Fixing bugs in Minecraft server plugins

---

## 1. Thread Safety

### Avoid ConcurrentModificationException in Entity Loops

**BAD:**
```java
for (Entity entity : world.getEntities()) {
    if (shouldRemove(entity)) {
        entity.remove(); // ConcurrentModificationException!
    }
}
```

**GOOD:**
```java
List<Entity> entitiesToRemove = new ArrayList<>();
for (Entity entity : world.getEntities()) {
    if (shouldRemove(entity)) {
        entitiesToRemove.add(entity);
    }
}
for (Entity entity : entitiesToRemove) {
    entity.remove();
}
```

### Prevent Concurrent Operations with Flags

**BAD:**
```java
public void switchSlot(Player player, SlotData from, SlotData to) {
    // Player can click multiple times, causing data corruption
    performSwitch(player, from, to);
}
```

**GOOD:**
```java
private final Set<UUID> switchingPlayers = ConcurrentHashMap.newKeySet();

public void switchSlot(Player player, SlotData from, SlotData to) {
    UUID playerUUID = player.getUniqueId();

    if (switchingPlayers.contains(playerUUID)) {
        player.sendMessage("§cOperation already in progress!");
        return;
    }
    switchingPlayers.add(playerUUID);

    try {
        performSwitch(player, from, to);
    } finally {
        switchingPlayers.remove(playerUUID);
    }
}
```

### Use ConcurrentHashMap for Async Access

**BAD:**
```java
private final Map<UUID, Session> sessions = new HashMap<>(); // Not thread-safe!
```

**GOOD:**
```java
private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
```

---

## 2. Null Safety

### Always Check island.getCenter()

**BAD:**
```java
Island island = getIslands().getIsland(world, playerUUID);
int x = island.getCenter().getBlockX(); // NPE if center is null!
```

**GOOD:**
```java
Island island = getIslands().getIsland(world, playerUUID);
if (island == null) {
    return;
}
Location center = island.getCenter();
if (center == null) {
    return;
}
int x = center.getBlockX();
```

### Validate Offline Players Properly

**BAD:**
```java
UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
if (playerUUID == null) { // NEVER null - creates fake UUID for unknown players!
    return;
}
```

**GOOD:**
```java
OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
    user.sendMessage("Player not found!");
    return;
}
UUID playerUUID = offlinePlayer.getUniqueId();
```

### Check user.getPlayer() Even with setOnlyPlayer(true)

**BAD:**
```java
ItemStack item = user.getPlayer().getInventory().getItemInMainHand();
```

**GOOD:**
```java
Player player = user.getPlayer();
if (player == null) {
    return false;
}
ItemStack item = player.getInventory().getItemInMainHand();
```

### Check Array/List Results Before Accessing

**BAD:**
```java
File[] backups = listBackups(playerUUID, slotNumber);
File latest = backups[0]; // ArrayIndexOutOfBoundsException if empty!
```

**GOOD:**
```java
File[] backups = listBackups(playerUUID, slotNumber);
if (backups == null || backups.length == 0) {
    return null;
}
File latest = backups[0];
```

---

## 3. World Access Safety

### Never Assume Bukkit.getWorlds() Has Elements

**BAD:**
```java
Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation(); // IndexOutOfBoundsException!
```

**GOOD:**
```java
World spawnWorld = Bukkit.getWorld("world");
if (spawnWorld == null && !Bukkit.getWorlds().isEmpty()) {
    spawnWorld = Bukkit.getWorlds().get(0);
}
if (spawnWorld == null) {
    player.sendMessage("§cNo spawn world available!");
    return;
}
Location spawn = spawnWorld.getSpawnLocation();
```

---

## 4. Input Validation

### Validate Settings Boundaries

**BAD:**
```java
public void setMaxSlots(int maxSlots) {
    this.maxSlots = maxSlots; // Can be 0 or negative!
}
```

**GOOD:**
```java
public void setMaxSlots(int maxSlots) {
    this.maxSlots = Math.max(1, maxSlots);
}
```

### Validate Grid/Range Bounds

**BAD:**
```java
public void setGridMinX(int gridMinX) {
    this.gridMinX = gridMinX; // Can be > gridMaxX!
}
```

**GOOD:**
```java
public void setGridMinX(int gridMinX) {
    this.gridMinX = gridMinX;
    if (this.gridMaxX < gridMinX) {
        this.gridMaxX = gridMinX;
    }
}
```

### Prevent Division by Zero

**BAD:**
```java
int gridX = Math.floorDiv(worldX, spacing); // ArithmeticException if spacing is 0!
```

**GOOD:**
```java
if (spacing <= 0) {
    logWarning("Invalid spacing: " + spacing);
    return new GridCoordinate(0, 0);
}
int gridX = Math.floorDiv(worldX, spacing);
```

### Validate String Operations

**BAD:**
```java
String firstChar = part.substring(0, 1); // IndexOutOfBoundsException if empty!
```

**GOOD:**
```java
if (part.isEmpty()) {
    continue;
}
String firstChar = part.substring(0, 1);
```

---

## 5. Event Listener Memory Management

### Unregister Listeners on Close

**BAD:**
```java
public class MyGUI implements Listener {
    public MyGUI(Player player) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Never unregistered - memory leak!
    }
}
```

**GOOD:**
```java
public class MyGUI implements Listener {
    public MyGUI(Player player) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(this.inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}
```

### Clean Up Player Data on Quit

**BAD:**
```java
private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
// Sessions accumulate forever for disconnected players!
```

**GOOD:**
```java
private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    sessions.remove(playerId);
    pendingOperations.remove(playerId);
    // Clean up all player-related data
}
```

---

## 6. Async Callback Safety

### Store UUID, Not Player Reference

**BAD:**
```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    doAsyncWork();
    player.sendMessage("Done!"); // Player reference may be stale!
});
```

**GOOD:**
```java
UUID playerUUID = player.getUniqueId();
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    doAsyncWork();
    Bukkit.getScheduler().runTask(plugin, () -> {
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage("Done!");
        }
    });
});
```

### Always Clear Flags on All Exit Paths

**BAD:**
```java
operationInProgress.add(playerUUID);
try {
    if (checkFailed()) {
        return; // Flag never cleared!
    }
    doOperation();
} finally {
    // Missing cleanup in early returns!
}
```

**GOOD:**
```java
operationInProgress.add(playerUUID);
try {
    if (checkFailed()) {
        operationInProgress.remove(playerUUID);
        return;
    }
    doOperation();
    operationInProgress.remove(playerUUID);
} catch (Exception e) {
    operationInProgress.remove(playerUUID);
    throw e;
}
```

---

## 7. Tab Completion

### Filter by Typed Prefix

**BAD:**
```java
@Override
public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
    List<String> names = new ArrayList<>();
    Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
    return Optional.of(names); // Returns ALL players, ignoring what user typed
}
```

**GOOD:**
```java
@Override
public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
    List<String> names = new ArrayList<>();
    String prefix = args.isEmpty() ? "" : args.get(0).toLowerCase();

    for (Player p : Bukkit.getOnlinePlayers()) {
        if (p.getName().toLowerCase().startsWith(prefix)) {
            names.add(p.getName());
        }
    }
    return Optional.of(names);
}
```

---

## 8. Resource Management

### Use Try-With-Resources for Streams

**BAD:**
```java
FileOutputStream fos = new FileOutputStream(file);
ClipboardWriter writer = format.getWriter(fos);
writer.write(clipboard);
// Streams may not be closed on exception!
```

**GOOD:**
```java
try (FileOutputStream fos = new FileOutputStream(file);
     ClipboardWriter writer = format.getWriter(fos)) {
    writer.write(clipboard);
}
```

### Check File Operation Results

**BAD:**
```java
file.delete(); // Silently fails if deletion unsuccessful
```

**GOOD:**
```java
if (!file.delete()) {
    plugin.getLogger().warning("Failed to delete file: " + file.getPath());
}
```

---

## 9. CompletableFuture Timeouts

### Always Add Timeouts to Async Operations

**BAD:**
```java
CompletableFuture<Boolean> future = new CompletableFuture<>();
// Can hang forever if never completed!
future.get();
```

**GOOD:**
```java
CompletableFuture<Boolean> future = new CompletableFuture<>();
try {
    boolean result = future.get(30, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    plugin.logError("Operation timed out");
    return;
}
```

### Handle exceptionally() for Async Chains

**BAD:**
```java
CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
    .thenRun(() -> {
        processChunks();
    });
// Exceptions silently swallowed!
```

**GOOD:**
```java
CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
    .orTimeout(30, TimeUnit.SECONDS)
    .thenRun(() -> {
        processChunks();
    })
    .exceptionally(throwable -> {
        plugin.logWarning("Chunk loading failed: " + throwable.getMessage());
        return null;
    });
```

---

## Quick Reference Checklist

Before submitting code, verify:

- [ ] No entity removal during iteration
- [ ] Concurrent operation flags for long-running player actions
- [ ] ConcurrentHashMap for async-accessed collections
- [ ] Null checks for island.getCenter(), user.getPlayer()
- [ ] OfflinePlayer.hasPlayedBefore() check before using UUID
- [ ] World existence check before Bukkit.getWorlds().get(0)
- [ ] Input validation for settings (min/max bounds, positive values)
- [ ] Empty string/array checks before substring/index access
- [ ] Event listeners unregistered on inventory close
- [ ] Player data cleaned up on PlayerQuitEvent
- [ ] UUID stored instead of Player reference in async callbacks
- [ ] Flags cleared on ALL exit paths (success, failure, exception)
- [ ] Tab completion filters by typed prefix
- [ ] Try-with-resources for file streams
- [ ] Timeouts on CompletableFuture operations
