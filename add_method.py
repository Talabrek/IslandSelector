import re

filepath = 'src/main/java/world/bentobox/islandselector/gui/MainGridGUI.java'

with open(filepath, 'r') as f:
    content = f.read()

if 'centerOnCoordinate' in content:
    print('Method already exists')
else:
    # Insert after refresh() method
    old = '''        populateInventory();
        player.updateInventory();
    }

    /**
     * Get the grid coordinate for an inventory slot
     */'''

    new = '''        populateInventory();
        player.updateInventory();
    }

    /**
     * Center the viewport on a specific coordinate
     * @param coord The coordinate to center on
     */
    public void centerOnCoordinate(GridCoordinate coord) {
        viewportX = coord.getX() - GRID_COLS / 2;
        viewportZ = coord.getZ() - GRID_ROWS / 2;
        viewportX = Math.max(settings.getGridMinX(), viewportX);
        viewportX = Math.min(settings.getGridMaxX() - GRID_COLS + 1, viewportX);
        viewportZ = Math.max(settings.getGridMinZ(), viewportZ);
        viewportZ = Math.min(settings.getGridMaxZ() - GRID_ROWS + 1, viewportZ);
    }

    /**
     * Get the grid coordinate for an inventory slot
     */'''

    new_content = content.replace(old, new)

    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print('Method added successfully')
    else:
        print('Pattern not found')
