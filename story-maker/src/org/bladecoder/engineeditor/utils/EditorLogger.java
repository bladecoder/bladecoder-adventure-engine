package org.bladecoder.engineeditor.utils;

public class EditorLogger {
	private static String TAG = "EDITOR";
	public static enum Levels {DEBUG, ERROR};
	
	public static Levels level = Levels.ERROR;

	public static void debug(String message) {
		if(level == Levels.DEBUG)
			System.out.println(TAG + ": " + message);
	}

	public static void error(String message) {
		System.out.println(TAG + ": " + message);
	}

	public static void error(String message, Exception e) {
		System.out.println(TAG + ": " + message + " Exception: " + e.getMessage());
	}

	public static void toggle() {
		if (level == Levels.DEBUG)
			level = Levels.ERROR;
		else
			level = Levels.DEBUG;
	}

	public static boolean debugMode() {
		if (level == Levels.DEBUG)
			return true;

		return false;
	}

	public static Levels getDebugLevel() {
		return level;
	}

	public static void setDebugLevel(Levels l) {
		level = l;
	}
	
	public static void setDebug() {
		level = Levels.DEBUG;
	}
}
