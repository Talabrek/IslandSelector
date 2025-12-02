import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

# Find tests related to warp (Session 42 was about neighbor warp)
print("Tests 133-145:")
for i in range(133, min(146, len(tests))):
    test = tests[i]
    status = "PASS" if test.get('passes', False) else "FAIL"
    print(f"{i}. [{status}] {test['description']}")
