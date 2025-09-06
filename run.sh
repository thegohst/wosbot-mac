#!/bin/bash

# Whiteout Survival Bot - Run Script
# Enhanced with ADB emulator detection and Android Studio support

echo "🚀 Starting Whiteout Survival Bot..."
echo "📱 ADB Integration: Supports MuMu, MEmu, LDPlayer, and Android Studio emulators"
echo ""

# Set Java 21 environment
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"

# Navigate to the project directory
cd "$(dirname "$0")"

# Check if built version exists
if [ ! -f "wos-hmi/target/wos-bot-1.5.3.jar" ]; then
    echo "📦 Building project first..."
    mvn clean install -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Build failed. Please check the output above."
        exit 1
    fi
fi

echo "🎮 Launching application..."
echo "💡 Tip: The app now auto-detects emulators via ADB!"
echo ""

# Run the application
cd wos-hmi/target
java -Djna.library.path="/opt/homebrew/lib" -cp "wos-bot-1.5.3.jar:lib/*" cl.camodev.wosbot.main.Main

echo "👋 Application closed."
