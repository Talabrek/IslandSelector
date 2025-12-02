import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

print("Detailed Neighborhood GUI Tests (137-147):\n")
for i in range(137, min(148, len(tests))):
    test = tests[i]
    print(f"Test #{i}: {test['description']}")
    print(f"Steps:")
    for step in test['steps']:
        print(f"  {step}")
    print()
