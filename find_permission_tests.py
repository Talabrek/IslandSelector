import json

with open('feature_list.json', 'r') as f:
    tests = json.load(f)

# Find permission-related tests
print("Permission, alias, and integration tests:")
for i, test in enumerate(tests):
    desc = test['description'].lower()
    if not test.get('passes', False):
        if any(word in desc for word in ['permission', 'alias', 'vault', 'placeholder', 'message', 'locale', 'color']):
            print(f"{i}. {test['description'][:80]}")
