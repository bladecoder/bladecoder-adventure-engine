/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
