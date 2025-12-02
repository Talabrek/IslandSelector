import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

print("Tests 100-132 (unimplemented range):")
for i in range(100, min(133, len(tests))):
    test = tests[i]
    status = "PASS" if test.get('passes', False) else "FAIL"
    if not test.get('passes', False):
        print(f"{i}. {test['description']}")
