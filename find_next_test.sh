#!/bin/bash
# Find first failing test in feature_list.json

grep -n '"passes": false' feature_list.json | head -1
