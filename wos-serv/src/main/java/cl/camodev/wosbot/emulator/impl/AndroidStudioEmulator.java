package cl.camodev.wosbot.emulator.impl;

import cl.camodev.wosbot.emulator.Emulator;
import com.android.ddmlib.IDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        logger.info("Android Studio emulators are launched externally via Android Studio or command line");
        logger.info("Emulator should already be running with device ID: {}", getDeviceSerial(emulatorNumber));
    }
    
    @Override
    public void closeEmulator(String emulatorNumber) {
        // For Android Studio emulators, we can try to close via ADB
        String deviceSerial = getDeviceSerial(emulatorNumber);
        logger.info("Attempting to close Android Studio emulator: {}", deviceSerial);
        
        try {
            // Use withRetries pattern like other methods in the base class
            withRetries(emulatorNumber, device -> {
                try {
                    device.executeShellCommand("reboot -p", new com.android.ddmlib.NullOutputReceiver());
                    logger.info("Close command sent to emulator: {}", deviceSerial);
                } catch (Exception e) {
                    logger.warn("Could not close emulator via ADB: {}", e.getMessage());
                }
                return null;
            }, "closeEmulator");
        } catch (Exception e) {
            logger.warn("Could not close emulator: {}", e.getMessage());
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
}
