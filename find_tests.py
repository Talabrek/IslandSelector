import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

print(f"Total tests: {len(tests)}")
print("\nTests 100-110 (indices):")
for i in range(100, min(111, len(tests))):
    test = tests[i]
    status = "PASS" if test.get('passes', False) else "FAIL"
    print(f"{i}. [{status}] {test['description']}")
    
print("\nTests 137-145 (indices):")
for i in range(137, min(146, len(tests))):
    if i < len(tests):
        test = tests[i]
        status = "PASS" if test.get('passes', False) else "FAIL"
        print(f"{i}. [{status}] {test['description']}")
