---
status: complete
phase: 05-config-toggle
source: [05-01-SUMMARY.md]
started: 2026-01-20T12:00:00Z
updated: 2026-01-20T12:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Config option exists with correct default
expected: In config.yml, `slots.enabled` exists under slots section with default value `true`
result: pass

### 2. Slots command works when enabled
expected: With `slots.enabled: true` in config, running `/islandselector slots` opens the slot selection GUI (assuming FAWE is installed)
result: pass

### 3. Slots command blocked when disabled
expected: With `slots.enabled: false` in config, running `/islandselector slots` shows red message "The slot system is disabled on this server." and does NOT open GUI
result: pass

### 4. Slots button visible when enabled
expected: With `slots.enabled: true`, opening the main grid GUI (`/islandselector`) shows the slots button in the GUI
result: pass

### 5. Slots button hidden when disabled
expected: With `slots.enabled: false`, opening the main grid GUI (`/islandselector`) does NOT show the slots button
result: pass

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
