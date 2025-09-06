# WosBot Setup Guide

This guide will help you set up a new laptop to run the WosBot (Whiteout Survival Bot) application.

## Prerequisites

The WosBot application requires several components to function properly:
- Java 21 (OpenJDK)
- Maven 3.6+
- Android SDK (for ADB and emulator support)
- OpenCV (for image processing)
- Tesseract OCR (for text recognition)
- Git (for version control)

## Platform-Specific Installation

### 🍎 macOS Setup

#### 1. Install Homebrew (Package Manager)
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### 2. Install Java 21
```bash
brew install openjdk@21
```

Add to your shell profile (`~/.zshrc` or `~/.bash_profile`):
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.*/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

#### 3. Install Maven
```bash
brew install maven
```

#### 4. Install Android SDK
```bash
brew install android-sdk
brew install android-platform-tools
```

Set Android environment variables:
```bash
export ANDROID_HOME=/opt/homebrew/share/android-sdk
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
```

#### 5. Install OpenCV
```bash
brew install opencv
```

#### 6. Install Tesseract OCR
```bash
brew install tesseract
```

#### 7. Install Git
```bash
brew install git
```

#### 8. Install Android Studio (Optional but Recommended)
Download from: https://developer.android.com/studio
- This provides the Android emulator management interface
- Create at least one AVD (Android Virtual Device) for testing

### 🐧 Linux (Ubuntu/Debian) Setup

#### 1. Update Package Manager
```bash
sudo apt update && sudo apt upgrade -y
```

#### 2. Install Java 21
```bash
sudo apt install openjdk-21-jdk
```

Set Java environment:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

#### 3. Install Maven
```bash
sudo apt install maven
```

#### 4. Install Android SDK
```bash
# Install Android Studio or command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p ~/Android/Sdk/cmdline-tools
mv cmdline-tools ~/Android/Sdk/cmdline-tools/latest

# Set environment variables
export ANDROID_HOME=~/Android/Sdk
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"

# Install platform tools
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "emulator"
```

#### 5. Install OpenCV
```bash
sudo apt install libopencv-dev python3-opencv
```

#### 6. Install Tesseract OCR
```bash
sudo apt install tesseract-ocr
```

#### 7. Install Git
```bash
sudo apt install git
```

### 🪟 Windows Setup

#### 1. Install Java 21
- Download OpenJDK 21 from: https://adoptium.net/
- Add to PATH and set JAVA_HOME environment variable

#### 2. Install Maven
- Download from: https://maven.apache.org/download.cgi
- Extract and add to PATH

#### 3. Install Android SDK
- Download Android Studio: https://developer.android.com/studio
- Or download command line tools and set up manually
- Set ANDROID_HOME and add platform-tools to PATH

#### 4. Install OpenCV
- Download pre-built binaries from: https://opencv.org/releases/
- Extract and add to system PATH

#### 5. Install Tesseract OCR
- Download from: https://github.com/UB-Mannheim/tesseract/wiki
- Add to system PATH

#### 6. Install Git
- Download from: https://git-scm.com/download/win

## Post-Installation Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd wosbot-master
```

### 2. Verify Java Installation
```bash
java -version
# Should show Java 21.x.x

javac -version
# Should show Java 21.x.x
```

### 3. Verify Maven Installation
```bash
mvn -version
# Should show Maven 3.6+ and Java 21
```

### 4. Verify Android SDK
```bash
adb version
# Should show Android Debug Bridge version

adb devices
# Should list connected Android devices/emulators
```

### 5. Test OpenCV (Optional)
```bash
# OpenCV will be automatically loaded by the application
# No additional testing required
```

### 6. Create Android Virtual Device (AVD)
If using Android Studio:
1. Open Android Studio
2. Go to Tools → AVD Manager
3. Create Virtual Device
4. Choose a device (recommended: Pixel 8 Pro)
5. Download system image (API 34 recommended)
6. Name it `white_bot` for optimal bot performance
7. Finish creation

## Build and Run the Application

### 1. Build the Project
```bash
cd wosbot-master
mvn clean install -DskipTests
```

### 2. Run the Application
```bash
./run.sh
```

Or manually:
```bash
export JAVA_HOME=/path/to/java21
java -cp "wos-hmi/target/wos-bot-1.5.3.jar:wos-hmi/target/lib/*" \
     -Djna.library.path="/opt/homebrew/lib" \
     cl.camodev.wosbot.main.Main
```

## Troubleshooting

### Common Issues

#### 1. Java Version Mismatch
**Error**: "Unsupported major.minor version"
**Solution**: Ensure Java 21 is installed and JAVA_HOME is set correctly

#### 2. OpenCV Loading Issues
**Error**: "OpenCV native library failed to load"
**Solution**: 
- macOS: `brew install opencv`
- Linux: `sudo apt install libopencv-dev`
- Windows: Download OpenCV binaries and add to PATH

#### 3. Tesseract Not Found
**Error**: "TesseractException: Tesseract is not installed"
**Solution**: Install Tesseract and ensure it's in system PATH

#### 4. ADB Not Found
**Error**: "adb command not found"
**Solution**: Install Android SDK platform-tools and add to PATH

#### 5. No Emulator Found
**Error**: "No Android Virtual Devices found"
**Solution**: Create at least one AVD using Android Studio

### Performance Optimization

#### For Better Performance:
1. **Use SSD storage** for faster I/O operations
2. **8GB+ RAM** recommended for running emulators
3. **Enable hardware acceleration** for emulators:
   - Intel: Enable Intel HAXM
   - AMD: Enable Android Emulator Hypervisor Driver
4. **Close unnecessary applications** when running the bot

## Environment Variables Summary

Add these to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
# Java
export JAVA_HOME=/path/to/java21
export PATH="$JAVA_HOME/bin:$PATH"

# Android
export ANDROID_HOME=/path/to/android/sdk
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"

# Optional: For better library detection
export LD_LIBRARY_PATH="/usr/local/lib:$LD_LIBRARY_PATH"  # Linux
export DYLD_LIBRARY_PATH="/opt/homebrew/lib:$DYLD_LIBRARY_PATH"  # macOS
```

## Next Steps

After successful installation:
1. Start an Android emulator
2. Launch the WosBot application
3. Select your emulator from the dropdown
4. Load a profile or create a new one
5. Click "Start Bot" to begin automation

## Support

If you encounter issues:
1. Check the logs in `wos-hmi/target/log/bot.log`
2. Verify all dependencies are correctly installed
3. Ensure Android emulator is running and detectable via `adb devices`
4. Check Java version compatibility

---

**Note**: This application is designed for educational and automation purposes. Ensure compliance with game terms of service when using automation tools.
