import json

with open('feature_list.json', 'r') as f:
    data = json.load(f)

tests = [t for t in data['tests'] if not t['passes']]
print(f"Total failing tests: {len(tests)}\n")
print("Next 15 failing tests:")
for i, t in enumerate(tests[:15]):
    print(f"{i+1}. {t['id']}: {t['test_name']}")
