import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

print("Tests 147-170:")
for i in range(147, min(171, len(tests))):
    test = tests[i]
    status = "PASS" if test.get('passes', False) else "FAIL"
    if not test.get('passes', False):
        print(f"{i}. {test['description'][:80]}")
