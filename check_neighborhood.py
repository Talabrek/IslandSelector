import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

print("Neighborhood-related tests:")
for i, test in enumerate(tests):
    if 'neighborhood' in test['description'].lower() or 'neighbor' in test['description'].lower():
        status = "PASS" if test.get('passes', False) else "FAIL"
        print(f"{i}. [{status}] {test['description']}")
