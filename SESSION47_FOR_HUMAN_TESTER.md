# Session 47: Custom Event API

## Quick Summary
**Features:** Custom Bukkit events for developers to hook into IslandSelector
**Time Estimate:** 20-30 minutes
**Players Required:** 2-3 accounts (for comprehensive testing)
**Complexity:** Advanced (requires writing a test listener plugin)
**Dependencies:** None (optional: IDE for creating test plugin)

## What Was Implemented

### Custom Event Classes
Three new custom Bukkit events that fire during major operations:

1. **GridLocationClaimEvent** - Fires when a player claims a grid location
2. **SlotSwitchEvent** - Fires when a player switches between island slots
3. **IslandRelocateEvent** - Fires when a player relocates their island

All events are **cancellable** and fire BEFORE the operation begins, allowing other plugins to:
- Cancel the operation
- Log the operation
- Charge additional costs
- Apply custom restrictions
- Trigger other actions

## Test Cases

### Test #1: GridLocationClaimEvent Fires on Claim
**Description:** Event fires with correct data when player claims a grid location

**Prerequisites:**
- Create a test plugin that listens for GridLocationClaimEvent
- Have a new player account that has never created an island

**Test Plugin Example:**
```java
@EventHandler
public void onGridClaim(GridLocationClaimEvent event) {
    Player player = event.getPlayer();
    String coord = event.getGridCoordinateString();
    int worldX = event.getWorldX();
    int worldZ = event.getWorldZ();

    // Log to console
    Bukkit.getLogger().info("CLAIM EVENT: " + player.getName() +
                           " claiming " + coord +
                           " at world coordinates " + worldX + "," + worldZ);
}
```

**Steps:**
1. Install your test plugin on the server
2. Join with a new player (no island)
3. Run `/island` command
4. Click on an empty location in the grid GUI (e.g., "B5")
5. Confirm the claim in the confirmation GUI
6. Check server console for your event log

**Expected Result:**
- Console shows: "CLAIM EVENT: PlayerName claiming B5 at world coordinates X,Z"
- Event data (coordinates, player) is accurate
- Island is created successfully after event fires

**Pass Criteria:** ✅ Event fires with correct data before island creation

---

### Test #2: GridLocationClaimEvent Can Be Cancelled
**Description:** Cancelling the event prevents island creation

**Test Plugin Example:**
```java
@EventHandler
public void onGridClaim(GridLocationClaimEvent event) {
    String coord = event.getGridCoordinateString();

    // Cancel claims to location A1
    if (coord.equals("A1")) {
        event.setCancellationReason("Location A1 is reserved for VIP players!");
        // event.setCancelled(true); is called automatically by setCancellationReason
    }
}
```

**Steps:**
1. Update test plugin to cancel claims to "A1"
2. Reload/restart server with updated plugin
3. Join with new player
4. Try to claim location "A1"
5. Confirm the claim
6. Verify island is NOT created
7. Verify player receives cancellation message
8. Try to claim a different location (e.g., "B2")
9. Verify that location works normally

**Expected Result:**
- Claiming A1 shows error: "Location A1 is reserved for VIP players!"
- Island is NOT created at A1
- Player returns to grid GUI (or stays in-game, no crash)
- Other locations work normally

**Pass Criteria:** ✅ Cancelled event prevents island creation with custom message

---

### Test #3: SlotSwitchEvent Fires on Slot Switch
**Description:** Event fires with correct data when player switches slots

**Prerequisites:**
- Player with at least 2 island slots
- Islands in both slot 1 and slot 2
- Player has renamed slot 1 to "Main Base" and slot 2 to "Farm Island"

**Test Plugin Example:**
```java
@EventHandler
public void onSlotSwitch(SlotSwitchEvent event) {
    Player player = event.getPlayer();
    int from = event.getFromSlot();
    int to = event.getToSlot();
    String fromName = event.getFromSlotName();
    String toName = event.getToSlotName();

    Bukkit.getLogger().info("SLOT SWITCH: " + player.getName() +
                           " switching from slot " + from + " (" + fromName + ")" +
                           " to slot " + to + " (" + toName + ")");
}
```

**Steps:**
1. Ensure player is on slot 1 ("Main Base")
2. Open slot GUI: `/islandselector slots`
3. Click slot 2 ("Farm Island")
4. Confirm the switch
5. Check server console for event log

**Expected Result:**
- Console shows: "SLOT SWITCH: PlayerName switching from slot 1 (Main Base) to slot 2 (Farm Island)"
- Event data is accurate
- Slot switch completes successfully after event fires

**Pass Criteria:** ✅ Event fires with correct slot numbers and names

---

### Test #4: SlotSwitchEvent Can Be Cancelled
**Description:** Cancelling the event prevents slot switching

**Test Plugin Example:**
```java
@EventHandler
public void onSlotSwitch(SlotSwitchEvent event) {
    // Prevent switching to slot 5 (pretend it's locked)
    if (event.getToSlot() == 5) {
        event.setCancellationReason("Slot 5 is locked! Complete the quest first.");
    }
}
```

**Steps:**
1. Update test plugin to cancel switches to slot 5
2. Player tries to switch to slot 5
3. Confirm the switch
4. Verify switch does NOT happen
5. Verify player receives cancellation message
6. Verify player stays on current slot
7. Try switching to a different slot (e.g., slot 2)
8. Verify that switch works normally

**Expected Result:**
- Switching to slot 5 shows error: "Slot 5 is locked! Complete the quest first."
- Player remains on current slot
- Current island is NOT saved/changed
- Other slot switches work normally

**Pass Criteria:** ✅ Cancelled event prevents slot switching with custom message

---

### Test #5: IslandRelocateEvent Fires on Relocation
**Description:** Event fires with correct data when player relocates island

**Prerequisites:**
- Player with an island at a known location (e.g., "C5")
- Player wants to relocate to "F10"

**Test Plugin Example:**
```java
@EventHandler
public void onRelocate(IslandRelocateEvent event) {
    Player player = event.getPlayer();
    String from = event.getFromCoordinateString();
    String to = event.getToCoordinateString();
    double cost = event.getCost();

    Bukkit.getLogger().info("RELOCATE EVENT: " + player.getName() +
                           " relocating from " + from + " to " + to +
                           " (cost: $" + cost + ")");
}
```

**Steps:**
1. Note player's current island location (e.g., "C5")
2. Open grid GUI: `/islandselector`
3. Click on an empty location (e.g., "F10")
4. Confirm relocation in confirmation GUI
5. Check server console for event log

**Expected Result:**
- Console shows: "RELOCATE EVENT: PlayerName relocating from C5 to F10 (cost: $X)"
- Event data is accurate (from/to coordinates, cost)
- Relocation completes successfully after event fires
- If Vault is installed, cost is correct
- If no Vault, cost is 0

**Pass Criteria:** ✅ Event fires with correct coordinates and cost data

---

### Test #6: IslandRelocateEvent Can Be Cancelled
**Description:** Cancelling the event prevents island relocation

**Test Plugin Example:**
```java
@EventHandler
public void onRelocate(IslandRelocateEvent event) {
    String to = event.getToCoordinateString();

    // Block relocations to the "danger zone" (rows 15+)
    GridCoordinate coord = event.getToCoordinate();
    int row = coord.getZ();

    if (row >= 15) {
        event.setCancellationReason("Cannot relocate beyond row 15 - danger zone!");
    }
}
```

**Steps:**
1. Update test plugin to cancel relocations to row 15+
2. Try to relocate to a location in row 15 or higher (e.g., "A15")
3. Confirm the relocation
4. Verify relocation does NOT happen
5. Verify player receives cancellation message
6. Verify island stays at original location
7. Try relocating to a different location in rows 1-14
8. Verify that relocation works normally

**Expected Result:**
- Relocating to row 15+ shows error: "Cannot relocate beyond row 15 - danger zone!"
- Island is NOT moved
- Old location remains occupied by player
- New location remains available
- Other relocations work normally

**Pass Criteria:** ✅ Cancelled event prevents relocation with custom message

---

### Test #7: Events Fire in Correct Order
**Description:** Events fire BEFORE operations begin, allowing cancellation

**Test Plugin Example:**
```java
@EventHandler(priority = EventPriority.MONITOR)
public void onSlotSwitch(SlotSwitchEvent event) {
    if (event.isCancelled()) {
        Bukkit.getLogger().info("Switch was cancelled by another plugin");
    } else {
        Bukkit.getLogger().info("Switch is proceeding - logging for audit");
    }
}
```

**Steps:**
1. Set up two test plugins:
   - Plugin A: Cancels specific operations (high priority)
   - Plugin B: Logs operations (MONITOR priority)
2. Trigger a claim/switch/relocation that Plugin A cancels
3. Check console logs
4. Verify Plugin B sees the event as cancelled
5. Verify operation does not proceed
6. Trigger a normal operation (not cancelled)
7. Verify Plugin B logs it as successful
8. Verify operation proceeds

**Expected Result:**
- Events respect Bukkit's event priority system
- MONITOR listeners can see if event was cancelled by earlier listeners
- Cancelled events do NOT trigger the operation
- Operations only proceed if event is not cancelled

**Pass Criteria:** ✅ Event system works correctly with Bukkit event priorities

---

### Test #8: Event Data is Accurate and Useful
**Description:** All event data fields contain correct, useful information

**Test Plugin Example:**
```java
@EventHandler
public void onAllEvents(GridLocationClaimEvent event) {
    // Test coordinate string format
    assert event.getGridCoordinateString().matches("[A-Z]+[0-9]+");

    // Test world coordinates are reasonable
    assert Math.abs(event.getWorldX()) < 1000000;
    assert Math.abs(event.getWorldZ()) < 1000000;

    // Test player is online
    assert event.getPlayer().isOnline();
}

@EventHandler
public void onRelocate(IslandRelocateEvent event) {
    // Test from != to
    assert !event.getFromCoordinateString().equals(event.getToCoordinateString());

    // Test cost is non-negative
    assert event.getCost() >= 0;

    // Test isFree() matches cost
    assert event.isFree() == (event.getCost() == 0);
}
```

**Steps:**
1. Create a test plugin with assertions/checks for all event data
2. Trigger each type of event (claim, switch, relocate)
3. Verify no assertion errors in console
4. Verify all data fields are populated correctly:
   - Player objects are valid and online
   - Coordinates are in valid format
   - World coordinates match grid coordinates
   - Slot numbers are 1-5
   - Slot names are not null/empty
   - Costs are accurate

**Expected Result:**
- All event data is accurate
- No null pointer exceptions
- No assertion failures
- Data matches what actually happens in the operation

**Pass Criteria:** ✅ All event data fields are accurate and useful

---

## Quick Test Flow (15 minutes)

**Prerequisites:**
- Basic test plugin that logs all 3 events to console
- 2 player accounts
- Player 1 has 2 slots with islands

**Quick Test:**

1. **Claim Event** (3 min)
   - Player 2 claims a location
   - Check console for GridLocationClaimEvent log
   - Verify island is created

2. **Slot Switch Event** (3 min)
   - Player 1 switches from slot 1 to slot 2
   - Check console for SlotSwitchEvent log
   - Verify switch completes

3. **Relocate Event** (3 min)
   - Player 1 relocates island
   - Check console for IslandRelocateEvent log
   - Verify relocation completes

4. **Cancellation Test** (6 min)
   - Update plugin to cancel one of each event type
   - Reload plugin
   - Trigger each event
   - Verify operations are blocked
   - Verify custom messages shown

## Test Plugin Template

Here's a complete test plugin to get started:

```java
package com.example.istest;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import world.bentobox.islandselector.events.GridLocationClaimEvent;
import world.bentobox.islandselector.events.IslandRelocateEvent;
import world.bentobox.islandselector.events.SlotSwitchEvent;

public class IslandSelectorTest extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("IslandSelector Event Test Plugin Enabled!");
    }

    @EventHandler
    public void onClaim(GridLocationClaimEvent event) {
        getLogger().info("[CLAIM] " + event.getPlayer().getName() +
                        " claiming " + event.getGridCoordinateString() +
                        " at " + event.getWorldX() + "," + event.getWorldZ());

        // Example: Block claims to A1
        // if (event.getGridCoordinateString().equals("A1")) {
        //     event.setCancellationReason("A1 is reserved!");
        // }
    }

    @EventHandler
    public void onSwitch(SlotSwitchEvent event) {
        getLogger().info("[SWITCH] " + event.getPlayer().getName() +
                        " from slot " + event.getFromSlot() +
                        " to slot " + event.getToSlot());

        // Example: Block switching to slot 5
        // if (event.getToSlot() == 5) {
        //     event.setCancellationReason("Slot 5 is locked!");
        // }
    }

    @EventHandler
    public void onRelocate(IslandRelocateEvent event) {
        getLogger().info("[RELOCATE] " + event.getPlayer().getName() +
                        " from " + event.getFromCoordinateString() +
                        " to " + event.getToCoordinateString() +
                        " (cost: $" + event.getCost() + ")");

        // Example: Block relocations to dangerous areas
        // if (event.getToCoordinate().getZ() >= 15) {
        //     event.setCancellationReason("Danger zone!");
        // }
    }
}
```

## Configuration

No configuration changes needed - events are always fired.

## Known Limitations

**Event Timing:**
- Events fire synchronously on main thread for cancellability
- Heavy processing in event listeners may cause lag
- Use async tasks if you need to do expensive operations

**Event Cancellation:**
- Cancelling events prevents the operation entirely
- There's no way to modify event data (e.g., change target location)
- This is standard Bukkit behavior

## Troubleshooting

**Events not firing:**
- Verify IslandSelector 1.0.0-SNAPSHOT is installed (Session 47+)
- Check test plugin is loaded and enabled
- Verify listener is registered correctly
- Check for typos in event class names

**Cancellation not working:**
- Ensure you call `event.setCancelled(true)` or `event.setCancellationReason(...)`
- Check event handler priority (lower priorities run first)
- Verify no errors in console when event fires

**Data is null/incorrect:**
- Verify you're testing on the correct operation (claim vs switch vs relocate)
- Check that the operation actually completes (not already cancelled by another plugin)
- Enable debug logging to see event flow

## Success Criteria

- ✅ GridLocationClaimEvent fires on island claims
- ✅ SlotSwitchEvent fires on slot switches
- ✅ IslandRelocateEvent fires on island relocations
- ✅ All events can be cancelled with custom messages
- ✅ Event data (coordinates, slots, costs) is accurate
- ✅ Events fire BEFORE operations begin
- ✅ Cancelled events prevent operations
- ✅ Event system works with Bukkit priorities

## Test Report Template

```
Test #1 - GridLocationClaimEvent Fires: [PASS/FAIL]
Notes:

Test #2 - GridLocationClaimEvent Cancellable: [PASS/FAIL]
Notes:

Test #3 - SlotSwitchEvent Fires: [PASS/FAIL]
Notes:

Test #4 - SlotSwitchEvent Cancellable: [PASS/FAIL]
Notes:

Test #5 - IslandRelocateEvent Fires: [PASS/FAIL]
Notes:

Test #6 - IslandRelocateEvent Cancellable: [PASS/FAIL]
Notes:

Test #7 - Events Fire in Correct Order: [PASS/FAIL]
Notes:

Test #8 - Event Data is Accurate: [PASS/FAIL]
Notes:

Overall: [PASS/FAIL]
Issues Found:

```

## Additional Notes

**For Plugin Developers:**

These events allow you to:
- Create custom claim restrictions (e.g., require quest completion)
- Implement custom slot limits (e.g., unlock slots via purchase)
- Add relocation taxes or distance-based costs
- Log operations for statistics/analytics
- Integrate with your permission/rank systems
- Create mini-games or challenges around island management

**Event API Documentation:**

All events include:
- `getPlayer()` - The player performing the action
- `isCancelled()` / `setCancelled(boolean)` - Standard cancellable methods
- `getCancellationReason()` / `setCancellationReason(String)` - Custom error messages

Specific data per event type - see Javadocs in the event classes for details.
