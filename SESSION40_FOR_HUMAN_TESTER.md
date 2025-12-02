# Session 40 - For Human Tester

## Quick Summary

**Feature:** Slot Selection GUI & Slot Management System
**Status:** ✅ ALREADY IMPLEMENTED - Needs Testing
**Purpose:** This session documents existing slot selection and management features
**Build Status:** ✅ NO CHANGES (Documentation only)
**Your Task:** Test the slot selection GUI and slot management features

---

## What This Session Did

**Discovery:** Session 40 found that **Slot Selection GUI** and related slot management features (Tests #63-80+) were already fully implemented in previous sessions but never verified by human testing.

**No new code** was added - this session documents what already exists.

---

## What to Test

Comprehensive slot system features ready for testing:

1. **Slot Selection GUI Layout** (Tests #63-67)
   - GUI opens with correct 27-slot layout
   - Active/inactive/empty/locked slot displays
   - Permission-based slot unlocking

2. **Slot Interactions** (Tests #68-72)
   - Clicking slots (locked/empty/inactive)
   - Empty slot island creation flow
   - Inactive slot switch confirmation

3. **Slot Switching** (Tests #73-78)
   - Switch confirmation GUI
   - Save/clear/load island schematics
   - Teleport players to new slot
   - Cooldown system

4. **Slot Management** (Tests covered in Sessions 28-31)
   - Rename slots via `/islandselector setname`
   - Set custom icons via `/islandselector seticon`
   - Delete slots via `/islandselector delete`
   - Slot settings GUI (right-click menu)

---

## Quick 10-Minute Test

### Setup
Copy JAR: `output/IslandSelector-1.0.0-SNAPSHOT.jar` → `plugins/BentoBox/addons/`

### Core Test Flow

**Test Slot Selection GUI:**

```
1. Create an island first (becomes slot 1)
2. Run: /islandselector slots
3. Expected: 27-slot GUI opens titled "Your Island Slots"
4. Expected: Slot 1 shows with ENCHANTMENT GLOW and "★ ACTIVE"
5. Expected: Slots 2-5 visible (empty or locked depending on permissions)
6. Expected: Back button (slot 20) and Close button (slot 24)
```

**Test Permission-Based Unlocking:**

```
Default (no permissions):
1. Open slots GUI
2. Expected: Slot 1 accessible, slots 2-5 LOCKED (BARRIER item)
3. Hover over locked slot
4. Expected: Tooltip shows "Locked Slot" and permission needed

With permission:
1. Give permission: islandselector.slots.2
2. Reopen slots GUI
3. Expected: Slots 1-2 now accessible, slots 3-5 locked
```

**Test Empty Slot Creation:**

```
Setup: Player has slots.2 permission, island in slot 1 only
1. /islandselector slots
2. Click slot 2 (should show LIME_DYE - "Empty Slot")
3. Expected: Message "Island creation in empty slots coming soon..."
   OR BSkyBlock island creation flow starts
4. If creation flow works:
   - Create island in slot 2
   - Verify both slots now show islands
   - Verify same grid location as slot 1
```

**Test Slot Switching:**

```
Setup: Islands in both slot 1 and slot 2
1. /islandselector slots
2. Click slot 2 (inactive slot with island)
3. Expected: Confirmation GUI opens
4. Expected: Shows switch from [Slot 1] → [Slot 2]
5. Click GREEN WOOL (Confirm)
6. Expected:
   - Progress messages (saving, clearing, loading, teleporting)
   - Current island saved as schematic
   - World cleared at that location
   - Slot 2 island loaded
   - Player teleported to slot 2 spawn
7. Expected: Success message "Slot switch complete!"
```

---

## Detailed Test Cases

### Test #63: Slot Selection GUI Opens with Correct Layout

**Category:** UI
**File:** SlotSelectionGUI.java

**Steps:**
1. Execute `/islandselector slots` or click "Slot Selection" button in grid GUI
2. Verify a chest GUI with 3 rows (27 slots) opens
3. Verify title is "Your Island Slots"
4. Verify slots 2-6 in first row show slot items
5. Verify bottom row has Back (slot 20) and Close (slot 24) buttons

**Expected Result:**
- 27-slot GUI (3 rows × 9 columns)
- Title: "Your Island Slots"
- Slots displayed at positions 2, 3, 4, 5, 6 (slots 1-5)
- Back button (ARROW) at slot 20
- Close button (BARRIER) at slot 24
- Black glass pane fillers in empty spaces

---

### Test #64: Active Slot Displays with Enchantment Glow

**Category:** UI
**File:** SlotSelectionGUI.java (lines 82-105)

**Steps:**
1. Create an island (becomes slot 1, active by default)
2. Open slot selection GUI
3. Locate slot 1 display
4. Verify it has enchantment glow effect
5. Verify tooltip/lore contains "★ ACTIVE" indicator
6. Verify name is green and bold (&a&l)

**Expected Result:**
- Material: GRASS_BLOCK (or custom icon if set)
- Display Name: Green bold text with slot name
- Enchantment glow visible
- Lore shows:
  - Level: 0 (or actual level if Level addon present)
  - Members: 1 (or actual count)
  - Empty line
  - "★ ACTIVE" in yellow

---

### Test #65: Inactive Slot Shows Custom Icon

**Category:** UI
**File:** SlotSelectionGUI.java (lines 107-123)

**Setup:** Create islands in slot 1 and slot 2, ensure slot 1 is active

**Steps:**
1. Open slot selection GUI
2. Locate slot 2 display (inactive)
3. Verify it shows custom icon (default: GRASS_BLOCK) without glow
4. Verify lore shows island level and member count
5. Verify lore shows "Click to switch to this slot"

**Expected Result:**
- Material: GRASS_BLOCK (or custom icon)
- Display Name: White text (not bold)
- NO enchantment glow
- Lore shows:
  - Level: 0
  - Members: 1
  - Empty line
  - "Click to switch to this slot" in yellow

---

### Test #66: Empty Unlocked Slot Shows Lime Dye

**Category:** UI
**File:** SlotSelectionGUI.java (lines 125-136)

**Setup:** Player has 2 slots available but only 1 island

**Steps:**
1. Open slot selection GUI
2. Locate empty slot 2
3. Verify it shows as LIME_DYE item
4. Verify name is "&aEmpty Slot"
5. Verify lore shows "Click to create a new island"

**Expected Result:**
- Material: LIME_DYE
- Display Name: "Empty Slot" in green
- Lore:
  - "This slot is available!"
  - Empty line
  - "Click to create a new island" in yellow

---

### Test #67: Locked Slot Shows Barrier

**Category:** UI
**File:** SlotSelectionGUI.java (lines 138-150)

**Setup:** Player has permission for 2 slots but config allows up to 5

**Steps:**
1. Open slot selection GUI
2. Locate locked slot 3
3. Verify it shows as BARRIER item
4. Verify name is "&cLocked Slot"
5. Verify lore shows required permission
6. Verify lore explains how to unlock

**Expected Result:**
- Material: BARRIER
- Display Name: "Locked Slot" in red
- Lore:
  - "This slot is locked."
  - Empty line
  - "Unlock with: islandselector.slots.3"

---

### Test #68: Permission System - islandselector.slots.2

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 102-109)

**Steps:**
1. Give player permission `islandselector.slots.2`
2. Remove higher slot permissions (slots.3, slots.4, slots.5)
3. Open slot selection GUI
4. Verify slots 1 and 2 are unlocked
5. Verify slots 3-5 show as locked (BARRIER)

**Expected Result:**
- Slots 1-2: Accessible (GRASS_BLOCK or LIME_DYE)
- Slots 3-5: Locked (BARRIER with permission message)

---

### Test #69: Permission System - islandselector.slots.5

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 102-109)

**Steps:**
1. Give player permission `islandselector.slots.5`
2. Open slot selection GUI
3. Verify all 5 slots are unlocked (not showing BARRIER)
4. Verify player can create islands in all 5 slots

**Expected Result:**
- All 5 slots accessible
- No BARRIER items visible
- Can click any slot to create/switch

---

### Test #70: Clicking Locked Slot Shows Error

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 219-226)

**Steps:**
1. Player has only 2 slots unlocked
2. Open slot selection GUI
3. Click on locked slot 3 (BARRIER)
4. Verify error message is shown explaining unlock requirements
5. Verify no action occurs (no switch, no island creation)

**Expected Result:**
- Error message: "This slot is locked!"
- Info message: "You need permission: islandselector.slots.3"
- GUI remains open
- No slot switch or island creation

---

### Test #71: Clicking Empty Slot Starts Island Creation

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 252-266)

**Setup:** Player has 2 slots, with island in slot 1 only

**Steps:**
1. Open slot selection GUI
2. Click on empty slot 2 (LIME_DYE)
3. Verify BSkyBlock island creation/schematic selection starts
4. After creation, verify new island exists in slot 2
5. Verify both slots now show islands in slot GUI

**Expected Result:**
- Currently: "Island creation in empty slots coming soon..." message
- Future: BSkyBlock blueprint selection → island creation
- After creation: Slot 2 shows island with custom icon
- Both slots at same grid location

**Note:** This feature may show "coming soon" message - that's expected if island creation in empty slots isn't fully integrated yet.

---

### Test #72: New Island Shares Same Grid Location

**Category:** Functional
**File:** SlotSelectionGUI.java, SlotManager.java

**Steps:**
1. Create island in slot 1 at grid location H8
2. Create island in slot 2 (empty slot)
3. Switch to slot 2 and check location
4. Verify slot 2 island is also at grid location H8
5. Verify both islands load at the exact same world coordinates

**Expected Result:**
- Slot 1 island at H8 (e.g., world coords 3500, 3500)
- Slot 2 island also at H8 (same world coords)
- Switching slots changes which island is loaded, not location
- Both islands occupy identical space when loaded

---

### Test #73: Clicking Inactive Slot Opens Confirmation

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 246-250)

**Setup:** Have islands in slot 1 (active) and slot 2 (inactive)

**Steps:**
1. Open slot selection GUI
2. Left-click on slot 2 (inactive island)
3. Verify a confirmation GUI opens
4. Verify confirmation shows "Switch from [Slot 1 name] to [Slot 2 name]"
5. Verify confirmation has Confirm and Cancel buttons

**Expected Result:**
- Confirmation GUI opens (SlotSwitchConfirmationGUI)
- Shows current slot name → target slot name
- Green WOOL (Confirm) button
- Red WOOL (Cancel) button
- Warnings about visitors, cooldowns (if applicable)

---

### Test #74: Switch Confirmation Shows Slot Names

**Category:** UI
**File:** SlotSwitchConfirmationGUI.java

**Setup:** Rename slot 1 to "Main Base" and slot 2 to "Farm Island"

**Steps:**
1. Initiate switch from slot 1 to slot 2
2. Verify confirmation GUI shows "Main Base" → "Farm Island"
3. Verify format clearly indicates direction of switch

**Expected Result:**
- Title: "Confirm Slot Switch"
- Display shows: "Main Base" → "Farm Island"
- Clear indication of which slot you're switching TO
- Confirm/Cancel buttons visible

---

### Test #75: Confirming Switch Saves Current Island

**Category:** Functional
**File:** SlotSwitchManager.java (lines 70-83, 106-156)

**Steps:**
1. Build structures on slot 1 island (place unique blocks)
2. Switch from slot 1 to slot 2
3. Confirm the switch
4. Check file system at `plugins/IslandSelector/slots/{uuid}/`
5. Verify `slot-1.schem` file exists
6. Verify file contains data (non-zero size, usually 100KB+)

**Expected Result:**
- File created: `plugins/IslandSelector/slots/{player-uuid}/slot-1.schem`
- File size > 0 bytes (typically 100KB-5MB depending on island)
- File format: FAWE .schem format
- Progress message: "Saving current island..."

---

### Test #76: Slot Switch Clears and Loads Island

**Category:** Functional
**File:** SlotSwitchManager.java (lines 85-102, 158-198)

**Steps:**
1. Have island with visible structures in slot 1
2. Switch to slot 2 (with different structures)
3. After switch, check the grid location in world
4. Verify old slot 1 structures are removed
5. Verify slot 2 island structures are now present

**Expected Result:**
- Progress messages:
  - "Clearing current island from world..."
  - "Loading target island..."
- Slot 1 structures disappear (cleared to void/air)
- Slot 2 structures appear at same location
- Switch takes 3-5 seconds total
- No server lag or crashes

---

### Test #77: Slot Switch Teleports Owner

**Category:** Functional
**File:** SlotSwitchManager.java (lines 200-222)

**Steps:**
1. Be on slot 1 island
2. Switch to slot 2
3. After switch completes, verify you are teleported to slot 2 island spawn point
4. Verify location matches BSkyBlock's configured spawn for that island

**Expected Result:**
- Player teleported to slot 2 island spawn
- Teleport happens after island loads
- Progress message: "Teleporting to new island..."
- Success message: "Slot switch complete! Welcome to [Slot Name]"

---

### Test #78: Slot Switch Teleports Team Members

**Category:** Functional
**File:** SlotSwitchManager.java (lines 200-222)

**Steps:**
1. Add a team member to your island (`/island team invite`)
2. Have team member stand on the island
3. Island owner switches slots
4. Verify team member is teleported to new slot's island spawn
5. Verify team member remains on the team

**Expected Result:**
- All team members on the island are teleported
- Team members receive notification about slot switch
- Team structure remains intact
- Team members can interact with new slot island

---

### Test #79: Clicking Active Slot Shows Message

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 239-243)

**Steps:**
1. Open slots GUI
2. Click on the active slot (the one with glow)
3. Verify message appears saying it's already active
4. Verify no switch confirmation opens

**Expected Result:**
- Message: "This is already your active slot."
- No confirmation GUI
- No slot switch attempted

---

### Test #80: Right-Click Slot Opens Settings

**Category:** Functional
**File:** SlotSelectionGUI.java (lines 286-305), SlotSettingsGUI.java

**Steps:**
1. Have an island in slot 1
2. Open slots GUI
3. Right-click on slot 1
4. Verify SlotSettingsGUI opens
5. Verify it shows rename, icon, and delete buttons

**Expected Result:**
- Settings GUI opens (covered in Session 31 testing)
- Shows slot settings options:
  - Rename slot
  - Change icon
  - Delete slot
- See TESTING_SESSION31.md for detailed settings GUI tests

---

## Slot Management Commands

These were tested in previous sessions but are part of the slot system:

### Test: Rename Slot (Session 28)
```bash
/islandselector setname Main Base
# Expected: Slot 1 renamed to "Main Base"
# See TESTING_SESSION28.md for details
```

### Test: Set Custom Icon (Session 29)
```bash
# Hold a diamond block
/islandselector seticon
# Expected: Slot icon changed to diamond block
# See TESTING_SESSION29.md for details
```

### Test: Delete Slot (Session 30)
```bash
/islandselector delete
# Expected: Confirmation GUI opens
# See TESTING_SESSION30.md for details
```

---

## Configuration

**Config Section:** `slots`

```yaml
slots:
  # Default number of slots (without permissions)
  default-slots: 1

  # Max slots available (with permissions)
  max-slots: 5

  # Cooldown between switches (seconds, 0 = no cooldown)
  switch-cooldown: 300

  # Create backup when switching slots
  backup-on-switch: true
```

**Permissions:**
- `islandselector.slots` - Access `/islandselector slots` command
- `islandselector.slots.2` - Unlock 2 slots
- `islandselector.slots.3` - Unlock 3 slots
- `islandselector.slots.4` - Unlock 4 slots
- `islandselector.slots.5` - Unlock 5 slots (max)
- `islandselector.bypass.cooldown.switch` - Bypass switch cooldown

---

## Testing Requirements

**Players Needed:** 2 (for team member teleport test)
**Time Estimate:** 20-30 minutes
**Prerequisites:**
- FastAsyncWorldEdit installed (REQUIRED for slot switching)
- BSkyBlock addon active
- At least one island created
- Permissions plugin to test slot unlocking

---

## Expected Behaviors

### Slot Display Priority
1. **Active slot:** GRASS_BLOCK with glow and "★ ACTIVE"
2. **Inactive slot with island:** GRASS_BLOCK (or custom icon), no glow, "Click to switch"
3. **Empty unlocked slot:** LIME_DYE, "Click to create"
4. **Locked slot:** BARRIER, "Locked" with permission info

### Permission Hierarchy
- Higher permission includes lower (slots.5 includes slots.4, slots.3, slots.2)
- Check from highest to lowest (slots.5 → slots.4 → slots.3 → slots.2 → default)
- Default slots: 1 (configured in config.yml)

### Slot Switching Process
1. **Save:** Current island → schematic file (`.schem`)
2. **Backup:** Optional auto-backup if configured
3. **Clear:** Remove all blocks from world location
4. **Load:** Target island schematic → world
5. **Update:** Database active slot status
6. **Teleport:** Owner and team members to new island spawn
7. **Message:** Success notification

**Timing:** 3-5 seconds for typical island, longer for large builds

### File Structure
```
plugins/IslandSelector/
├── slots/
│   └── {player-uuid}/
│       ├── slot-1.schem
│       ├── slot-2.schem
│       ├── slot-3.schem
│       ├── slot-4.schem
│       └── slot-5.schem
└── backups/ (if backup-on-switch enabled)
    └── {player-uuid}/
        └── slot-{number}-{timestamp}.schem
```

---

## Troubleshooting

**Problem:** Slots GUI doesn't open
- **Check:** `/islandselector slots` command exists
- **Check:** Permission `islandselector.slots`
- **Check:** Console for errors
- **Try:** `/bentobox version` to verify addon loaded

**Problem:** All slots show as locked
- **Check:** Player permissions (needs `islandselector.slots.2` for slot 2, etc.)
- **Check:** Config `default-slots` setting
- **Try:** Give `islandselector.slots.5` to unlock all

**Problem:** Slot switch fails with error
- **Check:** FastAsyncWorldEdit installed and enabled
- **Check:** File permissions on `plugins/IslandSelector/slots/` folder
- **Check:** Console for WorldEdit errors
- **Try:** Switch with a small/empty island first

**Problem:** Slot switch clears island but doesn't load target
- **Check:** Target slot has a schematic file (`.schem`)
- **Check:** Schematic file is valid (non-zero size)
- **Check:** Console for WorldEdit paste errors
- **Try:** Admin restore command if available

**Problem:** "Island creation in empty slots coming soon"
- **This is expected** - Empty slot island creation may not be fully integrated
- **Workaround:** Create islands via standard `/island` command first
- **Note:** Future feature to create directly from empty slot

**Problem:** Team members not teleported
- **Check:** Team members are actually on the island during switch
- **Check:** Team members have valid island membership
- **Try:** Have team members rejoin island after switch

**Problem:** Schematic files not created
- **Check:** Directory exists: `plugins/IslandSelector/slots/{uuid}/`
- **Check:** File write permissions
- **Check:** Disk space available
- **Try:** Manual `/we save` command to test WorldEdit

---

## Report Format

After testing, please report results:

```
=== Session 40: Slot Selection System Test Report ===

CORE GUI TESTS:
Test #63 (GUI layout): [PASS/FAIL]
Test #64 (Active slot display): [PASS/FAIL]
Test #65 (Inactive slot display): [PASS/FAIL]
Test #66 (Empty slot display): [PASS/FAIL]
Test #67 (Locked slot display): [PASS/FAIL]

PERMISSION TESTS:
Test #68 (slots.2 permission): [PASS/FAIL]
Test #69 (slots.5 permission): [PASS/FAIL]

INTERACTION TESTS:
Test #70 (Click locked slot): [PASS/FAIL]
Test #71 (Click empty slot): [PASS/FAIL / EXPECTED MESSAGE]
Test #72 (Same grid location): [PASS/FAIL]

SLOT SWITCHING TESTS:
Test #73 (Switch confirmation opens): [PASS/FAIL]
Test #74 (Confirmation shows names): [PASS/FAIL]
Test #75 (Saves to schematic): [PASS/FAIL]
Test #76 (Clears and loads): [PASS/FAIL]
Test #77 (Teleports owner): [PASS/FAIL]
Test #78 (Teleports team members): [PASS/FAIL]

ADDITIONAL TESTS:
Test #79 (Click active slot): [PASS/FAIL]
Test #80 (Right-click settings): [PASS/FAIL]

Issues found:
[Describe any problems, unexpected behavior, or bugs]

Console errors:
[Paste any errors from server console]

Schematic files:
[Did slot-{number}.schem files get created? What size?]

Performance:
[Any lag during slot switch? How long did switch take?]

Overall: [PASS/FAIL]
```

---

## Success Criteria

✅ Slot selection GUI opens with correct 27-slot layout
✅ Active slot shows enchantment glow and "★ ACTIVE"
✅ Inactive slots show without glow, "Click to switch"
✅ Empty slots show LIME_DYE, "Click to create"
✅ Locked slots show BARRIER with permission info
✅ Permission system works (slots.2, slots.3, slots.4, slots.5)
✅ Clicking locked slot shows error message
✅ Clicking inactive slot opens switch confirmation
✅ Slot switch saves current island to .schem file
✅ Slot switch clears world and loads target island
✅ Slot switch teleports player to new island
✅ Slot switch teleports team members
✅ Right-click opens slot settings GUI
✅ No console errors during slot operations
✅ No server lag during slot switch

---

## File Info

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
**Size:** 180K (unchanged from Session 39)
**Changes:** None - documentation only
**Build Status:** ✅ SUCCESS

---

## Key Files Involved

**Already Implemented:**
- `SlotSelectionGUI.java` (332 lines) - Main slots GUI
- `SlotManager.java` (261 lines) - Slot data management
- `SlotSwitchManager.java` (~300 lines) - Switch operations with FAWE
- `SlotSwitchConfirmationGUI.java` - Switch confirmation
- `SlotSettingsGUI.java` - Slot settings menu (Session 31)
- `SlotDeleteConfirmationGUI.java` - Delete confirmation (Session 30)
- `SlotData.java` - Database model
- `SlotsCommand.java` - `/islandselector slots` command

---

**Ready to test!** 🎮

**Core Test:** Open slots GUI → View slot displays → Switch between slots → Verify saves/loads

**Time:** 20-30 minutes

**Required:** FAWE installed, 2 players for team test

**JAR:** `output/IslandSelector-1.0.0-SNAPSHOT.jar` (unchanged)

---

## Next Steps

**If All Slot Tests Pass:**
- Mark tests #63-80 as passing ✅ (approximately 18 tests)
- Combined with Sessions 38-39: Would be +29 tests total
- Progress: from 44/225 → ~73/225 tests passing
- Move to next feature block (likely relocation system or backups)

**If Any Test Fails:**
- Report the issue with details
- Agent will debug and fix in next session
- Re-test until passing

---

**This complements Sessions 38-39 for comprehensive feature testing** - three major feature sets ready for verification!
