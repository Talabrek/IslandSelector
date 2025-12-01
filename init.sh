#!/bin/bash

# IslandSelector - BentoBox Addon Build Script
# This script builds the plugin and prepares it for manual testing

set -e  # Exit on error

echo "=========================================="
echo "IslandSelector Build Script"
echo "=========================================="
echo ""

# Check if Maven is installed
if command -v mvn &> /dev/null; then
    BUILD_TOOL="maven"
    echo "✓ Maven detected"
elif command -v gradle &> /dev/null; then
    BUILD_TOOL="gradle"
    echo "✓ Gradle detected"
else
    echo "❌ Error: Neither Maven nor Gradle found!"
    echo "Please install Maven or Gradle to build this plugin."
    exit 1
fi

echo ""
echo "Building with $BUILD_TOOL..."
echo ""

# Clean and build based on detected tool
if [ "$BUILD_TOOL" = "maven" ]; then
    mvn clean package -DskipTests
    JAR_PATH="target/IslandSelector-*.jar"
elif [ "$BUILD_TOOL" = "gradle" ]; then
    ./gradlew clean build
    JAR_PATH="build/libs/IslandSelector-*.jar"
fi

echo ""
echo "=========================================="
echo "Build Complete!"
echo "=========================================="
echo ""

# Find the built JAR
BUILT_JAR=$(ls $JAR_PATH 2>/dev/null | head -n 1)

if [ -z "$BUILT_JAR" ]; then
    echo "❌ Error: Could not find built JAR file!"
    echo "Expected location: $JAR_PATH"
    exit 1
fi

echo "✓ JAR built successfully: $BUILT_JAR"
echo ""

# Create output directory for easy access
OUTPUT_DIR="output"
mkdir -p "$OUTPUT_DIR"

# Copy JAR to output directory
cp "$BUILT_JAR" "$OUTPUT_DIR/"
FINAL_JAR="$OUTPUT_DIR/$(basename $BUILT_JAR)"

echo "✓ JAR copied to: $FINAL_JAR"
echo ""

# Calculate file size
FILE_SIZE=$(du -h "$FINAL_JAR" | cut -f1)
echo "📦 File size: $FILE_SIZE"
echo ""

echo "=========================================="
echo "Manual Testing Instructions"
echo "=========================================="
echo ""
echo "1. Copy the JAR to your test server:"
echo "   $FINAL_JAR"
echo "   → plugins/BentoBox/addons/"
echo ""
echo "2. Ensure dependencies are installed:"
echo "   ✓ BentoBox (required)"
echo "   ✓ BSkyBlock (required)"
echo "   ✓ FastAsyncWorldEdit (required)"
echo "   • Vault (optional - for economy)"
echo "   • PlaceholderAPI (optional - for placeholders)"
echo "   • Level addon (optional - for island levels)"
echo ""
echo "3. Start/restart your test server"
echo ""
echo "4. Check console for successful addon loading"
echo ""
echo "5. Test features according to feature_list.json"
echo ""
echo "=========================================="
echo "Quick Test Commands"
echo "=========================================="
echo ""
echo "In-game commands to test:"
echo "  /islandselector          - Open grid GUI"
echo "  /islandselector slots    - Open slot selection"
echo "  /islandselector locate   - Show your coordinates"
echo "  /islandselector help     - Show all commands"
echo ""
echo "Admin commands:"
echo "  /islandselector admin version  - Check plugin version"
echo "  /islandselector admin info A1  - Check location info"
echo ""
echo "=========================================="
echo ""
echo "Happy testing! 🎮"
echo ""
