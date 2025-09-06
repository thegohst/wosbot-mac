package cl.camodev.wosbot.emulator.impl;

import cl.camodev.wosbot.emulator.Emulator;
import com.android.ddmlib.IDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Android Studio Emulator implementation that works with ADB device IDs
 */
public class AndroidStudioEmulator extends Emulator {
    
    private static final Logger logger = LoggerFactory.getLogger(AndroidStudioEmulator.class);
    
    public AndroidStudioEmulator(String adbDeviceReference) {
        super(adbDeviceReference);
    }
    
    @Override
    public String getDeviceSerial(String emulatorNumber) {
        // For Android Studio emulators, the emulator number might be the ADB device ID
        // Check if it starts with "adb:" prefix
        if (emulatorNumber.startsWith("adb:")) {
            return emulatorNumber.substring(4); // Remove "adb:" prefix
        }
        
        // If it's already a device ID (like emulator-5554), return as is
        if (emulatorNumber.startsWith("emulator-")) {
            return emulatorNumber;
        }
        
        // Default format for Android Studio emulators
        return "emulator-" + emulatorNumber;
    }
    
    @Override
    public void launchEmulator(String emulatorNumber) {
        String deviceSerial = getDeviceSerial(emulatorNumber);
        
        // Check if ANY emulator is already running (don't require specific device serial)
        try {
            if (bridge == null) {
                initializeBridge();
            }
            
            IDevice[] devices = bridge.getDevices();
            for (IDevice device : devices) {
                if (device.isOnline() && device.getSerialNumber().startsWith("emulator-")) {
                    logger.info("Found running Android Studio emulator: {}", device.getSerialNumber());
                    logger.info("No need to launch new emulator, using existing one");
                    return;
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking for running emulators: {}", e.getMessage());
        }
        
        logger.info("No running Android Studio emulator found. Attempting to launch one...");
        
        try {
            // Try to get available AVDs
            String[] availableAvds = getAvailableAvds();
            
            if (availableAvds.length == 0) {
                logger.warn("No Android Virtual Devices (AVDs) found. Please create an AVD in Android Studio first.");
                logger.info("You can create AVDs in Android Studio: Tools → AVD Manager → Create Virtual Device");
                return;
            }
            
            // Prefer AVDs with meaningful names for this bot, otherwise use first available
            String avdToLaunch = choosePreferredAvd(availableAvds);
            logger.info("Launching AVD: {} (selected from {} available AVDs)", avdToLaunch, availableAvds.length);
            
            // Launch emulator using emulator command
            String emulatorPath = getEmulatorExecutablePath();
            ProcessBuilder pb = new ProcessBuilder(emulatorPath, "-avd", avdToLaunch, "-no-snapshot-save");
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            logger.info("Android Studio emulator launch command executed. Process started with PID: {}", process.pid());
            
            // Wait a bit for emulator to start initializing
            Thread.sleep(3000);
            
            // Check if process is still alive (indicates successful start)
            if (process.isAlive()) {
                logger.info("Android Studio emulator is starting up. It may take a few moments to be fully ready.");
                logger.info("Once ready, it will appear in the emulator dropdown for selection.");
            } else {
                logger.warn("Emulator process exited quickly. Exit code: {}", process.exitValue());
                logger.info("This might be because an emulator is already running or there's a configuration issue.");
            }
            
        } catch (Exception e) {
            logger.error("Failed to launch Android Studio emulator: {}", e.getMessage());
            logger.info("Fallback: Please manually start an Android Studio emulator");
        }
    }
    
    @Override
    public void closeEmulator(String emulatorNumber) {
        String deviceSerial = getDeviceSerial(emulatorNumber);
        logger.info("Attempting to close Android Studio emulator: {}", deviceSerial);
        
        boolean closed = false;
        
        // Method 1: Try ADB shutdown command
        try {
            withRetries(emulatorNumber, device -> {
                try {
                    device.executeShellCommand("reboot -p", new com.android.ddmlib.NullOutputReceiver());
                    logger.info("ADB shutdown command sent to emulator: {}", deviceSerial);
                    return null;
                } catch (Exception e) {
                    logger.debug("ADB shutdown failed: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }, "closeEmulator");
            
            // Wait a bit and check if it worked
            Thread.sleep(3000);
            if (!isRunning(emulatorNumber)) {
                closed = true;
                logger.info("Emulator successfully closed via ADB");
            }
        } catch (Exception e) {
            logger.debug("ADB method failed: {}", e.getMessage());
        }
        
        // Method 2: Try adb emu kill command as fallback
        if (!closed) {
            try {
                String port = deviceSerial.replace("emulator-", "");
                ProcessBuilder pb = new ProcessBuilder("adb", "-s", deviceSerial, "emu", "kill");
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    logger.info("Emulator kill command sent successfully");
                    Thread.sleep(2000);
                    if (!isRunning(emulatorNumber)) {
                        closed = true;
                        logger.info("Emulator successfully closed via emu kill");
                    }
                } else {
                    logger.debug("Emulator kill command failed with exit code: {}", exitCode);
                }
            } catch (Exception e) {
                logger.debug("Kill command method failed: {}", e.getMessage());
            }
        }
        
        if (!closed) {
            logger.warn("Could not close emulator {}. It may need to be closed manually.", deviceSerial);
        }
    }
    
    @Override
    public boolean isRunning(String emulatorNumber) {
        try {
            String deviceSerial = getDeviceSerial(emulatorNumber);
            
            // Directly check if device is available without using withRetries to avoid recursion
            if (bridge == null) {
                initializeBridge();
            }
            
            IDevice[] devices = bridge.getDevices();
            for (IDevice device : devices) {
                if (deviceSerial.equals(device.getSerialNumber()) && device.isOnline()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.debug("Emulator {} is not running or not responsive: {}", emulatorNumber, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get list of available Android Virtual Devices (AVDs)
     */
    private String[] getAvailableAvds() {
        try {
            String emulatorPath = getEmulatorExecutablePath();
            ProcessBuilder pb = new ProcessBuilder(emulatorPath, "-list-avds");
            Process process = pb.start();
            
            List<String> avds = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("INFO") && !line.startsWith("WARNING")) {
                        avds.add(line);
                    }
                }
            }
            
            process.waitFor();
            logger.info("Found {} available AVDs: {}", avds.size(), avds);
            return avds.toArray(new String[0]);
            
        } catch (Exception e) {
            logger.warn("Failed to list available AVDs: {}", e.getMessage());
            return new String[0];
        }
    }
    
    /**
     * Choose the best AVD to launch based on naming preferences
     */
    private String choosePreferredAvd(String[] availableAvds) {
        // Prefer AVDs with bot-related names
        String[] preferredNames = {"white_bot", "whiteout", "bot", "wos"};
        
        for (String preferred : preferredNames) {
            for (String avd : availableAvds) {
                if (avd.toLowerCase().contains(preferred.toLowerCase())) {
                    logger.info("Found preferred AVD for bot: {}", avd);
                    return avd;
                }
            }
        }
        
        // Fallback to first available AVD
        logger.info("No preferred AVD found, using first available: {}", availableAvds[0]);
        return availableAvds[0];
    }
    
    /**
     * Get the path to the Android SDK emulator executable
     */
    private String getEmulatorExecutablePath() {
        String osName = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String emulatorExecutable = osName.contains("win") ? "emulator.exe" : "emulator";
        
        // Try different common paths for Android SDK
        String[] possiblePaths = {
            userHome + "/Library/Android/sdk/emulator/" + emulatorExecutable,  // macOS default
            userHome + "/Android/Sdk/emulator/" + emulatorExecutable,         // Linux default
            userHome + "\\AppData\\Local\\Android\\Sdk\\emulator\\" + emulatorExecutable, // Windows default
            "/opt/android-sdk/emulator/" + emulatorExecutable,                // Linux alternative
            System.getenv("ANDROID_HOME") + "/emulator/" + emulatorExecutable // Environment variable
        };
        
        for (String path : possiblePaths) {
            if (path != null && new File(path).exists()) {
                logger.debug("Found emulator executable at: {}", path);
                return path;
            }
        }
        
        // Fallback to system PATH
        logger.debug("Using emulator from system PATH");
        return emulatorExecutable;
    }
}
