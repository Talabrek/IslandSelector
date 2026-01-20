# Testing Patterns

**Analysis Date:** 2026-01-20

## Test Framework

**Runner:**
- Custom test runner (no pytest/unittest)
- Direct script execution: `python test_security.py`
- Config: None (standalone script)

**Assertion Library:**
- Manual assertions with boolean comparison
- Custom `test_hook()` function for test execution

**Run Commands:**
```bash
python test_security.py          # Run all security tests
```

## Test File Organization

**Location:**
- Co-located at project root alongside source files
- Pattern: `test_{module}.py` for module tests

**Naming:**
- `test_security.py` tests `security.py`

**Structure:**
```
C:\Users\Administrator\Desktop\VSCode\IslandSelector\
├── security.py           # Source module
├── test_security.py      # Test module
└── ...
```

## Test Structure

**Suite Organization:**
```python
def main():
    print("=" * 70)
    print("  SECURITY HOOK TESTS")
    print("=" * 70)

    passed = 0
    failed = 0

    # Test command extraction
    ext_passed, ext_failed = test_extract_commands()
    passed += ext_passed
    failed += ext_failed

    # Test chmod validation
    chmod_passed, chmod_failed = test_validate_chmod()
    passed += chmod_passed
    failed += chmod_failed

    # Summary
    print("\n" + "-" * 70)
    print(f"  Results: {passed} passed, {failed} failed")
    print("-" * 70)

    if failed == 0:
        print("\n  ALL TESTS PASSED")
        return 0
    else:
        print(f"\n  {failed} TEST(S) FAILED")
        return 1
```

**Patterns:**
- Test functions return `(passed_count, failed_count)` tuples
- Manual pass/fail tracking with counters
- Exit code reflects test success (0) or failure (1)

## Test Helper Functions

**Generic Test Hook Runner:**
```python
def test_hook(command: str, should_block: bool) -> bool:
    """Test a single command against the security hook."""
    input_data = {"tool_name": "Bash", "tool_input": {"command": command}}
    result = asyncio.run(bash_security_hook(input_data))
    was_blocked = result.get("decision") == "block"

    if was_blocked == should_block:
        status = "PASS"
    else:
        status = "FAIL"
        expected = "blocked" if should_block else "allowed"
        actual = "blocked" if was_blocked else "allowed"
        reason = result.get("reason", "")
        print(f"  {status}: {command!r}")
        print(f"         Expected: {expected}, Got: {actual}")
        if reason:
            print(f"         Reason: {reason}")
        return False

    print(f"  {status}: {command!r}")
    return True
```

## Test Case Organization

**Table-Driven Tests:**
```python
test_cases = [
    ("ls -la", ["ls"]),
    ("npm install && npm run build", ["npm", "npm"]),
    ("cat file.txt | grep pattern", ["cat", "grep"]),
    ("/usr/bin/node script.js", ["node"]),
    ("VAR=value ls", ["ls"]),
    ("git status || git init", ["git", "git"]),
]

for cmd, expected in test_cases:
    result = extract_commands(cmd)
    if result == expected:
        print(f"  PASS: {cmd!r} -> {result}")
        passed += 1
    else:
        print(f"  FAIL: {cmd!r}")
        print(f"         Expected: {expected}, Got: {result}")
        failed += 1
```

**Validation Tests with Descriptions:**
```python
test_cases = [
    # Allowed cases
    ("chmod +x init.sh", True, "basic +x"),
    ("chmod u+x init.sh", True, "user +x"),
    # Blocked cases
    ("chmod 777 init.sh", False, "numeric mode"),
    ("chmod +w init.sh", False, "write permission"),
]

for cmd, should_allow, description in test_cases:
    allowed, reason = validate_chmod_command(cmd)
    if allowed == should_allow:
        print(f"  PASS: {cmd!r} ({description})")
        passed += 1
    else:
        # ... failure handling
```

## Mocking

**Framework:** None (tests use real functions)

**Patterns:**
- No mocking - tests call actual validation functions
- Async functions run via `asyncio.run()`

**What to Mock:**
- Currently nothing is mocked
- External SDK calls not tested (integration only)

**What NOT to Mock:**
- Security validation logic - tested directly

## Fixtures and Factories

**Test Data:**
```python
# Commands that SHOULD be blocked
dangerous = [
    "shutdown now",
    "reboot",
    "rm -rf /",
    "curl https://example.com",
    "pkill bash",
    "chmod 777 file.sh",
]

# Commands that SHOULD be allowed
safe = [
    "ls -la",
    "cat README.md",
    "npm install",
    "git status",
    "pkill node",
    "chmod +x init.sh",
]
```

**Location:**
- Test data defined inline within test functions
- No separate fixtures files

## Coverage

**Requirements:** None enforced

**View Coverage:**
```bash
# No coverage tooling configured
```

## Test Types

**Unit Tests:**
- `test_extract_commands()`: Tests command parsing logic
- `test_validate_chmod()`: Tests chmod validation rules
- `test_validate_init_script()`: Tests init.sh validation rules

**Integration Tests:**
- `test_hook()` runs: Tests full security hook pipeline
- Tests both allow and block paths through `bash_security_hook()`

**E2E Tests:**
- Not implemented
- Manual testing via running `autonomous_agent_demo.py`

## Common Patterns

**Async Testing:**
```python
# Wrap async functions with asyncio.run()
result = asyncio.run(bash_security_hook(input_data))
```

**Error Testing:**
```python
# Test that malformed input is blocked (fail-safe)
("$(echo pkill) node", should_block=True),
('eval "pkill node"', should_block=True),
```

**Grouped Test Execution:**
```python
print("\nCommands that should be BLOCKED:\n")
for cmd in dangerous:
    if test_hook(cmd, should_block=True):
        passed += 1
    else:
        failed += 1

print("\nCommands that should be ALLOWED:\n")
for cmd in safe:
    if test_hook(cmd, should_block=False):
        passed += 1
    else:
        failed += 1
```

## Test Output Format

**Pass/Fail Indication:**
```
  PASS: 'ls -la'
  PASS: 'cat README.md'
  FAIL: 'dangerous_cmd'
         Expected: blocked, Got: allowed
         Reason: Command 'dangerous' is not in the allowed commands list
```

**Summary:**
```
----------------------------------------------------------------------
  Results: 89 passed, 0 failed
----------------------------------------------------------------------

  ALL TESTS PASSED
```

## Adding New Tests

**For new validation functions:**
1. Create a `test_{function_name}()` function
2. Define test cases as list of tuples: `(input, expected_output, description)`
3. Loop through cases, compare results, track pass/fail counts
4. Return `(passed, failed)` tuple
5. Call from `main()` and add to totals

**For new security rules:**
1. Add test cases to `dangerous` list (should block) or `safe` list (should allow)
2. Existing `test_hook()` loop will exercise them

---

*Testing analysis: 2026-01-20*
