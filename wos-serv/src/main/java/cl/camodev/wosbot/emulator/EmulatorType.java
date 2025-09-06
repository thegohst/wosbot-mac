package cl.camodev.wosbot.emulator;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;

public enum EmulatorType {
	// @formatter:off
    ANDROID_STUDIO("Android Studio Emulator", EnumConfigurationKey.ANDROID_STUDIO_PATH_STRING.name(), "emulator", "");
	    // @formatter:on

	private final String displayName;
	private final String configKey;
	private final String executableName;
	private final String defaultPath;

	EmulatorType(String displayName, String configKey, String executableName, String defaultPath) {
		this.displayName = displayName;
		this.configKey = configKey;
		this.executableName = executableName;
		this.defaultPath = defaultPath;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getConfigKey() {
		return configKey;
	}

	public String getExecutableName() {
		return executableName;
	}

	public String getDefaultPath() {
		if (this == ANDROID_STUDIO) {
			return getAndroidSdkPath() + getEmulatorExecutableName();
		}
		return defaultPath + executableName;
	}
	
	private static String getEmulatorExecutableName() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("win") ? "emulator.exe" : "emulator";
	}
	
	private static String getAndroidSdkPath() {
		String osName = System.getProperty("os.name").toLowerCase();
		String userHome = System.getProperty("user.home");
		
		if (osName.contains("win")) {
			return userHome + "\\AppData\\Local\\Android\\Sdk\\emulator\\";
		} else if (osName.contains("mac")) {
			return userHome + "/Library/Android/sdk/emulator/";
		} else {
			// Linux
			return userHome + "/Android/Sdk/emulator/";
		}
	}
}
