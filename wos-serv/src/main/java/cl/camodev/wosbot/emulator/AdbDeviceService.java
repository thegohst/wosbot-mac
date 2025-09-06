package cl.camodev.wosbot.emulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for interacting with ADB to detect and manage Android devices/emulators
 */
public class AdbDeviceService {
    
    private static final String ADB_PATH_WINDOWS = "adb/adb.exe";
    private static final String ADB_PATH_UNIX = "adb/adb";
    private static final Pattern DEVICE_PATTERN = Pattern.compile("^([^\\s]+)\\s+device\\s*$");
    
    /**
     * Represents an ADB device/emulator
     */
    public static class AdbDevice {
        private final String deviceId;
        private final String name;
        private final EmulatorType type;
        private final boolean isEmulator;
        
        public AdbDevice(String deviceId, String name, EmulatorType type, boolean isEmulator) {
            this.deviceId = deviceId;
            this.name = name;
            this.type = type;
            this.isEmulator = isEmulator;
        }
        
        public String getDeviceId() { return deviceId; }
        public String getName() { return name; }
        public EmulatorType getType() { return type; }
        public boolean isEmulator() { return isEmulator; }
        
        @Override
        public String toString() {
            return name + " (" + deviceId + ")";
        }
    }
    
    /**
     * Gets the path to the ADB executable
     */
    private static String getAdbPath() {
        // Determine OS-specific ADB path
        String osName = System.getProperty("os.name").toLowerCase();
        String adbPath = osName.contains("win") ? ADB_PATH_WINDOWS : ADB_PATH_UNIX;
        
        // Try to find ADB in the project directory
        File projectAdb = new File(adbPath);
        if (projectAdb.exists()) {
            return projectAdb.getAbsolutePath();
        }
        
        // Try to find ADB in system PATH
        return "adb";
    }
    
    /**
     * Gets all connected ADB devices and emulators
     */
    public static List<AdbDevice> getConnectedDevices() {
        List<AdbDevice> devices = new ArrayList<>();
        
        try {
            String adbPath = getAdbPath();
            Process process = Runtime.getRuntime().exec(adbPath + " devices");
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    Matcher deviceMatcher = DEVICE_PATTERN.matcher(line);
                    if (deviceMatcher.matches()) {
                        String deviceId = deviceMatcher.group(1);
                        AdbDevice device = createDeviceFromId(deviceId);
                        if (device != null) {
                            devices.add(device);
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing ADB command: " + e.getMessage());
        }
        
        return devices;
    }
    
    /**
     * Creates an AdbDevice object from a device ID
     */
    private static AdbDevice createDeviceFromId(String deviceId) {
        boolean isEmulator = deviceId.startsWith("emulator-");
        
        // Get device properties to determine emulator type and name
        String deviceName = getDeviceProperty(deviceId, "ro.product.model");
        String brand = getDeviceProperty(deviceId, "ro.product.brand");
        String manufacturer = getDeviceProperty(deviceId, "ro.product.manufacturer");
        
        EmulatorType type = determineEmulatorType(deviceName, brand, manufacturer, deviceId);
        
        // Generate display name
        String displayName;
        if (isEmulator) {
            if (type != null) {
                displayName = type.getDisplayName() + " - " + deviceId;
            } else {
                displayName = "Android Emulator - " + deviceId;
            }
        } else {
            displayName = deviceName != null && !deviceName.isEmpty() ? 
                deviceName + " - " + deviceId : "Physical Device - " + deviceId;
        }
        
        return new AdbDevice(deviceId, displayName, type, isEmulator);
    }
    
    /**
     * Gets a device property using ADB
     */
    private static String getDeviceProperty(String deviceId, String property) {
        try {
            String adbPath = getAdbPath();
            Process process = Runtime.getRuntime().exec(
                adbPath + " -s " + deviceId + " shell getprop " + property
            );
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String result = reader.readLine();
                process.waitFor();
                return result != null ? result.trim() : "";
            }
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }
    
    /**
     * Determines the emulator type based on device properties
     */
    private static EmulatorType determineEmulatorType(String deviceName, String brand, String manufacturer, String deviceId) {
        if (deviceName == null && brand == null && manufacturer == null) {
            return null;
        }
        
        String combined = (deviceName + " " + brand + " " + manufacturer).toLowerCase();
        
        // Check for MuMu emulator
        if (combined.contains("mumu") || combined.contains("netease")) {
            return EmulatorType.MUMU;
        }
        
        // Check for MEmu emulator
        if (combined.contains("memu") || combined.contains("microvirt")) {
            return EmulatorType.MEMU;
        }
        
        // Check for LDPlayer
        if (combined.contains("ldplayer") || combined.contains("changzhida")) {
            return EmulatorType.LDPLAYER;
        }
        
        // Check for Android Studio emulator
        if (combined.contains("android") || combined.contains("google") || 
            combined.contains("sdk") || deviceId.startsWith("emulator-")) {
            return EmulatorType.ANDROID_STUDIO;
        }
        
        return null; // Unknown emulator type
    }
    
    /**
     * Checks if ADB is available and working
     */
    public static boolean isAdbAvailable() {
        try {
            String adbPath = getAdbPath();
            Process process = Runtime.getRuntime().exec(adbPath + " version");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Starts ADB server if not already running
     */
    public static boolean startAdbServer() {
        try {
            String adbPath = getAdbPath();
            Process process = Runtime.getRuntime().exec(adbPath + " start-server");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Gets running emulators only (filters out physical devices)
     */
    public static List<AdbDevice> getRunningEmulators() {
        return getConnectedDevices().stream()
            .filter(AdbDevice::isEmulator)
            .toList();
    }
}
