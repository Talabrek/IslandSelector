#!/bin/bash

# Backup existing TESTING.md
cp TESTING.md TESTING.md.session39backup

# Create new header for TESTING.md
cat > TESTING.md << 'EOF'
# IslandSelector - Manual Testing Instructions

## Build Information
- **Build Date:** Session 40 (Latest - Slot Selection System Documentation)
- **JAR Location:** `output/IslandSelector-1.0.0-SNAPSHOT.jar`
- **JAR Size:** 180K
- **Status:** ✅ 44 tests verified, 29 tests pending manual verification

## Latest Sessions (38-40)

### Session 40: Slot Selection System & Slot Management ⭐ NEW
📋 **See [SESSION40_FOR_HUMAN_TESTER.md](SESSION40_FOR_HUMAN_TESTER.md) for quick test guide**
📋 **See [TESTING_SESSION40.md](TESTING_SESSION40.md) for detailed testing**

**Features Ready:** 18 slot-related tests
- Slot Selection GUI with 27-slot layout
- Active/inactive/empty/locked slot displays
- Permission-based slot unlocking (slots.2 through slots.5)
- Slot interactions (click locked/empty/inactive/active slots)
- Complete slot switching cycle with FAWE
- Schematic save/load operations
- Player and team member teleportation
- Slot cooldown system

**Testing Time:** 20-30 minutes
**Players Required:** 2 accounts (for team member test)
**Dependencies:** FastAsyncWorldEdit (FAWE) REQUIRED for slot switching

### Session 39: Player Search Functionality
📋 **See [SESSION39_FOR_HUMAN_TESTER.md](SESSION39_FOR_HUMAN_TESTER.md) for quick test guide**
📋 **See [TESTING_SESSION39.md](TESTING_SESSION39.md) for detailed testing**

**Features Ready:** 4 search-related tests
- Search button displays (COMPASS item)
- Search finds player by name
- Partial name matching ("Not" finds "Notch")
- Player not found error handling

**Testing Time:** 10-15 minutes
**Players Required:** 2-3 accounts

### Session 38: Island Claiming & Premium Locations
📋 **See [SESSION38_FOR_HUMAN_TESTER.md](SESSION38_FOR_HUMAN_TESTER.md) for quick test guide**
📋 **See [TESTING_SESSION38.md](TESTING_SESSION38.md) for detailed testing**

**Features Ready:** 7 island claiming tests
- New player intercepts /island command (CRITICAL TEST!)
- Click-to-select, double-click-to-confirm flow
- Confirmation GUI with Confirm/Cancel buttons
- Island created at selected grid coordinates
- Premium location tooltips (gold block with price)
- Grid coordinate mapping verification

**Testing Time:** 15-20 minutes
**Players Required:** 3-4 fresh accounts (can only claim once per account)

### Combined Sessions 38 + 39 + 40
**Total Tests Pending:** 29 tests (7 + 4 + 18)
**Combined Testing Time:** 40-60 minutes
**If All Pass:** Progress jumps from 44/225 to 73/225 tests (32.4% complete)

EOF

# Append the rest of the old file (from line 13 onwards)
tail -n +13 TESTING.md.session39backup >> TESTING.md

echo "TESTING.md updated successfully"
